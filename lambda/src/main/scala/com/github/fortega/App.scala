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
      config: Config
  ): RabbitMQ.Event[Try[ValidatedEvent]] => String = _.value match {
    case Success(validated) =>
      validated.invalidReasion match {
        case None    => config.valid
        case Some(_) => config.invalid
      }
    case _ => config.deadLetter
  }

  implicit def rabbitToByte[A <: { def toByteArray: Array[Byte] }](
      value: RabbitMQ.Event[Try[A]]
  ): (BasicProperties, Array[Byte]) = value.value match {
    case Failure(_)         => (null, Array[Byte]())
    case Success(validated) => (null, validated.toByteArray)
  }

  def run = for {
    _ <- Console.printLine("starting")
    args <- getArgs
    config <- ZIO.fromEither(getConfig(args))
    channel = RabbitMQ.createChannel(config.host)
    _ = RabbitMQ.createQueues(
      channel,
      config.deadLetter,
      config.invalid,
      config.valid
    )
    stream = RabbitMQ.consumerStream(channel, config.input)
    _ <- stream
      .map(
        _.map(v =>
          Try(Event.parseFrom(v))
            .map(event => ValidatedEvent(event, event.invalidReason))
        )
      )
      .tapSink(RabbitMQ.forwardSink(channel, queueDiscovery(config)))
      .tapSink(printSink)
      .run(RabbitMQ.ackSink(channel))
  } yield ()
}
