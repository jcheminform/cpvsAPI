package models

import scala.language.postfixOps

import org.openscience.cdk.interfaces.IAtomContainer
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.JsPath
import play.api.libs.json.Writes
import se.uu.farmbio.vs.{ SGUtils_Serial, ConformerPipeline }
import controllers.Global.{ receptorName, receptorPdbCode, oldSig2ID, obabelPath, svmModel }
import org.apache.spark.mllib.linalg.Vector

object Prediction {

  implicit val PredictionWrites: Writes[Prediction] = (
    (JsPath \ "r_pdbCode").write[String] and
    (JsPath \ "l_id").write[String] and
    (JsPath \ "l_prediction").write[String])(unlift(Prediction.unapply))

  def predictProfile(smilesArray: Array[String]): Array[Prediction] = {

    //Convert smi ligand to sdf format using obabel
    val smilesAndSdf: Array[(String, String)] = smilesArray.map { smiles =>
      (
        smiles,
        ConformerPipeline.pipeString(smiles, List(obabelPath, "-ismi", "-osdf", "--gen3d")))
    }

    //Getting Seq of IAtomContainer
    val smilesAndIAtomMol: Array[(String, IAtomContainer)] = smilesAndSdf.flatMap {
      case (smiles, sdfmol) =>
        ConformerPipeline.sdfStringToIAtomContainer(sdfmol)
          .map {
            case (iAtomMol) =>
              (smiles, iAtomMol)

          }

    }


    //Generate Signature(in vector form) of New Molecule(s)
    val smilesAndNewSigns: Array[(String, Vector)] = SGUtils_Serial.atoms2LP_carryData(smilesAndIAtomMol, oldSig2ID, 1, 3)

    //Predict New molecule(s) , svmModel comes from Global.scala loaded once on project startup
    val modelPredictions = smilesAndNewSigns.map { case (smiles, features) => (smiles, svmModel.predict(features.toArray, 0.2)) }

    //Actual prediction
    val predictions: Array[(String, String)] = modelPredictions.map {
      case (smiles, predSet) =>
        val lPrediction = if (predSet == Set(0.0)) "Low-Score"
        else if (predSet == Set(1.0)) "High-Score"
        else "UNKNOWN"
        (smiles, lPrediction)
    }

    val result = predictions.map { case(lId, lPrediction) => Prediction(receptorPdbCode, lId, lPrediction) }
    result
  }
}

case class Prediction(r_pdbCode: String, l_id: String, l_prediction: String)
