package de.innfactory.akkaliftml.train

import akka.actor.{Actor, ActorLogging}

import scala.concurrent.{Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global

object TrainActor {

  case class TrainWithModel(name: TrainingModel)

  case class TrainingResponse(responseMessage: String, train: Boolean)

  case class GetCurrentStatus()

}

class TrainActor extends Actor with ActorLogging {

  import TrainActor._

  var status = false

  def receive: Receive = {
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
          print("got a new model")
          print(model)
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
    case GetCurrentStatus => sender ! TrainingResponse(s"Training is running [${status}]", status)
  }

}