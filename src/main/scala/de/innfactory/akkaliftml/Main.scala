package de.innfactory.akkaliftml

import akka.actor.{ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import de.innfactory.akkaliftml.http.HttpService
import de.innfactory.akkaliftml.services.AuthService
import de.innfactory.akkaliftml.services.als.AlsService
import de.innfactory.akkaliftml.services.als.AlsService.Init
import de.innfactory.akkaliftml.utils.{AWSCognitoValidation, Configuration, FlywayService}

import scala.concurrent.ExecutionContext

object Main extends App with Configuration {
  // $COVERAGE-OFF$Main Application Wrapper
  implicit val actorSystem = ActorSystem()
  implicit val executor: ExecutionContext = actorSystem.dispatcher
  implicit val log: LoggingAdapter = Logging(actorSystem, getClass)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val flywayService = new FlywayService(jdbcUrl, dbUser, dbPassword)
  flywayService.migrateDatabaseSchema

  val authService = new AuthService(new AWSCognitoValidation(authCognito, log))

  val alsService = actorSystem.actorOf(AlsService.props)
  alsService.tell(Init(), alsService)

  val httpService = new HttpService(authService, alsService)

  Http().bindAndHandle(httpService.routes, httpHost, httpPort)
  // $COVERAGE-ON$
}
