package com.github.fortega.types

import com.github.fortega.model.{EventGps, Validated}
import scala.math.abs

sealed trait ErrorCheck[A] {
  def apply(value: A): Validated[A]
}

object GpsErrorCheck extends ErrorCheck[EventGps] {
  lazy private val sep = ". "
  private val validations = List[(String, EventGps => Boolean)](
    "invalid longitude" -> (d => abs(d.longitude) > 180),
    "invalid latitude" -> (d => abs(d.latitude) > 90),
    "invalid velocity" -> (d => d.velocity < 0 || d.velocity > 150),
    "invalid angle" -> (d => d.angle < 0 || d.angle >= 360)
  )

  override def apply(value: EventGps): Validated[EventGps] = {
    val invalidReason = validations.flatMap { case (message, check) =>
      if (check(value)) Some(message) else None
    } match {
      case Nil                  => None
      case errors: List[String] => Some(errors.mkString(sep))
    }
    Validated(value, invalidReason)
  }
}
