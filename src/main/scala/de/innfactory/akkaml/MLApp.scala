package de.innfactory.akkaml

import akka.actor._
import de.innfactory.akkaml.add.{AddActor, AddService}
import de.innfactory.akkaml.hello.{HelloActor, HelloService}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object MLApp {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("mlapp")

    system.actorOf(Props(new Master), "ml-serivce-master")

    Await.ready(system.whenTerminated, Duration.Inf)
  }
}

class Master extends Actor with ActorLogging with ActorSettings {
  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy
  private implicit val _ = context.dispatcher

  private val addService = createAddActor()
  private val helloService = createGreetingActor()
  context.watch(createHttpService(addService, helloService))

  log.info("Up and running")

  override def receive = {
    case Terminated(actor) => onTerminated(actor)
  }


  protected def createGreetingActor(): HelloService = {
    new HelloService(context.actorOf(Props[HelloActor]))
  }


  protected def createAddActor(): AddService = {
    new AddService(context.actorOf(Props[AddActor]))
  }


  protected def createHttpService(addService: AddService, helloSerivce: HelloService): ActorRef = {
    import settings.httpService._
    context
      .actorOf(HttpService.props(address, port, selfTimeout, addService, helloSerivce), HttpService.Name)
  }

  protected def onTerminated(actor: ActorRef): Unit = {
    log.error("Terminating the system because {} terminated!", actor)
    context.system.terminate()
  }
}
