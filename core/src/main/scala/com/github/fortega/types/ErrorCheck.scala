package com.github.fortega.types

import com.github.fortega.model.{EventGps, Validated}
import scala.math.abs

sealed trait ErrorCheck[A] {
  def apply(value: A): Validated[A]
}

object ErrorCheck {
  implicit class ErrorCheckOps[A](value: A) {
    def validate(implicit check: ErrorCheck[A]): Validated[A] = check(value)
  }

  implicit val eventGps = new ErrorCheck[EventGps] {
    private val separator = ". "
    private val validations = List[(String, EventGps => Boolean)](
      "invalid longitude" -> (d => abs(d.longitude) > 180),
      "invalid latitude" -> (d => abs(d.latitude) > 90),
      "invalid velocity" -> (d => d.velocity < 0 || d.velocity > 150),
      "invalid angle" -> (d => d.angle < 0 || d.angle >= 360)
    ).par

    override def apply(
        value: EventGps
    ): Validated[EventGps] = Validated(
      value = value,
      invalidReason = validations
        .flatMap({ case (message, check) =>
          if (check(value)) Some(message) else None
        }) match {
        case errors if errors.nonEmpty => Some(errors.mkString(separator))
        case _                         => None
      }
    )
  }
}
