package com.github.fortega.transformation

import org.scalatest.flatspec.AnyFlatSpec
import com.github.fortega.model.RawGps

class ValidateGpsTest extends AnyFlatSpec {
  "ValidateGps" should "run on valid events" in {
    List(
      RawGps(
        truckId = 1,
        time = 0,
        longitude = 0,
        latitude = 0,
        altitude = 0,
        velocity = 0,
        angle = 0
      )
    ).flatMap { event => ValidateGps(event).invalidReason } match {
      case Nil             => succeed
      case _: List[String] => fail
    }
  }

  it should "run on invalid events" in {
    List(
      "invalid longitude. invalid latitude. invalid velocity. invalid angle" -> RawGps(
        truckId = 1,
        time = 0,
        longitude = 1000,
        latitude = 1000,
        altitude = 0,
        velocity = 1000,
        angle = 1000
      ),
      "invalid longitude" -> RawGps(
        truckId = 1,
        time = 0,
        longitude = 1000,
        latitude = 0,
        altitude = 0,
        velocity = 0,
        angle = 0
      )
    ).foreach { case (expected, event) =>
      ValidateGps(event).invalidReason match {
        case None         => fail(s"Must be $expected -> $event")
        case Some(result) => assert(expected == result)
      }
    }
  }
}
