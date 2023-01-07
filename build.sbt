ThisBuild / organization := "com.github.fortega"
ThisBuild / version := "0.0.1"

// scoverage sbt plugin version restriction (newest have conflict with spark)
ThisBuild / scalaVersion := "2.12.15"

// Dependencies
val amqpClient = "com.rabbitmq" % "amqp-client" % "5.16.0"
val scalaTest = "org.scalatest" %% "scalatest" % "3.2.14" % Test
val slf4j = "org.slf4j" % "slf4j-simple" % "2.0.5"
val sparkSql = "org.apache.spark" %% "spark-sql" % "3.3.1"
val zio = {
  val version = "2.0.5"
  Seq(
    "dev.zio" %% "zio" % version,
    "dev.zio" %% "zio-streams" % version
  )
}

// Projects
val root = (project in file("."))
  .settings(
    name := "bigdata-architectures-protobuf"
  )

val core = (project in file("core"))
  .dependsOn(root)
  .settings(
    name := "bigdata-architectures-core",
    libraryDependencies ++= Seq(scalaTest, slf4j)
  )

val batch = (project in file("batch"))
  .dependsOn(core)
  .settings(
    name := "bigdata-architectures-batch",
    libraryDependencies += sparkSql
  )

val producer = (project in file("producer"))
  .dependsOn(core)
  .settings(
    name := "gps-events-producer",
    libraryDependencies ++= Seq(amqpClient, slf4j)
  )
  .enablePlugins(PackPlugin)

val lambda = (project in file("lambda"))
  .dependsOn(core)
  .settings(
    name := "bigdata-architectures-lambda",
    libraryDependencies ++= Seq(amqpClient) ++ zio
  )
  .enablePlugins(PackPlugin)

// ScalaPB
Compile / PB.targets := Seq(
  scalapb.gen() -> (core / Compile / sourceManaged).value / "scalapb"
)
