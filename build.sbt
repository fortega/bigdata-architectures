organization := "com.github.fortega"
version := "0.0.1"

scalacOptions += "-Ypartial-unification"

val core = (project in file("core"))
  .settings(
    name := "bigdata-architectures-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.9.0",
      "org.scalatest" %% "scalatest" % "3.2.14" % Test
    )
  )

val batch = (project in file("batch"))
  .dependsOn(core)
  .settings(
    name := "bigdata-architectures-batch",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-sql" % "3.3.1"
    )
  )

val flinkVersion = "1.16.0"
val lambda = (project in file("lambda"))
  .dependsOn(core)
  .settings(
    name := "bigdata-architectures-lambda",
    libraryDependencies ++= Seq(
      "org.apache.flink" %% "flink-streaming-scala" % flinkVersion,
      "org.apache.flink" % "flink-clients" % flinkVersion
    )
  )
