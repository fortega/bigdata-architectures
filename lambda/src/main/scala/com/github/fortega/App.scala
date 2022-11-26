package com.github.fortega

import scala.util.{Try, Failure, Success}
import com.github.fortega.model.gps.{Event, ValidatedEvent}
import com.github.fortega.types.InvalidReasonInstances._
import com.github.fortega.types.InvalidReasonSyntax._
import com.rabbitmq.client.{
  ConnectionFactory,
  Channel,
  Consumer,
  ShutdownSignalException,
  Envelope
}
import com.rabbitmq.client.AMQP.BasicProperties
import org.slf4j.LoggerFactory

object App {
  private lazy val log = LoggerFactory.getLogger(this.getClass)
  private lazy val exchange = ""
  def main(cmdArgs: Array[String]): Unit = cmdArgs match {
    case Array(host, inputQueue, deadLetterQueue, validQueue, invalidQueue) =>
      usingChannel(host) { channel =>
        log.info("declaring queues")
        Seq(deadLetterQueue, validQueue, invalidQueue).foreach { queue =>
          channel.queueDeclare(queue, true, false, false, null)
          log.info(s"$queue declared")
        }

        log.info("starting consumer")
        channel.basicConsume(
          inputQueue,
          consumer(channel, deadLetterQueue) { case (properties, body) =>
            val event = Event.parseFrom(body)
            val (output: String, newBody: Array[Byte]) =
              event.invalidReason match {
                case None => (validQueue, body)
                case Some(invalidReason) =>
                  (
                    invalidQueue,
                    ValidatedEvent(event, Some(invalidReason)).toByteArray
                  )
              }
            channel.basicPublish(exchange, output, properties, newBody)
          }
        )

        log.info("infinite loop")
        loop
      }

    case _ => sys.error("Invalid arguments")
  }

  private def consumer(
      channel: Channel,
      deadLetterQueue: String
  )(
      f: (BasicProperties, Array[Byte]) => Unit
  ): Consumer = new Consumer() {
    private lazy val log = LoggerFactory.getLogger(this.getClass)

    override def handleConsumeOk(consumerTag: String): Unit = {
      log.info(s"starting in $consumerTag")
    }

    override def handleCancelOk(consumerTag: String): Unit = handleCancel(
      consumerTag
    )

    override def handleCancel(consumerTag: String): Unit = {
      log.info(s"cancel $consumerTag. exiting")
      sys.exit(0)
    }

    override def handleShutdownSignal(
        consumerTag: String,
        sig: ShutdownSignalException
    ): Unit = {
      println(s"shutdown signal($consumerTag): ${sig.getMessage}")
      sig.printStackTrace
      sys.exit(1)
    }

    override def handleRecoverOk(consumerTag: String): Unit = {
      log.info(s"recovery($consumerTag)")
    }

    override def handleDelivery(
        consumerTag: String,
        envelope: Envelope,
        properties: BasicProperties,
        body: Array[Byte]
    ): Unit = {
      val tag = envelope.getDeliveryTag
      Try(f(properties, body)) match {
        case Failure(error) =>
          log.error(s"${error.getClass.getName} ${error.getMessage}", error)
          channel.basicPublish(exchange, deadLetterQueue, properties, body)
          log.debug(s"dead letter: $tag")
        case Success(_) =>
          log.debug(s"success: $tag")
      }
      channel.basicAck(tag, false)
      log.debug(s"ack $tag")
    }

  }

  private def loop = while (true) Thread.sleep(Long.MaxValue)

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
