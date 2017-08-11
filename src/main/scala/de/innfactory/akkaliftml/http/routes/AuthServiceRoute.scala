package de.innfactory.akkaliftml.http.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import de.innfactory.akkaliftml.http.SecurityDirectives
import de.innfactory.akkaliftml.services.AuthService

import scala.concurrent.ExecutionContext

class AuthServiceRoute(val authService: AuthService)(implicit executionContext: ExecutionContext) extends FailFastCirceSupport with SecurityDirectives {

  import StatusCodes._

  val route = pathPrefix("auth") {
     path("") {
        pathEndOrSingleSlash {
          get {
            complete(OK -> "Auth service available!")
          }
        }
      }
  }

}
