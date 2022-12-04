package com.github.fortega

import com.github.fortega.model.gps.{Event, ValidatedEvent}
import com.github.fortega.types.InvalidReasonInstances._
import com.github.fortega.types.InvalidReasonSyntax._
import scala.util.{Try, Failure, Success}
import zio._
import com.rabbitmq.client.{ConnectionFactory, Consumer}
import com.rabbitmq.client.ShutdownSignalException
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.AMQP.BasicProperties
import zio.stream._

case class Config(
    host: String,
    input: String,
    deadLetter: String,
    valid: String,
    invalid: String
)
object App extends ZIOAppDefault {
  def getConfig(args: Chunk[String]): Either[String, Config] = args match {
    case Chunk(host, input, deadLetter, valid, invalid) =>
      Right(Config(host, input, deadLetter, valid, invalid))
    case _ => Left("invalid arguments")
  }
  def consumerStream(host: String, queue: String) =
    ZStream.async[Any, String, Array[Byte]] { callback =>
      lazy val channel = {
        Console.printLine("creating channel")
        val cf = new ConnectionFactory
        cf.setHost(host)
        cf.newConnection.createChannel
      }
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
          ): Unit = {
            val tag = envelope.getDeliveryTag
            callback(ZIO.succeed(Chunk(body)))
            channel.basicAck(tag, false)
          }
        }
      )
    }

  val sink = ZSink.foreach[Any, Any, Try[Event]](i => Console.printLine(i))
  def run = for {
    args <- getArgs
    config <- ZIO.fromEither(getConfig(args))
    stream = consumerStream(config.host, config.input)
    event = stream.map { e =>
      Try(Event.parseFrom(e)).map(_.validated) match {
        case Success(event) => event.toString
        case Failure(error) =>
          s"error(${error.getClass.getName}): ${error.getMessage}"
      }
    }
    _ <- event.foreach(Console.printLine(_))
  } yield ()
}
/* object App {
  private lazy val exchange = ""
  private lazy val stats = new Stats

  def main(cmdArgs: Array[String]): Unit = cmdArgs match {
    case Array(host, input, deadLetter, valid, invalid) =>
      implicit val mq = RabbitQueue(host, List(deadLetter, valid, invalid))
      mq.addConsumer(input)(createConsumer(deadLetter, valid, invalid))
      loop
    case _ => sys.error("Invalid arguments")
  }

  private def createConsumer[A](
      deadLetter: A,
      valid: A,
      invalid: A
  )(implicit
      mq: MessageQueue[A, RabbitMessage]
  ): RabbitMessage => Unit = { msg =>
    Try(
      Event.parseFrom(msg.body)
    ) match {
      case Failure(error) => mq.publish(deadLetter, msg)
      case Success(event) =>
        val (queue, newMsg) =
          event.invalidReason match {
            case None =>
              stats.addValid
              (valid, msg)
            case Some(invalidReason) =>
              stats.addInvalid
              val validated = ValidatedEvent(event, Some(invalidReason))
              (invalid, msg.copy(body = validated.toByteArray))
          }

        mq.publish(queue, newMsg)
        stats.printStats
    }
  }

  private def loop = while (true) Thread.sleep(Long.MaxValue)
} */
