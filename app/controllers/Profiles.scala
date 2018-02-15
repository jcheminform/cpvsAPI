package controllers

import models.Profile
import models.Profile._
import models.Prediction
import models.Prediction._
import play.api.libs.json.Json
import play.api.mvc._
import java.io.FileNotFoundException

object Profiles extends Controller {

  def predictionByLigandId(smiles: String) = Action {
    val SmilesArray = smiles.split(",")
    val profilePredictions = Profile.predictProfile(SmilesArray)
    Ok(Json.obj("results" -> profilePredictions))
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
