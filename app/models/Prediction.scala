package models

import scala.language.postfixOps

import org.openscience.cdk.interfaces.IAtomContainer
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.JsPath
import play.api.libs.json.Writes
import se.uu.farmbio.vs.{ SGUtils_Serial, ConformerPipeline }
import controllers.Global.{ receptorName, receptorPdbCode, oldSig2ID, obabelPath, svmModel }

object Prediction {

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
}

case class Prediction(l_prediction: String, r_name: String, r_pdbCode: String)
