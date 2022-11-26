ThisBuild / organization := "com.github.fortega"
ThisBuild / version := "0.0.1"

// scoverage sbt plugin version restriction (newest have conflict with spark)
ThisBuild / scalaVersion := "2.12.15"

// Cats
scalacOptions += "-Ypartial-unification"

// Dependencies
val amqpClient = "com.rabbitmq" % "amqp-client" % "5.16.0"
val sparkSql = "org.apache.spark" %% "spark-sql" % "3.3.1"
val cats = "org.typelevel" %% "cats-core" % "2.9.0"
val slf4j = "org.slf4j" % "slf4j-simple" % "2.0.5"
val scalaTest = "org.scalatest" %% "scalatest" % "3.2.14"

// Projects
val root = (project in file("."))
  .settings(
    name := "bigdata-architectures-protobuf"
  )

val core = (project in file("core"))
  .dependsOn(root)
  .settings(
    name := "bigdata-architectures-core",
    libraryDependencies ++= Seq(cats, slf4j),
    libraryDependencies += scalaTest % Test
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
    libraryDependencies += amqpClient
  )

val lambda = (project in file("lambda"))
  .dependsOn(core)
  .settings(
    name := "bigdata-architectures-lambda",
    libraryDependencies += amqpClient
  )

// ScalaPB
Compile / PB.targets := Seq(
  scalapb.gen() -> (core / Compile / sourceManaged).value / "scalapb"
)
