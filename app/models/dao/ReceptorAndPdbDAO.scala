package models.dao

import anorm._
import models.ReceptorAndPdb
import play.api.db.DB
import play.api.Play.current

object ReceptorAndPdbDAO {
  
  def receptorAndPdbCodeIndex(): List[ReceptorAndPdb] = {
    DB.withConnection { implicit c =>
      val results = SQL(
        """
          | SELECT r_name, r_pdbCode FROM MODELS;    
        """.stripMargin).apply()

      results.map { row =>
        ReceptorAndPdb(row[String]("r_name"), row[String]("r_pdbCode"))
      }.force.toList
    }
  }
  
  
}


