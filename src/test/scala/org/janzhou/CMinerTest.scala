package org.janzhou.test

import org.janzhou.cminer._
import scala.collection.mutable.ArrayBuffer

import util.Random

object CMinerTest {
  def main (args: Array[String]) {
    val miner = new LSHMiner(2, 4, 3)
    val seq = ArrayBuffer(
      1,2,3,4,
      1,2,4,5,
      1,128,129,136,
      5,6,2,8)
    miner.mine(seq).map(println)
  }
}
