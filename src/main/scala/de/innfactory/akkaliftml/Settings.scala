package de.innfactory.akkaliftml

import akka.actor.{ Actor, ExtendedActorSystem, Extension, ExtensionKey }

import scala.concurrent.duration.{ FiniteDuration, MILLISECONDS }

object Settings extends ExtensionKey[Settings]

class Settings(system: ExtendedActorSystem) extends Extension {

  object httpService {
    val address: String = mlConfig.getString("http-service.address")
    val port: Int = mlConfig.getInt("http-service.port")
    val selfTimeout: FiniteDuration = getDuration("http-service.self-timeout")
  }

  object models {
    val fs = system.settings.config.getConfig("models.fs")
    val location = system.settings.config.getConfig("models.location")
  }

  object spark {
    val master = system.settings.config.getConfig("spark.master")
    val port = system.settings.config.getConfig("spark.port")
    val appName = system.settings.config.getConfig("spark.app-name")
  }

  private val mlConfig = system.settings.config.getConfig("mlconfig")

  private def getDuration(key: String) = FiniteDuration(mlConfig.getDuration(key, MILLISECONDS), MILLISECONDS)
}

trait ActorSettings {
  this: Actor =>
  val settings: Settings = Settings(context.system)
}
