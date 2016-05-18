package org.janzhou.cminer

import scala.collection.mutable.ArrayBuffer
import info.debatty.java.lsh.LSHSuperBit

class LSHMiner (
  override val minSupport:Int = 2,
  override val splitSize:Int = 512,
  override val depth:Int = 64,
  val buckets:Int = 128,
  val stages:Int = 8
) extends CMiner (minSupport, splitSize, depth) {
  private val lsh = new LSHSuperBit(stages, buckets, splitSize)

  private def lshGroup(splits:ArrayBuffer[ArrayBuffer[Int]], minSupport:Int)
  :ArrayBuffer[ArrayBuffer[ArrayBuffer[Int]]] = {
    splits.map( seq =>
      (lsh.hash(seq.toArray)(stages - 1), seq)
    ).groupBy( _._1 ).filter(_._2.length >= minSupport)
    .map(_._2.map(_._2)).to[ArrayBuffer]
  }

  override def mine(seq:ArrayBuffer[Int]):ArrayBuffer[ArrayBuffer[Int]] = {
    val groups = seq.grouped(splitSize).to[ArrayBuffer]

    val buckets = lshGroup(groups, minSupport)
    var index = 1
    val freqInBucket = buckets.map( splits => {
      println("lsh bucket " + index + "/" + buckets.length + " size "
        + splits.length + "/" + groups.length)
      index += 1
      val input = splits.fold(ArrayBuffer[Int]())( _ ++= _ )
      mine(input, minSupport, splitSize)
    })

    freqInBucket.fold(ArrayBuffer[ArrayBuffer[Int]]())( _ ++= _ ).distinct
  }
}
