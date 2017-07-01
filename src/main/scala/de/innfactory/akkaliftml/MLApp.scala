package de.innfactory.akkaliftml

import akka.actor._
import de.innfactory.akkaliftml.als.{AlsActor, AlsService}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object MLApp {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("mlliftapp")

    system.actorOf(Props(new Master), "ml-lift-master")

    Await.ready(system.whenTerminated, Duration.Inf)
  }
}

class Master extends Actor with ActorLogging with ActorSettings {
  override val supervisorStrategy = SupervisorStrategy.defaultStrategy
  private implicit val _ = context.dispatcher

  private val alsService = createAlsService()
  context.watch(createHttpService(alsService))

  log.info("Up and running")

  override def receive = {
    case Terminated(actor) => onTerminated(actor)
  }


  protected def createAlsService(): AlsService = {
    new AlsService(context.actorOf(Props[AlsActor]))
  }


  protected def createHttpService(trainService: AlsService): ActorRef = {
    import settings.httpService._
    context
      .actorOf(HttpService.props(address, port, selfTimeout, trainService), HttpService.Name)
  }

  protected def onTerminated(actor: ActorRef): Unit = {
    log.error("Terminating the system because {} terminated!", actor)
    context.system.terminate()
  }
}
