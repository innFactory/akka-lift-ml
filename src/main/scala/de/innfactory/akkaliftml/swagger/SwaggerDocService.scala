package de.innfactory.akkaliftml.swagger

import scala.reflect.runtime.{universe => ru}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.swagger.akka._
import com.github.swagger.akka.model.Info
import de.innfactory.akkaliftml.add.AddService
import de.innfactory.akkaliftml.hello.HelloService
import de.innfactory.akkaliftml.train.TrainService
import io.swagger.models.ExternalDocs
import io.swagger.models.auth.BasicAuthDefinition

class SwaggerDocService(address: String, port: Int, system: ActorSystem)
  extends SwaggerHttpService
    with HasActorSystem {
  override implicit val actorSystem: ActorSystem = system
  override implicit val materializer: ActorMaterializer = ActorMaterializer()
  override val apiTypes = Seq(
    ru.typeOf[AddService],
    ru.typeOf[HelloService],
    ru.typeOf[TrainService]
  )
  override val host = address + ":" + port
  override val info = Info(version = "1.0")
}