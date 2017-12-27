package models

import models.dao.ProfileDAO
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.Logger
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.io.Source
import java.io.PrintWriter

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

  def computeAndSaveScore(lId: String, rName: String, rPdbCode: String) =
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
      //PROVIDE RECEPTOR AND LIGAND
      //SEARCH RECPETOR LOCAL FOLDER AND LOAD
          
      //SEARCH CONFORMER LOCAL FOLDER AND LOAD, ELSE DOWNLOAD
    
    //Compute Score using docking
     
      //Convert sdf ligand to pdbqt format using obabel
      val pdbqtLigand: String = pipeString(lId,
          List(obabelPath, "-i", "sdf", "-o", "pdbqt")).trim()
      
      //Docking pdbqtLigand against receptor using VINA     
      val lScore : String = pipeString(pdbqtLigand,
        List(vinaDockingPath, "--receptor",
          rName, "--config", vinaConfPath))    

      //Save Score in DOCKED_LIGANDS
      ProfileDAO.saveLigandScoreById(lId, lScore, rName, rPdbCode)

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