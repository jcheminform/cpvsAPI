package controllers

import models.Profile
import models.Profile._
import play.api.libs.json.Json
import play.api.mvc._

object Profiles extends Controller {

  def findAll(r_name: String) = Action {
    val allProfiles = Profile.findProfileByReceptorName(r_name)

    Ok(Json.obj("result" -> allProfiles))
  }
}
