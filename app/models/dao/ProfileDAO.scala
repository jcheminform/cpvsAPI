package models.dao

import anorm._
import models.Profile
import play.api.db.DB
import play.api.Play.current
import java.io.PrintWriter

object ProfileDAO {
  
   def saveLigandScoreById(lId: String, lScore : String, rName : String, rPdbCode : String) = {
    DB.withConnection { implicit c =>
      SQL(
        """
          | INSERT IGNORE INTO DOCKED_LIGANDS (l_id, l_score, r_name, r_pdbCode)
          | VALUES
          |   ({l_id}, {l_score}, {r_name}, {r_pdbCode});
        """.stripMargin).on(
          "l_id" -> lId,
          "l_score" -> lScore,
          "r_name" -> rName,
          "r_pdbCode" -> rPdbCode
        ).executeInsert()
    }
  }
  
  def profileByLigandId(lId: String): List[Profile] = {
    DB.withConnection { implicit c =>
      val results = SQL(
        """
          | SELECT PREDICTED_LIGANDS.l_id, IFNULL(DOCKED_LIGANDS.l_score, "Not Available") AS l_score,
          | PREDICTED_LIGANDS.l_prediction, PREDICTED_LIGANDS.r_name, PREDICTED_LIGANDS.r_pdbCode
          | FROM PREDICTED_LIGANDS
          | LEFT JOIN DOCKED_LIGANDS
          | ON PREDICTED_LIGANDS.l_id=DOCKED_LIGANDS.l_id
          | WHERE PREDICTED_LIGANDS.l_id={l_id};
        """.stripMargin).on(
          "l_id" -> lId).apply()
      val pw = new PrintWriter("data/test")
      results.foreach(pw.println(_))
      pw.close
      
      //IF QUERY IS EMPTY MEANS PREDICTION NOT AVAIALBLE IMPLEMENT THIS MESSAGE
      
      results.map { row =>
        Profile(row[String]("l_id"), 
            row[String]("l_score"), 
            row[String]("l_prediction"), 
            row[String]("r_name"), 
            row[String]("r_pdbCode"))
      }.force.toList
    }

  }

}
