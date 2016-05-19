package org.janzhou.cminer

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import info.debatty.java.lsh.LSHMinHash

class LSHMiner (
  override val minSupport:Int = 2,
  override val splitSize:Int = 512,
  override val depth:Int = 64
) extends CMiner (minSupport, splitSize, depth) {
  private def lshGroup(splits:ArrayBuffer[ArrayBuffer[Int]], minSupport:Int, splitSize:Int)
  :ArrayBuffer[ArrayBuffer[ArrayBuffer[Int]]] = {
    val buckets:Int = splits.length
    val stages:Int = splitSize
    val lsh = new LSHMinHash(stages, buckets, splitSize)
    val the_buckets = splits.map( seq =>
      (lsh.hash(setAsJavaSet(seq.map( i => i:java.lang.Integer ).toSet))(stages - 1), seq)
    ).groupBy( _._1 ).filter(_._2.length >= minSupport)
    .map(_._2.map(_._2)).to[ArrayBuffer]
    the_buckets
  }

  override def mine(seq:ArrayBuffer[Int]):ArrayBuffer[ArrayBuffer[Int]] = {
    val groups = seq.grouped(splitSize).filter(_.length == splitSize).to[ArrayBuffer]

    val buckets = lshGroup(groups, minSupport, splitSize)
    //buckets.foreach( println )
    var index = 1
    val freqInBucket = buckets.map( splits => {
      println(splits)
      println("lsh bucket " + index + "/" + buckets.length + " size "
        + splits.length + "/" + groups.length)
      index += 1
      val input = splits.fold(ArrayBuffer[Int]())( _ ++= _ )
      val output = mine(input, minSupport, splitSize)
      println("correlation mined " + output.length)
      output
    })

    freqInBucket.fold(ArrayBuffer[ArrayBuffer[Int]]())( _ ++= _ ).distinct
  }
}
