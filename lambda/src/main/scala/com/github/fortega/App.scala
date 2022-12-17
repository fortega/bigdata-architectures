package com.github.fortega

import com.github.fortega.model.gps.{Event, ValidatedEvent}
import com.github.fortega.types.InvalidReasonInstances._
import com.github.fortega.types.InvalidReasonSyntax._
import scala.util.{Try, Failure, Success}
import zio._
import zio.stream._
import com.rabbitmq.client.AMQP.BasicProperties

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

  def printSink[A] = ZSink.foreach[Any, Any, A](Console.printLine(_))

  def queueDiscovery(
      deadLetter: String,
      invalid: String,
      valid: String
  ): ((Long, BasicProperties, Try[ValidatedEvent])) => String = {
    case (_, _, data) =>
      data match {
        case Success(validated) =>
          validated.invalidReason match {
            case None    => valid
            case Some(_) => invalid
          }
        case _ => deadLetter
      }
  }

  implicit def rabbitToByte[A <: { def toByteArray: Array[Byte] }](
      value: RabbitMQ.Event[Try[A]]
  ): (BasicProperties, Array[Byte]) = value.value match {
    case Failure(_)         => (null, Array[Byte]())
    case Success(validated) => (null, validated.toByteArray)
  }

  def app(config: Config) = for {
    stream <- RabbitMQ.consumerStream(config.input)
    forward <- {
      val discovery =
        queueDiscovery(config.deadLetter, config.invalid, config.valid)
      implicit val convert: ((Long, BasicProperties, Try[ValidatedEvent])) => (
          BasicProperties,
          Array[Byte]
      ) = { case (_, props, data) =>
        (props, data.get.toByteArray)
      }
      RabbitMQ.forwardSink(discovery)
    }
    ackSink <- RabbitMQ.ackSink
    result <- stream
      .map { case (tag, props, data) =>
        val validatedEvent = Try(Event.parseFrom(data))
          .map(event => ValidatedEvent(event, event.invalidReason))
        (tag, props, validatedEvent)
      }
      .tapSink(forward)
      .tapSink(printSink)
      .map { case (tag, _, _) => tag }
      .run(ackSink)
  } yield result

  def run = for {
    args <- getArgs
    config <- ZIO.fromEither(getConfig(args))
    result <- app(config).provide {
      ZLayer(ZIO.fromTry { RabbitMQ.createChannel(config.host) })
    }
  } yield result
}
