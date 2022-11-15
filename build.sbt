scalaVersion := "2.12.17"

val core = (project in file("core"))
  .settings(
    name := "bigdata-architectures-core",
    organization := "com.github.fortega",
    version := "0.0.1",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.14" % Test
    )
  )
