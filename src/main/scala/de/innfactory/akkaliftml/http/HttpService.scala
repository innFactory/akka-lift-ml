package de.innfactory.akkaliftml.http

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server.Directives._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import de.innfactory.akkaliftml.http.routes.{AlsServiceRoute, AuthServiceRoute, SwaggerUIRoute}
import de.innfactory.akkaliftml.services.{AuthService, SwaggerDocService}
import de.innfactory.akkaliftml.utils.Configuration

import scala.concurrent.ExecutionContext

class HttpService(authService: AuthService,
                  alsService: ActorRef
                 )(implicit executionContext: ExecutionContext, actorSystem: ActorSystem) extends Configuration{

  val authRouter = new AuthServiceRoute(authService)
  val alsRouter = new AlsServiceRoute(alsService)

  val swaggerRouter = new SwaggerUIRoute()
  val swaggerDocService = new SwaggerDocService(httpHost, httpPort, actorSystem)

  val settings = CorsSettings.defaultSettings.copy(allowedMethods = List(GET, POST, PUT, HEAD, OPTIONS, DELETE))

  // $COVERAGE-OFF$Routes are tested seperatly
  val routes =
    pathPrefix("v1") {
      cors(settings) {
        swaggerDocService.routes ~
        swaggerRouter.route ~
        authRouter.route ~
        alsRouter.route
      }
    }
  // $COVERAGE-ON$

}
