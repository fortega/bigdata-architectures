package com.github.fortega.transformation

import scala.math.abs
import com.github.fortega.model.RawGps
import com.github.fortega.model.ValidatedGps

/** Execute validation to [[com.github.fortega.model.RawGps]] */
object ValidateGps {

  /** Validate gps event using validations
    *
    * @param raw
    * @return
    */
  def apply(raw: RawGps): ValidatedGps =
    ValidatedGps(
      data = raw,
      invalidReason = validations
        .map(_(raw))
        .reduce(adder)
    )

  /** Check for error
    *
    * @param message
    *   error message
    * @param check
    *   return true when error is found
    */
  case class ErrorCheck(message: String, check: RawGps => Boolean) {
    def apply(raw: RawGps): Option[String] =
      if (check(raw)) Some(message) else None
  }

  val validations: List[ErrorCheck] = List(
    ErrorCheck("invalid longitude", d => abs(d.longitude) > 180),
    ErrorCheck("invalid latitude", d => abs(d.latitude) > 90),
    ErrorCheck("invalid velocity", d => d.velocity < 0 || d.velocity > 150),
    ErrorCheck("invalid angle", d => d.angle < 0 || d.angle >= 360)
  )

  private lazy val sep = ". "
  private def adder(
      a: Option[String],
      b: Option[String]
  ): Option[String] =
    List(a, b).flatten match {
      case Nil             => None
      case c: List[String] => Some(c.mkString(sep))
    }

}
