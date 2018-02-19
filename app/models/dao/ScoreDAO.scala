package models.dao

import anorm._
import models.Score
import play.api.db.DB
import play.api.Play.current

object ScoreDAO {
  
  def scoreExistCheck(lId: String, rPdbCode: String): Int = {
    DB.withConnection { implicit c =>
      val results = SQL(
        """
          | SELECT EXISTS(SELECT 1 FROM DOCKED_SMILES 
          | WHERE l_id LIKE {l_id}
          | AND r_pdbCode LIKE {r_pdbCode} LIMIT 1) as EXIST;
        """.stripMargin).on(
          "l_id" -> lId,
          "r_pdbCode" -> rPdbCode)
        .as(SqlParser.scalar[Int].single)
      results
    }

  }
  
  def scoreByLigandIdAndPdbCode(lId: String, receptorPdbCode: String): Score = {
    DB.withConnection { implicit c =>
      val results = SQL(
        """
          | SELECT r_pdbCode, l_id, l_score
          | FROM DOCKED_SMILES
          | WHERE l_id={l_id} AND r_pdbCode={r_pdbCode};
        """.stripMargin).on(
          "l_id" -> lId,
          "r_pdbCode" -> receptorPdbCode).apply()

      results.map { row =>
        Score(
          row[String]("r_pdbCode"),
          row[String]("l_id"),
          row[String]("l_score"))
      }.force.last

    }

  }
  
  def saveLigandScoreById(rPdbCode: String, lId: String, lScore: String) = {
    DB.withConnection { implicit c =>
      SQL(
        """
          | INSERT IGNORE INTO DOCKED_SMILES (r_pdbCode, l_id, l_score)
          | VALUES
          |   ({r_pdbCode},{l_id}, {l_score});
        """.stripMargin).on(
          "r_pdbCode" -> rPdbCode,
          "l_id" -> lId,
          "l_score" -> lScore).executeInsert()
    }
  }
  
}