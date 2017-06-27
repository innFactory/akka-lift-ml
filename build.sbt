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
  Library.spark
)

