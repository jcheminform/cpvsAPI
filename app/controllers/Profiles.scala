package controllers

import org.openscience.cdk.DefaultChemObjectBuilder
import org.openscience.cdk.exception.InvalidSmilesException
import org.openscience.cdk.smiles.SmilesParser

import models.Prediction
import models.Prediction.PredictionWrites
import models.Score
import models.Score.ScoreWrites
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller
import scala.io.Source

object Profiles extends Controller {
 
  def predictionByLigandId(smiles: String) = Action {
    val SmilesArray = smiles.split("\n").map(_.trim).filter(_ != "")
    SmilesArray.foreach(println(_))
    var counter = 1
    try {

      val smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance())

      //For getting exception if smiles is invalid
      //Although we can use map, this is fastest way to map array
      while (counter <= SmilesArray.length) {
        smilesParser.parseSmiles(SmilesArray(counter - 1))
        counter += 1
      }

      val profilePredictions = Prediction.predictProfile(SmilesArray)
      Ok(Json.obj("predictions" -> profilePredictions))
    } catch {
      case exec: InvalidSmilesException =>
        println("\n ###########  An invalid smile with following message  ########## " + "\n" + exec.getStackTraceString)
        BadRequest("Invalid Smiles at line " + counter)
    }

  }

  def dockingByLigandId(smiles: String) = Action {
    try {
      //For getting exception if smiles is invalid
      val smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance())
      val molecule = smilesParser.parseSmiles(smiles)

      //For getting docking score
      val dockingScore = Score.dockProfile(smiles)
      Ok(Json.obj("Score" -> dockingScore))
    } catch {
      case exec: InvalidSmilesException =>
        println("\n ###########  An invalid smile with following message  ########## " + "\n" + exec.getStackTraceString)
        BadRequest("Invalid Smiles")
    }
  }

  def welcome() = Action {
    Redirect(url = "/assets/docs/index.html")
  }

}
