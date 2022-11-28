package com.github.fortega

import com.rabbitmq.client.{
  ConnectionFactory,
  Envelope,
  ShutdownSignalException
}
import com.rabbitmq.client.AMQP.BasicProperties
import org.slf4j.LoggerFactory

sealed trait MessageQueue[A, B] {
  def publish(queue: A, event: B): Unit
  def addConsumer(queue: A)(f: B => Unit): Unit
}

case class RabbitMessage(props: BasicProperties, body: Array[Byte])

case class RabbitQueue(
    host: String,
    queues: List[String] = List(),
    exchange: String = ""
) extends MessageQueue[String, RabbitMessage] {
  queues.foreach(channel.queueDeclare(_, true, false, false, null))

  private lazy val props = new BasicProperties
  private lazy val channel = {
    val cf = new ConnectionFactory
    cf.setHost(host)
    cf.newConnection.createChannel
  }

  override def publish(
      queue: String,
      event: RabbitMessage
  ): Unit = channel.basicPublish(exchange, queue, event.props, event.body)

  override def addConsumer(queue: String)(
      f: RabbitMessage => Unit
  ): Unit = {
    channel.basicConsume(
      queue,
      new com.rabbitmq.client.Consumer {
        private val l = LoggerFactory.getLogger(this.getClass)

        override def handleConsumeOk(consumerTag: String): Unit =
          log(s"ConsumeOk: $consumerTag")

        override def handleCancelOk(consumerTag: String): Unit =
          log(s"CancelOk: $consumerTag", Some(0))

        override def handleCancel(consumerTag: String): Unit =
          log(s"Cancel: $consumerTag", Some(1))

        override def handleShutdownSignal(
            consumerTag: String,
            sig: ShutdownSignalException
        ): Unit = log(s"Shutdown: $consumerTag -> ${sig.getMessage}", Some(1))

        override def handleRecoverOk(consumerTag: String): Unit =
          log(s"Recover: $consumerTag")

        override def handleDelivery(
            consumerTag: String,
            envelope: Envelope,
            properties: BasicProperties,
            body: Array[Byte]
        ): Unit = {
          val tag = envelope.getDeliveryTag
          f(RabbitMessage(properties, body))
          channel.basicAck(tag, false)
        }

        private def log(message: String, exitCode: Option[Int] = None) =
          exitCode match {
            case None => l.info(message)
            case Some(status) =>
              l.error(message)
              sys.exit(status)
          }
      }
    )
  }
}
