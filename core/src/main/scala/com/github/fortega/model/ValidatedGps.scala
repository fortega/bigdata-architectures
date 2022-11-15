package com.github.fortega.model

case class ValidatedGps (
    data: RawGps,
    invalidReason: Option[String]
) {
    lazy val isValid = invalidReason.isEmpty
}
