package com.github.fortega

import com.github.fortega.model.gps.Event
import com.github.fortega.types.InvalidReasonInstances._
import com.github.fortega.types.InvalidReasonSyntax._
import org.apache.spark.sql.SparkSession
import scala.util.{Try, Failure, Success}

object App {
  def main(cmdArgs: Array[String]): Unit = cmdArgs match {
    case Array(in, out) => run(in, out)
    case _              => sys.error("missing input / output")
  }

  private def run(in: String, out: String): Unit = createSpark.map { spark =>
    import spark.implicits._

    val events = spark.read.parquet(in).as[Event]
    val validated = events.map(_.validated)
    validated.write.parquet(out)

    spark.stop
  } match {
    case Failure(error) => sys.error(s"error: ${error.getMessage}")
    case Success(_)     => println("success")
  }

  private def createSpark: Try[SparkSession] = Try {
    val master = sys.env.get("(SPARK_ENV_LOADED") match {
      case Some(_) => None
      case None    => Some("local[*]")
    }

    master
      .foldLeft(SparkSession.builder) { case (builder, master) =>
        builder.master(master)
      }
      .getOrCreate
  }
}
