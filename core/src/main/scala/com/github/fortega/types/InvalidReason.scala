package com.github.fortega.types

import com.github.fortega.model.{EventGps, Validated}
import math.abs

sealed trait InvalidReason[A] {
  val validate: A => Option[String]
}

object InvalidReasonInstances {
  implicit val eventGps = new InvalidReason[EventGps] {
    private val separator = ". "
    private val validations = List[(String, EventGps => Boolean)](
      "invalid longitude" -> (d => abs(d.longitude) > 180),
      "invalid latitude" -> (d => abs(d.latitude) > 90),
      "invalid velocity" -> (d => d.velocity < 0 || d.velocity > 150),
      "invalid angle" -> (d => d.angle < 0 || d.angle >= 360)
    ).par

    override val validate: EventGps => Option[String] = value =>
      validations
        .flatMap({ case (message, check) =>
          if (check(value)) Some(message) else None
        }) match {
        case errors if errors.nonEmpty => Some(errors.mkString(separator))
        case _                         => None
      }
  }
}

object InvalidReasonSyntax {
  implicit class InvalidReasonOps[A](value: A)(implicit check: InvalidReason[A]) {
    def validate = Validated(value, check.validate(value))
    def invalidReason = check.validate(value)
  }
}
