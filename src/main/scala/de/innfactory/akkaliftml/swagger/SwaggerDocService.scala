package de.innfactory.akkaliftml.swagger

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.swagger.akka._
import com.github.swagger.akka.model.Info
import de.innfactory.akkaliftml.als.AlsService

import scala.reflect.runtime.{universe => ru}

class SwaggerDocService(address: String, port: Int, system: ActorSystem)
  extends SwaggerHttpService
    with HasActorSystem {
  override implicit val actorSystem: ActorSystem = system
  override implicit val materializer: ActorMaterializer = ActorMaterializer()
  override val apiTypes = Seq(
    ru.typeOf[AlsService]
  )
  override val host = address + ":" + port
  override val info = Info(version = "1.0")
}