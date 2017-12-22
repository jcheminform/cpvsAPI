package models.dao

import anorm._
import models.Profile
import play.api.db.DB
import play.api.Play.current


object ProfileDAO {
  def profileByLigandId(lId: String) :List[Profile] = {
    DB.withConnection { implicit c =>
      val results = SQL(
        """
          | SELECT PREDICTED_LIGANDS.l_id, DOCKED_LIGANDS.l_score, PREDICTED_LIGANDS.l_prediction,
          | PREDICTED_LIGANDS.r_name, PREDICTED_LIGANDS.r_pdbCode
          | FROM PREDICTED_LIGANDS
          | INNER JOIN DOCKED_LIGANDS
          | ON PREDICTED_LIGANDS.l_id=DOCKED_LIGANDS.l_id
          | WHERE PREDICTED_LIGANDS.l_id={l_id};
        """.stripMargin).on(
          "l_id" -> lId
        ).apply()

      results.map { row =>
        Profile(row[String]("l_id"),row[Double]("l_score"),row[String]("l_prediction"),row[String]("r_name"),row[String]("r_pdbCode"))
      }.force.toList
    }
    
  }
   
}
