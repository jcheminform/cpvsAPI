package models.dao

import anorm._
import play.api.db.DB
import play.api.Play.current

object LigandDAO {
  def getLigandsList(): List[String] = {
    DB.withConnection { implicit c =>
      val results = SQL(
        """
          | SELECT l_id FROM PREDICTED_LIGANDS LIMIT 10;    
        """.stripMargin).apply()

      results.map { row =>
        row[String]("l_id")
        }.force.toList
    }
  }
}