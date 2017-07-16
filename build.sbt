import sbt.Keys._
import sbt._

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
  Library.hadoop,
  Library.hadoopAWS,
  TestLibrary.akkaHttpTestkit,
  TestLibrary.akkaTestkit,
  TestLibrary.scalaTest
)


mainClass in (Compile, run) := Some("de.innfactory.akkaliftml.MLApp")


scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings")

dockerfile in docker := {
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("java")
    add(artifact, artifactTargetPath)
    entryPoint(
      "java",
      "-jar",
      artifactTargetPath
    )
  }
}