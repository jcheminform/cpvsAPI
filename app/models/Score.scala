package models

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

import scala.io.Source
import scala.language.postfixOps

import org.openscience.cdk.interfaces.IAtomContainer

import models.dao.ScoreDAO
import play.api.Logger
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.JsPath
import play.api.libs.json.Writes
import se.uu.farmbio.vs.SGUtils_Serial
import se.uu.farmbio.vs.{ ConformerPipeline, PosePipeline, SBVSPipeline }
import controllers.Global.{ obabelPath, svmModel, oldSig2ID, receptorName, receptorPdbCode, resourcesHome }

import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import org.openscience.cdk.io.MDLV2000Reader
import org.openscience.cdk.tools.manipulator.ChemFileManipulator
import org.openscience.cdk.silent.ChemFile

import org.openscience.cdk.DefaultChemObjectBuilder
import org.openscience.cdk.interfaces.IAtomContainer
import org.openscience.cdk.smiles.SmilesParser
import org.openscience.cdk.exception.InvalidSmilesException

import java.io.FileNotFoundException

object Score {
  //Need to be Updated

  implicit val ScoreWrites: Writes[Score] = (
    (JsPath \ "r_pdbCode").write[String] and
    (JsPath \ "l_id").write[String] and
    (JsPath \ "l_score").write[String])(unlift(Score.unapply))

  //dock the sdf file against Autodock vina

  //Save the pdb_code, l_id (inchiKey) and l_score (docking score)

  /*
  def findProfileByLigandId(lId: String): List[Profile] =
    ProfileDAO.profileByLigandId(lId)

  def receptorExistCheck(rName: String, rPdbCode: String) = ProfileDAO.receptorExistCheck(rName, rPdbCode)
  *
  */

  def dockProfile(smiles: String): Score =
    {

      //Convert smi to inchiKey
      val smiToInchiKey =
        ConformerPipeline.pipeString(
          smiles,
          List(obabelPath, "-ismi", "-oinchikey")).trim()

      //Row Existance test
      var result: Score = null
      val scoreExist = ScoreDAO.scoreExistCheck(smiToInchiKey, receptorPdbCode)

      //If Score exists in database, then read and display to user
      if (scoreExist == 1) {
        println("\n######  Loading Score from database  ###### \n")
        result = ScoreDAO.scoreByLigandIdAndPdbCode(smiToInchiKey, receptorPdbCode)
      } //Else dock the molecule and show result to user
      else {
        //Use local sh file if VINA_DOCKING is set
        val vinaDockingPath = if (System.getenv("VINA_DOCKING") != null) {
          Logger.info("JOB_INFO: using local multivana: " + System.getenv("VINA_DOCKING"))
          System.getenv("VINA_DOCKING")
        } else {
          Logger.info("JOB_ERROR: VINA_DOCKING is not set")
          "VINA_DOCKING is not set"
        }

        //Use local vina conf.txt file if VINA_CONF is set
        val vinaConfPath = if (System.getenv("VINA_CONF") != null) {
          Logger.info("JOB_INFO: using local vina conf: " + System.getenv("VINA_CONF"))
          System.getenv("VINA_CONF")
        } else {
          Logger.info("JOB_ERROR: VINA_CONF is not set")
          "VINA_CONF is not set"
        }

        //Create ReceptorPath
        val rNameWithExtension = receptorName + ".pdbqt"
        val receptorPath = resourcesHome + rNameWithExtension
        Logger.info("JOB_INFO: The receptor file complete path is " + receptorPath)

        //Convert SMILES to SDF using obabel
        val sdfLigand: String = ConformerPipeline.pipeString(
          smiles,
          List(obabelPath, "-ismi", "-osdf", "--gen3d"))

        //Compute Score using docking
        //Convert sdf ligand to pdbqt format using obabel
        val pdbqtLigand: String = "MODEL\n" + ConformerPipeline.pipeString(
          sdfLigand,
          List(obabelPath, "-i", "sdf", "-o", "pdbqt")).trim() + "\nENDMDL"

        //Docking pdbqtLigand against receptor using VINA
        val dockedpdbqt: String = ConformerPipeline.pipeString(
          pdbqtLigand,
          List(vinaDockingPath, "--receptor",
            receptorPath, "--config", vinaConfPath))

        //Convert pdbqt ligand to sdf format using obabel
        val pdbqtToSdfLigand: String = ConformerPipeline.pipeString(
          dockedpdbqt,
          List(obabelPath, "-i", "pdbqt", "-o", "sdf"))

        //Cleaning Molecule after docking and getting score
        val lScore = PosePipeline.parseScore(ConformerPipeline.cleanPoses(pdbqtToSdfLigand, false).trim).toString

        println("The Docking Score is  " + lScore)

        //Save Score in DOCKED_LIGANDS
        ScoreDAO.saveLigandScoreById(receptorPdbCode, smiToInchiKey, lScore)
        result = Score(receptorPdbCode, smiToInchiKey, lScore)
      }
      result

    }

  /*
  def predictAndSave(lId: String, rName: String, rPdbCode: String): String = {
    //Row Existance test
    var result: String = null
    val predictionExist = ProfileDAO.predictionExistCheck(lId, rPdbCode)

    if (predictionExist == 1) result = "Prediction Already Exist, Use GET"
    else {
      //Get link and download the conformer using link
      val ligand = downloadFile(getDownloadLink(lId), lId)

      //Loading oldSig2ID Mapping
      val oldSig2ID: Map[String, Long] = SGUtils_Serial.loadSig2IdMap(resourcesHome + "sig2Id")

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
      val svmModel = ProfileDAO.loadModel(rName, rPdbCode)
      //val svmModel = loadModel(rName,rPdbCode)

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
  }*/

}

case class Score(r_pdbCode: String, l_id: String, l_score: String)
