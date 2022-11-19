package com.github.fortega.model

import com.github.fortega.types.InvalidReason

case class Validated[A](value: A)(implicit check: InvalidReason[A]) {
  lazy val invalidReason: Option[String] = check.validate(value)
  lazy val isValid = invalidReason.isEmpty
}
