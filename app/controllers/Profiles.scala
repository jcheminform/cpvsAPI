package controllers

import models.Prediction
import models.Prediction._
import models.Score
import models.Score._
import play.api.libs.json.Json
import play.api.mvc._
import java.io.FileNotFoundException

import org.openscience.cdk.DefaultChemObjectBuilder
import org.openscience.cdk.interfaces.IAtomContainer
import org.openscience.cdk.smiles.SmilesParser
import org.openscience.cdk.exception.InvalidSmilesException

object Profiles extends Controller {

  def predictionByLigandId(smiles: String) = Action {
    val SmilesArray = smiles.trim.split("££££")
    var counter = 1
    try {
      
      val smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance())
      
      //For getting exception if smiles is invalid
      //Although we can use map, this is fastest way to map array
      while (counter <= SmilesArray.length) {
        smilesParser.parseSmiles(SmilesArray(counter-1))
        counter += 1
      }

      val profilePredictions = Prediction.predictProfile(SmilesArray)
      Ok(Json.obj("predictions" -> profilePredictions))
    } catch {
      case exec: InvalidSmilesException =>
        println("\n ###########  An invalid smile with following message  ########## " + "\n" + exec.getStackTraceString)
        BadRequest("Invalid Smiles at line " + counter)
    }

  }

  def dockingByLigandId(smiles: String) = Action {
    try {
      //For getting exception if smiles is invalid
      val smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance())
      val molecule = smilesParser.parseSmiles(smiles)

      //For getting docking score
      val dockingScore = Score.dockProfile(smiles)
      Ok(Json.obj("Score" -> dockingScore))
    } catch {
      case exec: InvalidSmilesException =>
        println("\n ###########  An invalid smile with following message  ########## " + "\n" + exec.getStackTraceString)
        BadRequest("Invalid Smiles")
    }
  }

  /*
  def profileByLigandId(lId: String) = Action {
    val allProfiles = Profile.findProfileByLigandId(lId)
    if ((allProfiles == null) || (allProfiles.length < 1)) {
      NotFound("Profile Not Available in Database, but you can use our prediction service")
    } else
      Ok(Json.obj("result" -> allProfiles))
  }

  def dockAndSave(lId: String, rName: String, rPdbCode: String) = Action {

    try {
      val receptorExist = Profile.receptorExistCheck(rName, rPdbCode)
      if (receptorExist == 1) {
        val profile = Profile.computeAndSaveScore(lId, rName, rPdbCode)
        Ok(Json.obj("Score" -> profile))
      } else {
        BadRequest("Receptor Not found, make sure receptor name and id is correct")
      }
    } catch {
      case fnf: FileNotFoundException => BadRequest("Ligand Not found, make sure ZINC ligand id is correct")
    }

  }

  def predict(lId: String, rName: String, rPdbCode: String) = Action {
    try {
      val receptorExist = Profile.receptorExistCheck(rName, rPdbCode)
      if (receptorExist == 1) {
      val profile = Profile.predictAndSave(lId, rName, rPdbCode)
      Ok(Json.obj("Prediction" -> profile))
      }
      else {
        BadRequest("Receptor Not found, make sure receptor name and id is correct")
      }
    } catch {
      case fnf: FileNotFoundException => BadRequest("Ligand Not found, make sure ZINC ligand id is correct")
    }
  }*/

  def welcome() = Action {
    Redirect(url = "/assets/docs/index.html")
  }

}
