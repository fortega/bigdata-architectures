package com.github.fortega.model

case class RawGps (
    truckId: Int,
    time: Long,
    longitude: Double,
    latitude: Double,
    altitude: Double,
    velocity: Int,
    angle: Int
)
