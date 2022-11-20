package com.github.fortega.types

import com.github.fortega.model.Validated
import com.github.fortega.model.gps.Event
import com.github.fortega.types.InvalidReasonInstances._
import com.github.fortega.types.InvalidReasonSyntax._
import org.scalatest.flatspec.AnyFlatSpec

class InvalidReasonTest extends AnyFlatSpec {
  "InvalidReason" should "run on valid gps events" in {
    List(
      Event(
        deviceId = 1,
        time = 0,
        longitude = 0,
        latitude = 0,
        altitude = 0,
        velocity = 0,
        angle = 0
      )
    )
      .flatMap { _.validated.invalidReason } match {
      case Nil             => succeed
      case _: List[String] => fail
    }
  }

  it should "run on invalid gps events" in {
    List(
      "invalid longitude. invalid latitude. invalid velocity. invalid angle" -> Event(
        deviceId = 1,
        time = 0,
        longitude = 1000,
        latitude = 1000,
        altitude = 0,
        velocity = 1000,
        angle = 1000
      ),
      "invalid longitude" -> Event(
        deviceId = 1,
        time = 0,
        longitude = 1000,
        latitude = 0,
        altitude = 0,
        velocity = 0,
        angle = 0
      )
    ).foreach { case (expected, event) =>
      event.invalidReason match {
        case None         => fail(s"Must be $expected -> $event")
        case Some(result) => assert(expected == result)
      }
    }
  }
}
