package com.github.fortega

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import scala.util.Try
import com.github.fortega.model.gps.Event

object App {
  val routingKey = "queue"
  def main(cmdArgs: Array[String]): Unit = usingChannel("localhost") {
    channel =>
      channel.queueDeclare(routingKey, false, false, false, null)

      Stream.from(1).map { i =>
        require(i < 1000000)
        Event(
          deviceId = i % 10,
          time = i * 10L,
          longitude = (i % 360) - 180,
          latitude = (i % 180) - 90,
          altitude = (i % 500).toDouble,
          velocity = (i % 200),
          angle = (i % 360)
        )
      }.foreach { event =>
        channel.basicPublish("", routingKey, null, event.toByteArray)
      }
  }

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
