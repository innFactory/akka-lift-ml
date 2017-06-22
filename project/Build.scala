import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object Build extends AutoPlugin {

  override def requires = JvmPlugin

  override def trigger = allRequirements

  override def projectSettings =
    Vector(
    // Core settings
    organization := "de.innfactory",
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalaVersion := Version.Scala,
    crossScalaVersions := Vector(scalaVersion.value),
    scalacOptions ++= Vector(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding", "UTF-8",
      "-Xexperimental"
    ),
    unmanagedSourceDirectories.in(Compile) := Vector(scalaSource.in(Compile).value),
    unmanagedSourceDirectories.in(Test) := Vector(scalaSource.in(Test).value)
  )
}
