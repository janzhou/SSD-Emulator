package org.janzhou.test

import org.janzhou.cminer._
import scala.collection.mutable.ArrayBuffer

import util.Random

object CMinerTest {
  def main (args: Array[String]) {
    val miner = new LSHMiner(2, 4, 3)
    val seq = ArrayBuffer(
      0,1,3,
      0,1,3,
      0,1,3,
      0,1,3,
      0,1,3,
      2,6,7,
      4,1,3,
      0,1,3,
      0,1,3,
      2,4,5)
    miner.mine(seq).map(println)
  }
}
