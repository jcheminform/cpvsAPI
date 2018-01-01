package controllers

import models.ReceptorAndPdb
import models.ReceptorAndPdb._
import play.api.libs.json.Json
import play.api.mvc._

object ReceptorsAndPdbs extends Controller {

  def receptorAndPdbIndex() = Action {
    
    val allReceptorsAndPdb = ReceptorAndPdb.findReceptorAndPdbList()
    
    Ok(Json.obj("result" -> allReceptorsAndPdb))
  }
  
 
}
