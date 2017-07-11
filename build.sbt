lazy val akkaml = project
  .copy(id = "akkaml")
  .in(file("."))

name := "akkaliftml"


libraryDependencies ++= Vector(
  Library.swagger,
  Library.swaggerAkka,
  Library.akka,
  Library.akkaPersistence,
  Library.akkaHttpSprayJson,
  Library.akkaHttpCors,
  Library.akkaActor,
  Library.akkaStream,
  Library.akkaHttpCors,
  Library.akkaLog4j,
  Library.log4jCore,
  Library.slf4jLog4jBridge,
  Library.scopt,
  Library.spark,
  Library.sparkSQL,
  Library.sparkMLlib,
  Library.sparkHive,
  TestLibrary.akkaHttpTestkit,
  TestLibrary.akkaTestkit,
  TestLibrary.scalaTest
)


mainClass in (Compile, run) := Some("de.innfactory.akkaliftml.MLApp")

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)