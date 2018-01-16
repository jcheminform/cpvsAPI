package models.dao

import anorm._
import models.Profile
import play.api.db.DB
import play.api.Play.current
import java.sql.DriverManager
import se.uu.it.cp.InductiveClassifier
import java.io.ObjectInputStream
import java.io.ByteArrayInputStream
import se.uu.farmbio.vs.MLlibSVM
import org.apache.spark.mllib.regression.LabeledPoint
import play.api.Logger


object ProfileDAO {

  def saveLigandScoreById(lId: String, lScore: String, rName: String, rPdbCode: String) = {
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
          "r_pdbCode" -> rPdbCode).executeInsert()
    }
  }

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

  def scoreExistCheck(lId: String, rPdbCode: String): Int = {
    DB.withConnection { implicit c =>
      val results = SQL(
        """
          | SELECT EXISTS(SELECT 1 FROM DOCKED_LIGANDS 
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

      results.map { row =>
        Profile(
          row[String]("l_id"),
          row[String]("l_score"),
          row[String]("l_prediction"),
          row[String]("r_name"),
          row[String]("r_pdbCode"))
      }.force.toList
    }

  }

  def loadModel(rName: String): InductiveClassifier[MLlibSVM, LabeledPoint] = {
  DB.withConnection { implicit c =>
    val result = SQL(
      """
        | SELECT r_model
        | FROM MODELS
        | WHERE r_name={r_name}
      """.stripMargin)
      .on("r_name" -> rName)
      .as(SqlParser.byteArray("r_model").single) 
    Logger.info(s"result ${result.getClass} => $result")
    deserialize[InductiveClassifier[MLlibSVM, LabeledPoint]](result)
  }
}
  
  def deserialize[T](byteArray: Array[Byte]): T = {
  val ois = new ObjectInputStream(new ByteArrayInputStream(byteArray))
  ois.readObject().asInstanceOf[T]
}

}
