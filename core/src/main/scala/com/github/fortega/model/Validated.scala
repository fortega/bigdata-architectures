package com.github.fortega.model

import com.github.fortega.types.InvalidReason

case class Validated[A](value: A, invalidReason: Option[String]) {
  lazy val isValid = invalidReason.isEmpty
}
