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

  object fs {
    val target = mlConfig.getString("fs.target")
    val ratingsFormat = mlConfig.getString("fs.ratings-format")
    val loadLastOnStartup = mlConfig.getString("fs.load-last-on-startup")
  }

  object aws {
    val location = mlConfig.getString("aws.location")
    val accessKeyId = mlConfig.getString("aws.access-key-id")
    val secretAccessKey = mlConfig.getString("aws.secret-access-key")
  }

  object spark {
    val defaultMaster = mlConfig.getString("spark.default-master")
    val port = mlConfig.getInt("spark.port")
    val appName = mlConfig.getString("spark.app-name")
    val executorMemory = mlConfig.getString("spark.executor-memory")
  }

  private val mlConfig = system.settings.config.getConfig("mlconfig")

  private def getDuration(key: String) = FiniteDuration(mlConfig.getDuration(key, MILLISECONDS), MILLISECONDS)
}

trait ActorSettings {
  this: Actor =>
  val settings: Settings = Settings(context.system)
}
