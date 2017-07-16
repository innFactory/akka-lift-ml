package de.innfactory.akkaliftml

import akka.actor.{Actor, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}

import scala.concurrent.duration._

//object Settings with Extension[Settings]

object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {

  override def lookup = Settings

  override def createExtension(system: ExtendedActorSystem) = new SettingsImpl(system)

  override def get(system: ActorSystem): SettingsImpl = super.get(system)
}

class SettingsImpl(system: ExtendedActorSystem) extends Extension {

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
    val jar = mlConfig.getString("spark.jar")
  }

  private val mlConfig = system.settings.config.getConfig("mlconfig")

  private def getDuration(key: String) = FiniteDuration(mlConfig.getDuration(key, MILLISECONDS), MILLISECONDS)
}

trait ActorSettings {
  this: Actor =>
  val settings = Settings(context.system)
}
