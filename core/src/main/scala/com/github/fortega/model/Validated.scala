package com.github.fortega.model

import com.github.fortega.types.InvalidReason

case class Validated[A](value: A, invalidReason: Option[String]) {
  lazy val isValid = invalidReason.isEmpty
}

object Validated {
  def apply[A](value: A)(implicit check: InvalidReason[A]): Validated[A] = apply(value, check.validate(value))
}
