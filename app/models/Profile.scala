package models

import models.dao.ProfileDAO
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.Logger
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.io.Source
import java.io.PrintWriter
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.URL
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.io.OutputStream
import java.io.InputStream
import scala.io.Source
import java.io.BufferedInputStream
import scala.language.postfixOps
import se.uu.farmbio.vs.ConformerPipeline
import se.uu.farmbio.vs.PosePipeline

object Profile {
  //Need to be Updated
  val VINA_DOCKING_URL = "http://pele.farmbio.uu.se/cpvs-vina/multivina.sh"
  val VINA_CONF_URL = "http://pele.farmbio.uu.se/cpvs-vina/conf.txt"
  val VINA_HOME = "http://pele.farmbio.uu.se/cpvs-vina/"
  val OBABEL_HOME_URL = "http://pele.farmbio.uu.se/cpvs-vina/"

  implicit val ProfileWrites: Writes[Profile] = (
    (JsPath \ "l_id").write[String] and
    (JsPath \ "l_score").write[String] and
    (JsPath \ "l_prediction").write[String] and
    (JsPath \ "r_name").write[String] and
    (JsPath \ "r_pdbCode").write[String])(unlift(Profile.unapply))

  def findProfileByLigandId(lId: String): List[Profile] =
    ProfileDAO.profileByLigandId(lId)

  def computeAndSaveScore(lId: String, rName: String, rPdbCode: String) : String =
    {
      //Use local sh file if VINA_DOCKING is set
      val vinaDockingPath = if (System.getenv("VINA_DOCKING") != null) {
        Logger.info("JOB_INFO: using local multivana: " + System.getenv("VINA_DOCKING"))
        System.getenv("VINA_DOCKING")
      } else {
        Logger.info("JOB_INFO: using remote multivina: " + VINA_DOCKING_URL)
        VINA_DOCKING_URL
      }

      //Use local vina conf.txt file if VINA_CONF is set
      val vinaConfPath = if (System.getenv("VINA_CONF") != null) {
        Logger.info("JOB_INFO: using local vina conf: " + System.getenv("VINA_CONF"))
        System.getenv("VINA_CONF")
      } else {
        Logger.info("JOB_INFO: using remote vina conf: " + VINA_CONF_URL)
        VINA_CONF_URL
      }

      //Use local vina conf.txt file if VINA_CONF is set
      val obabelPath = if (System.getenv("OBABEL_HOME") != null) {
        Logger.info("JOB_INFO: using local obabel: " + System.getenv("OBABEL_HOME"))
        System.getenv("OBABEL_HOME")
      } else {
        Logger.info("JOB_INFO: using remote obabel: " + OBABEL_HOME_URL)
        OBABEL_HOME_URL
      }

      //Use local receptors file if set otherwise complain
      val receptorHome = if (System.getenv("RECEPTORS_HOME") != null) {
        Logger.info("JOB_INFO: using local directory for reading receptors: " + System.getenv("RECEPTORS_HOME"))
        System.getenv("RECEPTORS_HOME")
      } else {
        Logger.error("JOB_ERROR: RECEPTORS_HOME is not set")
        System.exit(1)
        "Path Not set"
      }
      
      //Create ReceptorPath
      val rNameWithExtension = rName + ".pdbqt"
      val receptorPath = receptorHome + rNameWithExtension
      Logger.info("JOB_INFO: The receptor file complete path is " + receptorPath)

      //Get Link for the conformer file
      val linkHref = getDownloadLink(lId, rName, receptorHome)

      //DOWNLOAD the conformer using link
      val ligand = downloadFile(linkHref, lId)

      //Compute Score using docking

      //Convert sdf ligand to pdbqt format using obabel
      val pdbqtLigand: String = "MODEL\n" + pipeString(
        ligand,
        List(obabelPath, "-i", "sdf", "-o", "pdbqt")).trim() + "\nENDMDL"

      //Docking pdbqtLigand against receptor using VINA
      val dockedpdbqt: String = pipeString(
        pdbqtLigand,
        List(vinaDockingPath, "--receptor",
          receptorPath, "--config", vinaConfPath))
          
      //Convert pdbqt ligand to sdf format using obabel    
      val pdbqtToSdfLigand = pipeString(
        dockedpdbqt,
        List(obabelPath, "-i", "pdbqt", "-o", "sdf"))

      //Cleaning Molecule after docking and getting score
      val lScore = PosePipeline.parseScore(ConformerPipeline.cleanPoses(pdbqtToSdfLigand, false).trim).toString

      //Save Score in DOCKED_LIGANDS
      ProfileDAO.saveLigandScoreById(lId, lScore, rName, rPdbCode)
      lScore
    }

  private def downloadFile(urlLink: String, lId: String): String = {
    val url = new URL(urlLink)
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    val in: InputStream = connection.getInputStream
    val fileToDownloadAs = new java.io.File("data/" + lId + ".sdf")
    val out: OutputStream = new BufferedOutputStream(new FileOutputStream(fileToDownloadAs))
    val byteArray = Stream.continually(in.read).takeWhile(-1 !=).map(_.toByte).toArray
    out.write(byteArray)
    out.flush()
    out.close
    val byteString = Source.fromBytes(byteArray).getLines().toArray.reduce(_ + "\n" + _)
    byteString
  }

  private def getDownloadLink(lId: String, rName: String, receptorHome: String): String = {

    //Get download link for conformer
    val httpLink = "http://zinc.docking.org/substance/" + lId
    Logger.info("JOB_INFO: Download Page for " + lId + " is " + httpLink)

    val doc = Jsoup.connect(httpLink).get();
    val title = doc.title()
    val link = doc.select("a[href*=f=d]").first()

    val linkHref = link.attr("href");
    Logger.info("JOB_INFO: Required link is " + linkHref)
    linkHref
  }

  private def pipeString(str: String, command: List[String]) = {

    //Start executable
    val pb = new ProcessBuilder(command.asJava)
    val proc = pb.start
    // Start a thread to print the process's stderr to ours
    new Thread("stderr reader") {
      override def run() {
        for (line <- Source.fromInputStream(proc.getErrorStream).getLines) {
          System.err.println(line)
        }
      }
    }.start
    // Start a thread to feed the process input
    new Thread("stdin writer") {
      override def run() {
        val out = new PrintWriter(proc.getOutputStream)
        out.println(str)
        out.close()
      }
    }.start
    //Return results as a single string
    Source.fromInputStream(proc.getInputStream).mkString

  }

}

case class Profile(l_id: String, l_score: String, l_prediction: String, r_name: String, r_pdbCode: String)