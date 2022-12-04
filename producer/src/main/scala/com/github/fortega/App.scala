package com.github.fortega

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import scala.util.Try
import com.github.fortega.model.gps.Event
import scala.util.Failure
import scala.util.Success

object App {
  def main(cmdArgs: Array[String]): Unit = config(sys.env) match {
    case Some(Array(host, queue, period)) if Try(period.toLong).isSuccess =>
      usingChannel(host) { channel =>
        channel.queueDeclare(queue, true, false, false, null)

        createTimer(period.toLong) { () =>
          val event = randomEvent
          channel.basicPublish("", queue, null, event.toByteArray)
        }

        loop
      } match {
        case Failure(error) => 
          error.printStackTrace()
        case Success(_) =>
      }
    case _ => sys.error("invalid arguments")
  }
  private def config(env: Map[String, String]) = for {
    host <- env.get("HOST")
    queue <- env.get("QUEUE")
    period <- env.get("PERIOD")
  } yield Array(host, queue, period)

  private def loop = while (true) { Thread.sleep(Long.MaxValue) }

  private def createTimer(period: Long)(f: () => Unit) = {
    val task = new java.util.TimerTask() {
      override def run = f()
    }
    val timer = new java.util.Timer(true)
    timer.scheduleAtFixedRate(task, 0, period)
  }

  private def randomEvent = Event(
    deviceId = (100 * math.random).toInt,
    time = System.currentTimeMillis,
    longitude = (360 * math.random) - 180,
    latitude = (180 * math.random) - 90,
    altitude = (100 * math.random),
    velocity = (200 * math.random).toInt,
    angle = (360 * math.random).toInt
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
