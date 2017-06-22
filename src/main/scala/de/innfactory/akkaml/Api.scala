package de.innfactory.akkaml

import akka.http.scaladsl.server.RouteConcatenation
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives
import de.innfactory.akkaml.add.AddService
import de.innfactory.akkaml.hello.HelloService
import de.innfactory.akkaml.swagger.{SwaggerDocService, SwaggerUIService}

/**
 * The REST API layer. It exposes the REST services, but does not provide any
 * web server interface.<br/>
 * Notice that it requires to be mixed in with ``core.CoreActors``, which provides access
 * to the top-level actors that make up the system.
 */
trait Api extends RouteConcatenation {
  this: CoreActors with Core =>

  private implicit val _ = system.dispatcher

  val routes =
    cors() (
      new SwaggerUIService().route ~
      new AddService(add).route ~
      new HelloService(hello).route ~
      new SwaggerDocService(system).routes)

}
