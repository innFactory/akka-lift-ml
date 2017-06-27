package de.innfactory.akkaliftml

import akka.actor._
import de.innfactory.akkaliftml.add.{AddActor, AddService}
import de.innfactory.akkaliftml.hello.{HelloActor, HelloService}
import de.innfactory.akkaliftml.train.{TrainActor, TrainService}

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
  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy
  private implicit val _ = context.dispatcher

  private val addService = createAddService()
  private val helloService = createGreetingActor()
  private val trainService = createTrainService()
  context.watch(createHttpService(addService, helloService))

  log.info("Up and running")

  override def receive = {
    case Terminated(actor) => onTerminated(actor)
  }


  protected def createGreetingActor(): HelloService = {
    new HelloService(context.actorOf(Props[HelloActor]))
  }


  protected def createAddService(): AddService = {
    new AddService(context.actorOf(Props[AddActor]))
  }

  protected def createTrainService(): TrainService = {
    new TrainService(context.actorOf(Props[TrainActor]))
  }


  protected def createHttpService(addService: AddService, helloSerivce: HelloService): ActorRef = {
    import settings.httpService._
    context
      .actorOf(HttpService.props(address, port, selfTimeout, addService, helloSerivce, trainService), HttpService.Name)
  }

  protected def onTerminated(actor: ActorRef): Unit = {
    log.error("Terminating the system because {} terminated!", actor)
    context.system.terminate()
  }
}
