package controllers

import models.Profile
import models.Profile._
import play.api.libs.json.Json
import play.api.mvc._

object Profiles extends Controller {
  
  def profileByLigandId(lId: String) = Action {
    val allProfiles = Profile.findProfileByLigandId(lId)

    Ok(Json.obj("result" -> allProfiles))
  }
  
  def welcome() = Action {
    
    Ok("CPVS API is Up and Running" + "\n" + "Please Use /profiles end point for details and /receptors end point for list of receptors and their pdbCodes")
  }
}
