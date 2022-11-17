package com.github.fortega.transformation

import org.scalatest.flatspec.AnyFlatSpec
import com.github.fortega.model.{EventGps, Validated}
import com.github.fortega.types.GpsErrorCheck

class ErrorCheckTest extends AnyFlatSpec {
  "ErrorCheck" should "run on valid gps events" in {
    List(
      EventGps(
        deviceId = 1,
        time = 0,
        longitude = 0,
        latitude = 0,
        altitude = 0,
        velocity = 0,
        angle = 0
      )
    ).flatMap { event => GpsErrorCheck(event).invalidReason } match {
      case Nil             => succeed
      case _: List[String] => fail
    }
  }

  it should "run on invalid gps events" in {
    List(
      "invalid longitude. invalid latitude. invalid velocity. invalid angle" -> EventGps(
        deviceId = 1,
        time = 0,
        longitude = 1000,
        latitude = 1000,
        altitude = 0,
        velocity = 1000,
        angle = 1000
      ),
      "invalid longitude" -> EventGps(
        deviceId = 1,
        time = 0,
        longitude = 1000,
        latitude = 0,
        altitude = 0,
        velocity = 0,
        angle = 0
      )
    ).foreach { case (expected, event) =>
      GpsErrorCheck(event).invalidReason match {
        case None         => fail(s"Must be $expected -> $event")
        case Some(result) => assert(expected == result)
      }
    }
  }
}
