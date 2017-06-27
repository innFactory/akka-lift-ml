package de.innfactory.akkaliftml

import javax.ws.rs.Path

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directives, RouteConcatenation}
import akka.stream.{ActorMaterializer, Materializer}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import de.innfactory.akkaliftml.add.AddService
import de.innfactory.akkaliftml.hello.HelloService
import de.innfactory.akkaliftml.swagger.{SwaggerDocService, SwaggerUIService}
import de.innfactory.akkaliftml.train.TrainService

import scala.concurrent.ExecutionContext
import io.swagger.annotations._

import scala.util.{Failure, Success}

object HttpService {

  private case object Stop

  // $COVERAGE-OFF$
  final val Name = "http-service"
  // $COVERAGE-ON$

  def props(
             address: String,
             port: Int,
             internalTimeout: Timeout,
             add: AddService,
             hello: HelloService,
             trainer: TrainService
           ): Props =
    Props(new HttpService(address, port, internalTimeout, add, hello, trainer))

  private def route(
                            httpService: ActorRef,
                            add: AddService,
                            hello: HelloService,
                            trainer: TrainService,
                            swaggerDocService: SwaggerDocService
                          )(implicit ec: ExecutionContext, mat: Materializer) = {
    import Directives._

    // format: OFF
    def assets = pathPrefix("swagger") {
      getFromResourceDirectory("swagger") ~ pathSingleSlash(get(redirect("index.html", StatusCodes.PermanentRedirect)))
    }

    def stop = pathSingleSlash {
      delete {
        complete {
          httpService ! Stop
          "Stopping ..."
        }
      }
    }

    cors()(
        assets ~
        add.route ~
        hello.route ~
        trainer.route ~
        swaggerDocService.routes
    )
  }
}

class HttpService(address: String,
                  port: Int,
                  internalTimeout: Timeout,
                  add: AddService,
                  hello: HelloService,
                  trainer: TrainService)
  extends Actor with ActorLogging {

  import HttpService._
  import context.dispatcher

  private implicit val mat = ActorMaterializer()

  Http(context.system)
    .bindAndHandle(
      route(self, add, hello, trainer, new SwaggerDocService(address, port, context.system)),
      address,
      port)
    .pipeTo(self)

  override def receive = binding

  private def binding: Receive = {
    case serverBinding@Http.ServerBinding(address) =>
      log.info("Listening on {}", address)
      context.become(bound(serverBinding))

    case Status.Failure(cause) =>
      log.error(cause, s"Can't bind to $address:$port")
      context.stop(self)
  }

  private def bound(serverBinding: Http.ServerBinding): Receive = {
    case Stop =>
      serverBinding.unbind()
      context.stop(self)
  }
}
