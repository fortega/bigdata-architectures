package com.github.fortega

import com.rabbitmq.client.{
  Channel,
  ConnectionFactory,
  Consumer,
  Envelope,
  ShutdownSignalException
}
import com.rabbitmq.client.AMQP.BasicProperties
import scala.util.Try
import zio.{Chunk, Console, ZIO}
import zio.stream.{ZSink, ZStream}

object RabbitMQ {
  case class Event[A](
      tag: Long,
      value: A
  ) {
    def map[B](f: A => B): Event[B] = this.copy(value = f(value))
  }

  def createQueues(queues: String*)(implicit channel: Channel): Unit =
    queues.foreach(channel.queueDeclare(_, true, false, false, null))

  def createChannel(host: String) = Try {
    val cf = new ConnectionFactory
    cf.setHost(host)
    cf.newConnection
  }
    .map(_.createChannel)

  def ackSink[A] = ZIO.service[Channel].flatMap { channel =>
    ZIO.succeed(ZSink.foreach[Channel, Throwable, Long] { tag =>
      ZIO.succeed(channel.basicAck(tag, false))
    })
  }

  def consumerStream(queue: String) = ZIO.service[Channel].flatMap { channel =>
    ZIO.succeed(
      ZStream.asyncZIO[Channel, String, (Long, BasicProperties, Array[Byte])] {
        callback =>
          val consumer = new Consumer() {
            override def handleConsumeOk(consumerTag: String): Unit =
              ZIO.log(s"consume($consumerTag")

            override def handleCancelOk(consumerTag: String): Unit =
              callback(ZIO.fail(Some(s"cancelOk($consumerTag")))

            override def handleCancel(consumerTag: String): Unit =
              callback(ZIO.fail(Some(s"cancel($consumerTag)")))

            override def handleShutdownSignal(
                consumerTag: String,
                sig: ShutdownSignalException
            ): Unit =
              callback(
                ZIO.fail(Some(s"shutdown($consumerTag): ${sig.getMessage}"))
              )

            override def handleRecoverOk(consumerTag: String): Unit =
              ZIO.log(s"recover($consumerTag")

            override def handleDelivery(
                consumerTag: String,
                envelope: Envelope,
                properties: BasicProperties,
                body: Array[Byte]
            ): Unit = callback(
              ZIO.succeed(Chunk((envelope.getDeliveryTag, properties, body)))
            )
          }
          ZIO.succeed(channel.basicConsume(queue, consumer))
      }
    )
  }

  def forwardSink[A](queue: A => String, exchange: String = "")(implicit
      convert: A => (BasicProperties, Array[Byte])
  ) =
    ZIO.service[Channel].flatMap { channel =>
      val sink = ZSink.foreach[Channel, Throwable, A] { value =>
        val (props, body) = convert(value)
        ZIO.succeed(channel.basicPublish(exchange, queue(value), props, body))
      }
      ZIO.succeed(sink)
    }
}
