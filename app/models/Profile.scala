package models

import models.dao.ProfileDAO
import play.api.libs.functional.syntax._
import play.api.libs.json._


object Profile {
  implicit val ProfileWrites: Writes[Profile] = (
    (JsPath \ "l_id").write[String] and
      (JsPath \ "l_score").write[Double] and
      (JsPath \ "l_prediction").write[String] and
      (JsPath \ "r_name").write[String] and
      (JsPath \ "r_pdbCode").write[String] 
    )(unlift(Profile.unapply))
 
  def findProfileByLigandId(lId : String) : List[Profile] = 
    ProfileDAO.profileByLigandId(lId)
}

case class Profile( l_id: String, l_score: Double, l_prediction : String, r_name: String, r_pdbCode: String)