package de.innfactory.akkaliftml.swagger

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import de.innfactory.akkaliftml.DefaultJsonFormats

import scala.concurrent.ExecutionContext

class SwaggerUIService()(implicit executionContext: ExecutionContext)
  extends Directives with DefaultJsonFormats {


  def assets = pathPrefix("swagger") {
    getFromResourceDirectory("swagger") ~ pathSingleSlash(get(redirect("index.html", StatusCodes.PermanentRedirect)))
  }

  val route = assets


}

