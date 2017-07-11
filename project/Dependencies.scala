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
  final val Spark = "2.1.0"
  final val Scope = "3.6.0"
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
  val spark = "org.apache.spark" %% "spark-core" % Version.Spark
  val sparkSQL = "org.apache.spark" %% "spark-sql" % Version.Spark
  val sparkMLlib = "org.apache.spark" %% "spark-mllib" % Version.Spark
  val sparkHive = "org.apache.spark" %% "spark-hive" % Version.Spark
  val scopt = "com.github.scopt" %% "scopt" % Version.Scope

}

object TestVersion {
  final val akkaTestkit = "2.5.3"
  final val akkaHttpTestkit =  "10.0.9"
  final val scalaTest = "3.0.1"
}

object TestLibrary {
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % TestVersion.akkaTestkit
  val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % TestVersion.akkaHttpTestkit
  val scalaTest = "org.scalatest" %% "scalatest" % TestVersion.scalaTest % "test"
}
