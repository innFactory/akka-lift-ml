package de.innfactory.akkaliftml.train

import akka.actor.{Actor, ActorLogging}
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel

import scala.concurrent.{Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global

object TrainActor {

  case class TrainWithModel(name: TrainingModel)

  case class TrainingResponse(responseMessage: String, train: Boolean)

  case class GetCurrentStatus()

  case class SaveModel(rmse : Double, model : MatrixFactorizationModel)

}

class TrainActor extends Actor with ActorLogging {

  import TrainActor._

  var status = false
  var modelRmse = Double.MaxValue
  var currentModel : Option[MatrixFactorizationModel] = None

  def receive: Receive = {

    case SaveModel(rmse, model) => {
      if(rmse < modelRmse) {
        currentModel = Some(model)
        modelRmse = rmse
      }
    }
    case TrainWithModel(model) => {
      if (status) {
        sender ! TrainingResponse(s"Training is running, please wait!", status)
      } else {
        sender ! TrainingResponse(s"Training started with ${model.toString}", status)
        status = true
        Future {
          blocking {
            ALSTrainer.sparkJob(model)
          }
        }.map(model => {
          print("got a new model with RMSE - ")
          print(model._1)
          self ! SaveModel(model._1, model._2)
          status = false
        }).recoverWith {
          case e: Exception => {
            println("task failed")
            status = false
            Future.failed(e)
          }

        }

      }
    }
    case GetCurrentStatus => sender ! TrainingResponse(s"Training is running [${status}] Current best RMSE (${modelRmse})", status)
  }

}