package models

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.sql.DriverManager
import java.lang.Long

import scala.io.Source
import scala.language.postfixOps

import org.apache.spark.mllib.regression.LabeledPoint
import org.jsoup.Jsoup
import org.openscience.cdk.interfaces.IAtomContainer

import models.dao.ProfileDAO
import play.api.Logger
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.JsPath
import play.api.libs.json.Writes
import se.uu.farmbio.vs.{ MLlibSVM, ConformerPipeline, PosePipeline, SGUtils_Serial }
import se.uu.it.cp.InductiveClassifier

object Profile {
  //Need to be Updated
  val VINA_DOCKING_URL = "http://pele.farmbio.uu.se/cpvs-vina/multivina.sh"
  val VINA_CONF_URL = "http://pele.farmbio.uu.se/cpvs-vina/conf.txt"
  val VINA_HOME = "http://pele.farmbio.uu.se/cpvs-vina/"
  val OBABEL_HOME_URL = "http://pele.farmbio.uu.se/cpvs-vina/"

  //Use local receptors file if set otherwise complain
  val resourcesHome = if (System.getenv("RESOURCES_HOME") != null) {
    Logger.info("JOB_INFO: using local resources for reading receptors: " + System.getenv("RESOURCES_HOME"))
    System.getenv("RESOURCES_HOME")
  } else {
    Logger.error("JOB_ERROR: RESOURCES_HOME is not set")
    System.exit(1)
    "Path Not set"
  }

  implicit val ProfileWrites: Writes[Profile] = (
    (JsPath \ "l_id").write[String] and
    (JsPath \ "l_score").write[String] and
    (JsPath \ "l_prediction").write[String] and
    (JsPath \ "r_name").write[String] and
    (JsPath \ "r_pdbCode").write[String])(unlift(Profile.unapply))

  def findProfileByLigandId(lId: String): List[Profile] =
    ProfileDAO.profileByLigandId(lId)

  def computeAndSaveScore(lId: String, rName: String, rPdbCode: String): String =
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

      //Create ReceptorPath
      val rNameWithExtension = rName + ".pdbqt"
      val receptorPath = resourcesHome + rNameWithExtension
      Logger.info("JOB_INFO: The receptor file complete path is " + receptorPath)

      //Get link and download the conformer using link
      val ligand = downloadFile(getDownloadLink(lId), lId)

      //Compute Score using docking
      //Convert sdf ligand to pdbqt format using obabel
      val pdbqtLigand: String = "MODEL\n" + ConformerPipeline.pipeString(
        ligand,
        List(obabelPath, "-i", "sdf", "-o", "pdbqt")).trim() + "\nENDMDL"

      //Docking pdbqtLigand against receptor using VINA
      val dockedpdbqt: String = ConformerPipeline.pipeString(
        pdbqtLigand,
        List(vinaDockingPath, "--receptor",
          receptorPath, "--config", vinaConfPath))

      //Convert pdbqt ligand to sdf format using obabel
      val pdbqtToSdfLigand = ConformerPipeline.pipeString(
        dockedpdbqt,
        List(obabelPath, "-i", "pdbqt", "-o", "sdf"))

      //Cleaning Molecule after docking and getting score
      val lScore = PosePipeline.parseScore(ConformerPipeline.cleanPoses(pdbqtToSdfLigand, false).trim).toString

      //Save Score in DOCKED_LIGANDS
      ProfileDAO.saveLigandScoreById(lId, lScore, rName, rPdbCode)
      lScore
    }

  def predictAndSave(lId: String, rName: String, rPdbCode: String): String = {
    //Row Existance test
    var result: String = null
    val predictionExist = ProfileDAO.predictionExistCheck(lId, rPdbCode)
   
    if (predictionExist == 1) result = "Prediction Already Exist, Use GET"
    else {
      //Get link and download the conformer using link
      val ligand = downloadFile(getDownloadLink(lId), lId)

      //Loading oldSig2ID Mapping
      val oldSig2ID: Map[String, Long] = SGUtils_Serial.loadSig2IdMap(resourcesHome + "/sig2Id")

      //Getting Seq of IAtomContainer
      val iAtomSeq: Seq[IAtomContainer] = ConformerPipeline.sdfStringToIAtomContainer(ligand)

      //Array of IAtomContainers
      val iAtomArray = iAtomSeq.toArray

      //Unit sent as carry, later we can add any type required
      val iAtomArrayWithFakeCarry = iAtomArray.map { case x => (Unit, x) }

      //Generate Signature(in vector form) of New Molecule(s)
      val newSigns = SGUtils_Serial.atoms2LP_carryData(iAtomArrayWithFakeCarry, oldSig2ID, 1, 3)

      //Load Model
      //val svmModel = ProfileDAO.getModelByReceptorNameAndPdbCode(rName, rPdbCode)
      val svmModel = loadModel(rName, rPdbCode)
      //Predict New molecule(s)
      val predictions = newSigns.map { case (sdfMols, features) => (features, svmModel.predict(features.toArray, 0.5)) }

      //Actual prediction
      val prediction: Array[String] = predictions.map {
        case (vector, predSet) => predSet.toSeq(0) match {
          case 0.0 => "BAD"
          case 1.0 => "GOOD"
          case _   => "UNKNOWN"
        }
      }

      //Update Predictions to the Prediction Table
      ProfileDAO.saveLigandPredictionById(lId, prediction(0).toString, rName, rPdbCode)
      result = prediction(0).toString
    }
    result
  }

  private def downloadFile(urlLink: String, lId: String): String = {
    val url = new URL(urlLink)
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    val in: InputStream = connection.getInputStream
    val byteArray = Stream.continually(in.read).takeWhile(-1 !=).map(_.toByte).toArray
    val byteString = Source.fromBytes(byteArray).getLines().toArray.reduce(_ + "\n" + _)
    byteString
  }

  //Using Jsoup to reach parse zinc webpage
  private def getDownloadLink(lId: String): String = {

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

  def loadModel(rName: String, rPdbCode: String) = {
    //Connection Initialization
    Class.forName("org.mariadb.jdbc.Driver")
    val jdbcUrl = s"jdbc:mysql://localhost:3306/db_profile?user=root&password=2264421_root"
    val connection = DriverManager.getConnection(jdbcUrl)

    //Reading Pre-trained model from Database
    var model: InductiveClassifier[MLlibSVM, LabeledPoint] = null
    if (!(connection.isClosed())) {

      val sqlRead = connection.prepareStatement("SELECT r_model FROM MODELS WHERE r_name = ? and r_pdbCode = ?")
      sqlRead.setString(1, rName)
      sqlRead.setString(2, rPdbCode)
      val rs = sqlRead.executeQuery()
      rs.next()

      val modelStream = rs.getObject("r_model").asInstanceOf[Array[Byte]]
      val modelBaip = new ByteArrayInputStream(modelStream)
      val modelOis = new ObjectInputStream(modelBaip)
      model = modelOis.readObject().asInstanceOf[InductiveClassifier[MLlibSVM, LabeledPoint]]

      rs.close
      sqlRead.close
      connection.close()
    } else {
      println("MariaDb Connection is Close")
      System.exit(1)
    }
    model
  }

}

case class Profile(l_id: String, l_score: String, l_prediction: String, r_name: String, r_pdbCode: String)