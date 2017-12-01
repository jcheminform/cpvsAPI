package models

import models.dao.ProfileDAO
import play.api.libs.functional.syntax._
import play.api.libs.json._


object Profile {
  implicit val ProfileWrites: Writes[Profile] = (
    (JsPath \ "r_name").write[String] and
      (JsPath \ "pdbCode").write[String] and
      (JsPath \ "l_id").write[String] and
      (JsPath \ "l_name").write[Double]
    )(unlift(Profile.unapply))
  
  def findProfileByReceptorName(r_name: String): List[Profile] =
    ProfileDAO.indexByReceptorName(r_name)

  def findProfileByPdbCode(pdbCode: String): List[Profile] =
    ProfileDAO.indexByPdbCode(pdbCode)
}

case class Profile(r_name: String, pdbCode: String, l_id: String, l_score: Double)