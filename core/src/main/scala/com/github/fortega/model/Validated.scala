package com.github.fortega.model

case class Validated[A](
    value: A,
    invalidReason: Option[String]
) {
  lazy val isValid = invalidReason.isEmpty
}
