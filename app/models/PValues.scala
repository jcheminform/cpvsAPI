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
import scala.math.BigDecimal

object PValues {

  implicit val PValuesWrites: Writes[PValues] = (
    (JsPath \ "r_pdbCode").write[String] and
    (JsPath \ "l_id").write[String] and
    (JsPath \ "pv_0").write[String] and
    (JsPath \ "pv_1").write[String])(unlift(PValues.unapply))

  def predictProfile(smilesArray: Array[String]): Array[PValues] = {

    //Convert smi ligand to sdf format using obabel
    val smilesAndSdf: Array[(String, String)] = smilesArray.map { smiles =>
      (
        smiles,
        ConformerPipeline.pipeString(smiles, List(obabelPath, "-h", "-ismi", "-osdf", "--gen3d")))
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
    //val modelPredictions = smilesAndNewSigns.map { case (smiles, features) => (smiles, svmModel.predict(features.toArray, 0.2)) }
    val modelP_Values = smilesAndNewSigns.map { case (smiles, features) => (smiles, svmModel.mondrianPv(features.toArray)) }
/*
    //Actual prediction
    val predictions: Array[(String, String)] = modelPredictions.map {
      case (smiles, predSet) =>
        val lPrediction = if (predSet == Set(0.0)) "High-Score"
        else if (predSet == Set(1.0)) "Low-Score"
        else "UNKNOWN"
        (smiles, lPrediction)
    }
   
    */
    
    //Actual pValues
    val pValues = modelP_Values.map{
      case (smiles, pvalues) => 
        
        val pv_0 = BigDecimal(pvalues(0)).setScale(4,BigDecimal.RoundingMode.HALF_UP).toDouble.toString()
        val pv_1 = BigDecimal(pvalues(1)).setScale(4,BigDecimal.RoundingMode.HALF_UP).toDouble.toString()
      
      (smiles,pv_0, pv_1)
    }     

    val result = pValues.map { case (lId, pv_0, pv_1) => PValues(receptorPdbCode, lId, pv_0, pv_1) }
    result
  }
}

case class PValues(r_pdbCode: String, l_id: String, pv_0: String, pv_1: String)
