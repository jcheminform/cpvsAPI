package models

import models.dao.ReceptorAndPdbDAO
import play.api.libs.functional.syntax._
import play.api.libs.json._


object ReceptorAndPdb {
  implicit val ReceptorAndPdbWrites: Writes[ReceptorAndPdb] = (
    (JsPath \ "r_name").write[String] and
      (JsPath \ "r_pdbCode").write[String]
    )(unlift(ReceptorAndPdb.unapply))
  
  def findReceptorAndPdbList() : List[ReceptorAndPdb] =
    ReceptorAndPdbDAO.receptorAndPdbCodeIndex()
}

case class ReceptorAndPdb(r_name: String, r_pdbCode : String)