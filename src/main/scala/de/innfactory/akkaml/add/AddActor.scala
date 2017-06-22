package de.innfactory.akkaml.add

import akka.actor.{Actor, ActorLogging}

object AddActor {
  case class AddRequest(numbers: Array[Int])
  case class AddResponse(sum: Int)
}

class AddActor extends Actor with ActorLogging {
  import AddActor._

  def receive: Receive = {
    case request: AddRequest => {
      log.debug("adding numbers")
      sender ! AddResponse(request.numbers.reduce(_ + _))
    }
  }
}