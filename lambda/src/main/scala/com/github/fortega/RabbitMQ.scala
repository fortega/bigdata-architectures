package com.github.fortega

import com.rabbitmq.client.{
  Channel,
  ConnectionFactory,
  Consumer,
  Envelope,
  ShutdownSignalException
}
import com.rabbitmq.client.AMQP.BasicProperties
import zio.{Chunk, Console, ZIO}
import zio.stream.{ZSink, ZStream}

object RabbitMQ {
  case class Event[A](
      tag: Long,
      value: A
  ) {
    def map[B](f: A => B): Event[B] = this.copy(value = f(value))
  }

  def createQueues(channel: Channel, queues: String*): Unit =
    queues.foreach(channel.queueDeclare(_, true, false, false, null))

  def createChannel(host: String): Channel = {
    val cf = new ConnectionFactory
    cf.setHost(host)
    cf.newConnection.createChannel
  }

  def ackSink[A](channel: Channel) =
    ZSink.foreach[Any, Throwable, Event[A]] { event =>
      ZIO.succeed(channel.basicAck(event.tag, false))
    }

  def consumerStream(channel: Channel, queue: String) =
    ZStream.async[Any, String, Event[Array[Byte]]] { callback =>
      channel.basicConsume(
        queue,
        new Consumer() {
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
            ZIO.succeed(Chunk(Event(envelope.getDeliveryTag, body)))
          )
        }
      )
    }

  def forwardSink[A](
      channel: Channel,
      queue: A => String,
      exchange: String = ""
  )(implicit
      convert: A => (BasicProperties, Array[Byte])
  ) = ZSink.foreach[Any, Any, A] { value =>
    val (props, body) = convert(value)
    ZIO.succeed(channel.basicPublish(exchange, queue(value), props, body))
  }
}
