package models.dao

import anorm._
import models.Profile
import play.api.db.DB
import play.api.Play.current

object ProfileDAO {
  
  def index(r_name: String): List[Profile] = {
    DB.withConnection { implicit c =>
      val results = SQL(
        """
          | SELECT RECEPTORS.r_name,RECEPTORS.pdbCode, LIGANDS.l_id, LIGANDS.l_score 
          | FROM RECEPTORS
          | INNER JOIN LIGANDS
          | WHERE RECEPTORS.r_name={r_name};
        """.stripMargin).on(
          "r_name" -> r_name
        ).apply()

      results.map { row =>
        Profile(row[String]("r_name"), row[String]("pdbCode"),row[String]("l_id"),row[Double]("l_score"))
      }.force.toList
    }
  }
}


