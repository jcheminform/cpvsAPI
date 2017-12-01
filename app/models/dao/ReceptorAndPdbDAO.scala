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
          | SELECT r_name, pdbCode FROM RECEPTORS;    
        """.stripMargin).apply()

      results.map { row =>
        ReceptorAndPdb(row[String]("r_name"), row[String]("pdbCode"))
      }.force.toList
    }
  }
  
  
}


