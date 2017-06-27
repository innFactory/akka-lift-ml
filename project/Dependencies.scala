import sbt._

object Version {
  final val akka = "2.5.3"
  final val akkaHttp = "10.0.8"
  final val Scala = "2.11.8"
  final val AkkaLog4j = "1.4.0"
  final val Log4j = "2.8.2"
  final val swagger = "1.5.14"
  final val swaggerAkka = "0.9.2"
  final val akkaHttpCors = "0.2.1"
  final val Spark = "2.1.1"
}

object Library {

  val swagger = "io.swagger" % "swagger-jaxrs" % Version.swagger
  val swaggerAkka = "com.github.swagger-akka-http" %% "swagger-akka-http" % Version.swaggerAkka
  val akka = "com.typesafe.akka" %% "akka-http" % Version.akkaHttp
  val akkaHttpSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % Version.akkaHttp
  val akkaPersistence = "com.typesafe.akka" %% "akka-persistence" % Version.akka
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % Version.akka
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % Version.akka
  val akkaHttpCors = "ch.megard" %% "akka-http-cors" % Version.akkaHttpCors
  val akkaLog4j = "de.heikoseeberger" %% "akka-log4j" % Version.AkkaLog4j
  val log4jCore = "org.apache.logging.log4j" % "log4j-core" % Version.Log4j
  val slf4jLog4jBridge = "org.apache.logging.log4j" % "log4j-slf4j-impl" % Version.Log4j
  val spark = "org.apache.spark" %% "spark-core" % Version.Spark % "provided"

}
