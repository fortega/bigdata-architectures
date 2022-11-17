package com.github.fortega.model

case class EventGps (
    deviceId: Int,
    time: Long,
    longitude: Double,
    latitude: Double,
    altitude: Double,
    velocity: Int,
    angle: Int
)
