scalaVersion := "2.13.10"

val core = (project in file("core"))
  .settings(
    name := "bigdata-architectures-core",
    organization := "com.github.fortega",
    version := "0.0.1",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.9.0",
      "org.scalatest" %% "scalatest" % "3.2.14" % Test
    )
  )
