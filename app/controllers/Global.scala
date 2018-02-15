package controllers

import play.api._
import play.api.db.DB
import play.api.Play.current
import anorm._
import se.uu.it.cp.InductiveClassifier
import se.uu.farmbio.vs.{ MLlibSVM, SGUtils_Serial }
import org.apache.spark.mllib.regression.LabeledPoint
import java.io.ObjectInputStream
import java.io.ByteArrayInputStream
import java.lang.Long

object Global extends GlobalSettings {

  //Use local receptors file if set otherwise complain
  val resourcesHome = if (System.getenv("RESOURCES_HOME") != null) {
    Logger.info("JOB_INFO: using local resources for reading receptors: " + System.getenv("RESOURCES_HOME"))
    System.getenv("RESOURCES_HOME")
  } else {
    Logger.error("JOB_ERROR: RESOURCES_HOME is not set")
    System.exit(1)
  }

  var svmModel: InductiveClassifier[MLlibSVM, LabeledPoint] = null
  var oldSig2ID: Map[String, Long] = null

  override def onStart(app: Application) {
    Logger.info("********* WELCOME TO CPVS **********")
    //Loading SvmModel
    svmModel = loadModel_new()
    //Loading oldSig2ID Mapping
    oldSig2ID = SGUtils_Serial.loadSig2IdMap(resourcesHome + "sig2Id")
  }

  override def onStop(app: Application) {
    Logger.info("******** BYE BYE FROM CPVS ********")
  }

  def loadModel_new(): InductiveClassifier[MLlibSVM, LabeledPoint] = {
    DB.withConnection { implicit c =>
      val result = SQL(
        """
        | SELECT r_model
        | FROM MODELS
      """.stripMargin)
        .as(SqlParser.byteArray("r_model").single)
      Logger.info(s"result ${result.getClass} => $result")
      deserialize[InductiveClassifier[MLlibSVM, LabeledPoint]](result)
    }
  }

  def deserialize[T](byteArray: Array[Byte]): T = {
    val ois = new ObjectInputStream(new ByteArrayInputStream(byteArray))
    ois.readObject().asInstanceOf[T]
  }

}