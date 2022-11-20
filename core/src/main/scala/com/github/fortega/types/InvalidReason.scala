package com.github.fortega.types

import com.github.fortega.model.Validated
import com.github.fortega.model.gps.Event
import math.abs

sealed trait InvalidReason[A] {
  val validate: A => Option[String]
}

object InvalidReasonInstances {
  implicit val eventGps = new InvalidReason[Event] {
    private val separator = ". "
    private val validations = List[(String, Event => Boolean)](
      "invalid longitude" -> (d => abs(d.longitude) > 180),
      "invalid latitude" -> (d => abs(d.latitude) > 90),
      "invalid velocity" -> (d => d.velocity < 0 || d.velocity > 150),
      "invalid angle" -> (d => d.angle < 0 || d.angle >= 360)
    ).par

    override val validate: Event => Option[String] = value =>
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
    def invalidReason: Option[String] = check.validate(value)
    def validated: Validated[A] = Validated(value, invalidReason)
  }
}
