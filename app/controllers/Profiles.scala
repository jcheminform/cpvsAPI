package controllers

import models.Profile
import models.Profile._
import play.api.libs.json.Json
import play.api.mvc._

object Profiles extends Controller {

  def profileByReceptorName(r_name: String) = Action {
    val allProfiles = Profile.findProfileByReceptorName(r_name)

    Ok(Json.obj("result" -> allProfiles))
  }
  
  def profileByPdbCode(pdbCode: String) = Action {
    val allProfiles = Profile.findProfileByPdbCode(pdbCode)

    Ok(Json.obj("result" -> allProfiles))
  }
  
  def welcome() = Action {
    
    Ok("CPVS API is Up and Running" + "\n" + "Please Use /profiles end point for details and /receptors end point for list of receptors and their pdbCodes")
  }
}
