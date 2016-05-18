package org.janzhou.test

import org.janzhou.cminer._

import util.Random

object CMinerTest {
  def main (args: Array[String]) {
    val miner = new LSHMiner(2, 3, 3)
    val seq = List(
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
