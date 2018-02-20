package models.dao

import anorm.SQL
import anorm.SqlParser
import anorm.sqlToSimple
import play.api.Play.current
import play.api.db.DB

object PredictionDAO {

  def predictionExistCheck(lId: String, rPdbCode: String): Int = {
    DB.withConnection { implicit c =>
      val results = SQL(
        """
          | SELECT EXISTS(SELECT 1 FROM PREDICTED_LIGANDS 
          | WHERE l_id LIKE {l_id}
          | AND r_pdbCode LIKE {r_pdbCode} LIMIT 1) as EXIST;
        """.stripMargin).on(
          "l_id" -> lId,
          "r_pdbCode" -> rPdbCode)
        .as(SqlParser.scalar[Int].single)
      results
    }

  }

  def saveLigandPredictionById(lId: String, lPrediction: String, rName: String, rPdbCode: String) = {
    DB.withConnection { implicit c =>
      SQL(
        """
          | INSERT IGNORE INTO PREDICTED_LIGANDS (l_id, l_prediction, r_name, r_pdbCode)
          | VALUES
          |   ({l_id}, {l_prediction}, {r_name}, {r_pdbCode});
        """.stripMargin).on(
          "l_id" -> lId,
          "l_prediction" -> lPrediction,
          "r_name" -> rName,
          "r_pdbCode" -> rPdbCode).executeInsert()
    }
  }
}
