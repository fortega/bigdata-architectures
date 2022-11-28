package com.github.fortega

import com.github.fortega.model.gps.{Event, ValidatedEvent}
import com.github.fortega.types.InvalidReasonInstances._
import com.github.fortega.types.InvalidReasonSyntax._
import scala.util.{Try, Failure, Success}

object App {
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
}
