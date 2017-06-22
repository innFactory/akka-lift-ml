package de.innfactory.akkaml.swagger

import scala.reflect.runtime.{universe => ru}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.swagger.akka._
import com.github.swagger.akka.model.Info
import de.innfactory.akkaml.add.AddService
import de.innfactory.akkaml.hello.HelloService
import io.swagger.models.ExternalDocs
import io.swagger.models.auth.BasicAuthDefinition

class SwaggerDocService(system: ActorSystem) extends SwaggerHttpService with HasActorSystem {
  override implicit val actorSystem: ActorSystem = system
  override implicit val materializer: ActorMaterializer = ActorMaterializer()
  override val apiTypes = Seq(ru.typeOf[AddService], ru.typeOf[HelloService])
  override val host = "localhost:12345"
  override val info = Info(version = "1.0")
  override val externalDocs = Some(new ExternalDocs("Core Docs", "http://acme.com/docs"))
  override val securitySchemeDefinitions = Map("basicAuth" -> new BasicAuthDefinition())
}