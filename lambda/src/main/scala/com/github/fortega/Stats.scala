package com.github.fortega

import scala.concurrent.Lock

class Stats {
  private var invalid = 0L
  private var valid = 0L
  private val lock = new Lock

  def printStats = usingLock { println(s"invalid/valid -> $invalid/$valid") }
  def addInvalid = usingLock { invalid += 1 }
  def addValid = usingLock { valid += 1 }

  private def usingLock(f: => Unit): Unit = {
    lock.acquire
    f
    lock.release
  }
}
