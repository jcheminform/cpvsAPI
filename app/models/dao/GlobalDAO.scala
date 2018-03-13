package models.dao

import anorm.SQL
import anorm.SqlParser
import anorm.sqlToSimple
import play.api.Play.current
import play.api.db.DB
import play.api.Logger

import org.apache.spark.mllib.regression.LabeledPoint
import se.uu.farmbio.vs.MLlibSVM

import java.io.{ByteArrayInputStream, ObjectInputStream}

import se.uu.it.cp.InductiveClassifier

object GlobalDAO {
  def loadModel(receptorPdbCode: String): InductiveClassifier[MLlibSVM, LabeledPoint] = {
    DB.withConnection { implicit c =>
      val result = SQL(
        """
        | SELECT r_model
        | FROM MODELS
        | WHERE r_pdbCode={r_pdbCode};
      """.stripMargin)
        .on("r_pdbCode" -> receptorPdbCode)
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