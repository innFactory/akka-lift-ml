package de.innfactory.akkaliftml.utils

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

trait Configuration {
  protected val config : Config = ConfigFactory.load()
  private val httpConfig = config.getConfig("http")
  private val databaseConfig = config.getConfig("database")
  private val authenticationConfig = config.getConfig("auth")
  private val mlConfig = config.getConfig("mlconfig")

  val httpHost = httpConfig.getString("interface")
  val httpPort = httpConfig.getInt("port")
  val httpSelfTimeout = httpConfig.getDuration("self-timeout")

  val jdbcUrl = databaseConfig.getString("db.url")
  val dbUser = databaseConfig.getString("db.user")
  val dbPassword = databaseConfig.getString("db.password")

  val authCognito = authenticationConfig.getString("cognito")
  val allowAll = authenticationConfig.getBoolean("allow-all")

  val fsTarget = mlConfig.getString("fs.target")
  val fsRatingsFormat = mlConfig.getString("fs.ratings-format")
  val fsLoadLastOnStartup = mlConfig.getString("fs.load-last-on-startup")

  val awsLocation = mlConfig.getString("aws.location")
  val awsAccessKeyId = mlConfig.getString("aws.access-key-id")
  val awsSecretAccessKey = mlConfig.getString("aws.secret-access-key")

  val sparkDefaultMaster = mlConfig.getString("spark.default-master")
  val sparkPort = mlConfig.getInt("spark.port")
  val sparkAppName = mlConfig.getString("spark.app-name")
  val sparkExecutorMemory = mlConfig.getString("spark.executor-memory")
  val sparkDriverMemory = mlConfig.getString("spark.driver-memory")
  val sparkJar = mlConfig.getString("spark.jar")

  val alsTimeout = getDuration("mlconfig.als.timeout")

  private def getDuration(key: String) = FiniteDuration(config.getDuration(key, MILLISECONDS), MILLISECONDS)
}
