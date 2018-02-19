package models

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

import scala.io.Source
import scala.language.postfixOps

import org.openscience.cdk.interfaces.IAtomContainer

import models.dao.PredictionDAO
import play.api.Logger
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.JsPath
import play.api.libs.json.Writes
import se.uu.farmbio.vs.SGUtils_Serial
import se.uu.farmbio.vs.{ ConformerPipeline, PosePipeline }
import controllers.Global.{ obabelPath, svmModel, oldSig2ID, receptorName, receptorPdbCode, resourcesHome }

object Prediction {
  //Need to be Updated

  implicit val PredictionWrites: Writes[Prediction] = (
    (JsPath \ "l_prediction").write[String] and
    (JsPath \ "r_name").write[String] and
    (JsPath \ "r_pdbCode").write[String])(unlift(Prediction.unapply))

  def predictProfile(smilesArray: Array[String]): Array[Prediction] = {

    //Convert smi ligand to sdf format using obabel
    val smiToSdf: Array[String] = smilesArray.map { smiles =>
      ConformerPipeline.pipeString(
        smiles,
        List(obabelPath, "-ismi", "-osdf", "--gen3d"))
    }

    //Getting Seq of IAtomContainer
    val iAtomSeq: Seq[IAtomContainer] = smiToSdf.flatMap { smiToSdfLigand =>
      ConformerPipeline.sdfStringToIAtomContainer(smiToSdfLigand)
    }

    //Array of IAtomContainers
    val iAtomArray = iAtomSeq.toArray

    //Unit sent as carry, later we can add any type required
    val iAtomArrayWithFakeCarry = iAtomArray.map { case x => (Unit, x) }

    //Generate Signature(in vector form) of New Molecule(s)
    val newSigns = SGUtils_Serial.atoms2LP_carryData(iAtomArrayWithFakeCarry, oldSig2ID, 1, 3)

    //Predict New molecule(s) , svmModel comes from Global.scala loaded once on project startup
    val modelPredictions = newSigns.map { case (sdfMols, features) => (features, svmModel.predict(features.toArray, 0.5)) }

    //Actual prediction
    val predictions: Array[String] = modelPredictions.map {
      case (sdfmol, predSet) =>
        val lPrediction = if (predSet == Set(0.0)) "BAD"
        else if (predSet == Set(1.0)) "GOOD"
        else "UNKNOWN"
        lPrediction
    }

    val result = predictions.map { actualPrediction => Prediction(actualPrediction, receptorName, receptorPdbCode) }
    result
  }

  //dock the sdf file against Autodock vina

  //Save the pdb_code, l_id (inchiKey) and l_score (docking score)

  /*
  def findProfileByLigandId(lId: String): List[Profile] =
    ProfileDAO.profileByLigandId(lId)

  def receptorExistCheck(rName: String, rPdbCode: String) = ProfileDAO.receptorExistCheck(rName, rPdbCode)
  *
  */

  
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

case class Prediction(l_prediction: String, r_name: String, r_pdbCode: String)
