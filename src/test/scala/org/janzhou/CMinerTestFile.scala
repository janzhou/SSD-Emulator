package org.janzhou.test

import org.janzhou.cminer._
import scala.collection.mutable.ArrayBuffer

import java.io._

object CMinerTestFile {
  def main (args: Array[String]) {
    val oos = new ObjectInputStream(new FileInputStream(args(0)))
    val seq = oos.readObject().asInstanceOf[ArrayBuffer[Int]]
    oos.close

    val miner = new LSHMiner(2, 64, 8)
    miner.mine(seq).map(println)
  }
}
