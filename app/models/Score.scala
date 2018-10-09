package models

import scala.language.postfixOps

import controllers.Global.{ obabelPath, svmModel, oldSig2ID, receptorName, receptorPdbCode, resourcesHome }
import models.dao.ScoreDAO
import play.api.Logger
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.JsPath
import play.api.libs.json.Writes
import se.uu.farmbio.vs.{ ConformerPipeline, PosePipeline }

object Score {

  implicit val ScoreWrites: Writes[Score] = (
    (JsPath \ "r_pdbCode").write[String] and
    (JsPath \ "l_id").write[String] and
    (JsPath \ "l_score").write[String])(unlift(Score.unapply))


  def dockProfile(smiles: String): Score =
    {
      //Convert smi to inchiKey
      val smiToInchiKey =
        ConformerPipeline.pipeString(
          smiles,
          List(obabelPath, "-ismi", "-oinchikey")).trim()

      //Row Existance test
      var result: Score = null
      val scoreExist = ScoreDAO.scoreExistCheck(smiToInchiKey, receptorPdbCode)

      //If Score exists in database, then read and display to user
      if (scoreExist == 1) {
        println("\n######  Loading Score from database  ###### \n")
        result = ScoreDAO.scoreByLigandIdAndPdbCode(smiToInchiKey, receptorPdbCode)
      } //Else dock the molecule and show result to user
      else {
        //Use local sh file if VINA_DOCKING is set
        val vinaDockingPath = if (System.getenv("VINA_DOCKING") != null) {
          Logger.info("JOB_INFO: using local multivana: " + System.getenv("VINA_DOCKING"))
          System.getenv("VINA_DOCKING")
        } else {
          Logger.info("JOB_ERROR: VINA_DOCKING is not set")
          "VINA_DOCKING is not set"
        }

        //Use local vina conf.txt file if VINA_CONF is set
        val vinaConfPath = if (System.getenv("VINA_CONF") != null) {
          Logger.info("JOB_INFO: using local vina conf: " + System.getenv("VINA_CONF"))
          System.getenv("VINA_CONF")
        } else {
          Logger.info("JOB_ERROR: VINA_CONF is not set")
          "VINA_CONF is not set"
        }

        //Create ReceptorPath
        val rNameWithExtension = receptorName + ".pdbqt"
        val receptorPath = resourcesHome + rNameWithExtension
        Logger.info("JOB_INFO: The receptor file complete path is " + receptorPath)

        //Convert SMILES to SDF using obabel
        val sdfLigand: String = ConformerPipeline.pipeString(
          smiles,
          List(obabelPath, "-ismi", "-osdf", "--gen3d"))

        //Compute Score using docking
        //Convert sdf ligand to pdbqt format using obabel
        val pdbqtLigand: String = "MODEL\n" + ConformerPipeline.pipeString(
          sdfLigand,
          List(obabelPath, "-h", "-i", "sdf", "-o", "pdbqt")).trim() + "\nENDMDL"

        //Docking pdbqtLigand against receptor using VINA
        val dockedpdbqt: String = ConformerPipeline.pipeString(
          pdbqtLigand,
          List(vinaDockingPath, "--receptor",
            receptorPath, "--config", vinaConfPath))

        //Convert pdbqt ligand to sdf format using obabel
        val pdbqtToSdfLigand: String = ConformerPipeline.pipeString(
          dockedpdbqt,
          List(obabelPath, "-i", "pdbqt", "-o", "sdf"))

        //Cleaning Molecule after docking and getting score
        val lScore = PosePipeline.parseScore(ConformerPipeline.cleanPoses(pdbqtToSdfLigand, false).trim).toString

        println("The Docking Score is  " + lScore)

        //Save Score in DOCKED_LIGANDS
        ScoreDAO.saveLigandScoreById(receptorPdbCode, smiToInchiKey, lScore)
        result = Score(receptorPdbCode, smiToInchiKey, lScore)
      }
      result
    }

}

case class Score(r_pdbCode: String, l_id: String, l_score: String)
