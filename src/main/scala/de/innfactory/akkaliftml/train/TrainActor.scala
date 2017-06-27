package de.innfactory.akkaliftml.train
import akka.actor.{Actor, ActorLogging}
import org.apache.spark.SparkConf

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
      sender ! TrainingResponse(s"Training started with ${model.toString}", status)
      status = true

//
//      // initialise spark contex
//      val conf = new SparkConf().setAppName("HelloWorld")
//      val sc = new SparkContext(conf)
//
//      // do stuff
//      println("************")
//      println("************")
//      println("Hello, world!")
//      println("************")
//      println("************")
//
//      // terminate spark context
//      sc.stop()

    }
    case GetCurrentStatus => sender ! TrainingResponse(s"Training is running [${status}]", status)
  }
}