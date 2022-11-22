package com.github.fortega

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import scala.util.Try
import com.github.fortega.model.gps.Event
import scala.util.Failure
import scala.util.Success

object App {
  def main(cmdArgs: Array[String]): Unit = cmdArgs match {
    case Array(server, queue, msgs) if Try(msgs.toInt).isSuccess =>
      usingChannel(server) { channel =>
        channel.queueDeclare(queue, false, false, false, null)

        Stream
          .range(0, msgs.toInt)
          .map(createEvent)
          .map(_.toByteArray)
          .foreach(channel.basicPublish("", queue, null, _))
      }
    case _ => sys.error("invalid arguments")
  }

  private def createEvent(i: Int): Event = Event(
    deviceId = i % 10,
    time = i * 10L,
    longitude = (i % 360) - 180,
    latitude = (i % 180) - 90,
    altitude = (i % 500).toDouble,
    velocity = (i % 200),
    angle = (i % 360)
  )

  private def usingChannel[A](
      host: String
  )(
      f: Channel => A
  ): Try[A] = Try {
    val factory = new ConnectionFactory
    factory.setHost(host)
    val connection = factory.newConnection
    val result = f(connection.createChannel)
    connection.close()

    result
  }
}
