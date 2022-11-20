package com.github.fortega

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import scala.util.{Try, Failure, Success}
import com.github.fortega.model.gps.Event
import com.github.fortega.types.InvalidReasonInstances._
import com.github.fortega.types.InvalidReasonSyntax._
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator
import org.apache.flink.streaming.api.functions.sink.PrintSink
import org.apache.flink.streaming.api.functions.sink.SinkFunction

object App {
  def main(cmdArgs: Array[String]): Unit = getEnv.map { env =>
    val events = env.fromSequence(1, 100).map(i => fakeEvent(i))

    events
      .map { _.validated.toString }
      .print()
    env.execute
  } match {
    case Failure(error) => sys.error(s"error: ${error.getMessage()}")
    case Success(value) => println("success")
  }

  private def fakeEvent(i: Long) = Event(
    deviceId = (i % 10).toInt,
    time = i * 10,
    longitude = i.toDouble,
    latitude = i.toDouble,
    altitude = i.toDouble,
    velocity = i.toInt,
    angle = (i % 360).toInt
  )

  private def getEnv: Try[StreamExecutionEnvironment] = Try {
    StreamExecutionEnvironment.getExecutionEnvironment
  }
}
