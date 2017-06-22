package de.innfactory.akkaml

import akka.actor.{ Actor, ExtendedActorSystem, Extension, ExtensionKey }

import scala.concurrent.duration.{ FiniteDuration, MILLISECONDS }

object Settings extends ExtensionKey[Settings]

class Settings(system: ExtendedActorSystem) extends Extension {

  object httpService {
    val address: String = mlConfig.getString("http-service.address")
    val port: Int = mlConfig.getInt("http-service.port")
    val selfTimeout: FiniteDuration = getDuration("http-service.self-timeout")
  }

  private val mlConfig = system.settings.config.getConfig("mlconfig")

  private def getDuration(key: String) = FiniteDuration(mlConfig.getDuration(key, MILLISECONDS), MILLISECONDS)
}

trait ActorSettings {
  this: Actor =>
  val settings: Settings = Settings(context.system)
}
