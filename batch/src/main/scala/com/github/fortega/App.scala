package com.github.fortega

import org.apache.spark.sql.SparkSession
import scala.util.Try
import scala.util.Failure
import scala.util.Success

object App {
  def main(cmdArgs: Array[String]): Unit = {
    sparkSession match {
      case Failure(error) => handleError("spark session", error)
      case Success(spark) =>
        println(s"Spark version: ${spark.version}")
        spark.close
    }
  }

  private def handleError(name: String, error: Throwable) =
    sys.error(s"$name: ${error.getMessage} ")

  private def sparkSession: Try[SparkSession] = Try {
    val master = sys.env.get("(SPARK_ENV_LOADED") match {
      case Some(value) if (value == "1") => None
      case None                          => Some("local[*]")
    }

    master
      .foldLeft(SparkSession.builder) { case (builder, master) =>
        builder.master(master)
      }
      .getOrCreate
  }
}
