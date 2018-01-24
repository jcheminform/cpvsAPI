package controllers

import models.dao.LigandDAO
import play.api.libs.json.Json
import play.api.mvc._

object Ligands extends Controller {
  def ligandSample() = Action {
    
    val ligandSample = LigandDAO.getLigandsList()
    
    Ok(Json.obj("result" -> ligandSample))
  }
}