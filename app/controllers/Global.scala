package controllers

import org.apache.spark.mllib.regression.LabeledPoint

import anorm.SQL
import anorm.SqlParser
import anorm.sqlToSimple
import java.lang.Long
import play.api.Application
import play.api.GlobalSettings
import play.api.Logger
import play.api.Play.current
import play.api.db.DB
import se.uu.farmbio.vs.{ MLlibSVM, SGUtils_Serial }
import se.uu.it.cp.InductiveClassifier
import models.dao.GlobalDAO

object Global extends GlobalSettings {
  //Receptor Env. Variables
  val receptorName = System.getenv("RECEPTOR_NAME")
  val receptorPdbCode = System.getenv("RECEPTOR_PDBCODE")

  //Use local receptors file if set otherwise complain
  val resourcesHome = if (System.getenv("RESOURCES_HOME") != null) {
    Logger.info("JOB_INFO: using local resources for reading receptors: " + System.getenv("RESOURCES_HOME"))
    System.getenv("RESOURCES_HOME")
  } else {
    Logger.error("JOB_ERROR: RESOURCES_HOME is not set")
    System.exit(1)
  }

  //Use local obabel if OBABEL_HOME is set
  val obabelPath = if (System.getenv("OBABEL_HOME") != null) {
    Logger.info("JOB_INFO: using local obabel: " + System.getenv("OBABEL_HOME"))
    System.getenv("OBABEL_HOME")
  } else {
    Logger.info("JOB_ERROR: OBABEL_HOME is not set")
    System.exit(1)
    "PATH NOT FOUND"
  }

  var svmModel: InductiveClassifier[MLlibSVM, LabeledPoint] = null
  var oldSig2ID: Map[String, Long] = null

  override def onStart(app: Application) {
    Logger.info("********* WELCOME TO CPVS **********")
    //Loading SvmModel
    println("\n  #### Initializing model ####")
    svmModel = GlobalDAO.loadModel(receptorPdbCode)
    //Loading oldSig2ID Mapping
    println("\n  #### Initializing Sig2Id ####")
    oldSig2ID = SGUtils_Serial.loadSig2IdMap(resourcesHome + "sig2Id")
  }

  override def onStop(app: Application) {
    Logger.info("******** BYE BYE FROM CPVS ********")
  }

 

 

}