package org.janzhou.cminer

import scala.collection.mutable.ArrayBuffer
import collection.mutable.HashMap
import scala.util.control.Breaks

trait Miner {
  def mine(seq:ArrayBuffer[Int]):ArrayBuffer[ArrayBuffer[Int]]
}

class CMiner (
  val minSupport:Int = 2,
  val splitSize:Int = 512,
  val depth:Int = 64
) extends Miner {
  class CMinerSubsequence(
    val seq:ArrayBuffer[Int],
    val split:Array[Int],
    val pos:Int,
    val father_support:Int
  ) {
    var support = 0

    override def equals(o: Any) = o match {
      case that:CMinerSubsequence => this.seq.equals(that.seq)
      case _ => false
    }

    override def hashCode():Int = {
      this.seq.hashCode
    }
  }

  private def frequentSubsequence(list:ArrayBuffer[CMinerSubsequence], minSupport:Int)
  :Map[CMinerSubsequence, ArrayBuffer[CMinerSubsequence]] = {
    def filter(e:CMinerSubsequence, list:ArrayBuffer[CMinerSubsequence])
    :Boolean = {
      e.support = list.length
      if( e.father_support >= minSupport ) {
        e.support >= minSupport || e.support < (e.father_support / 2)
      } else {
        e.support >= minSupport
      }
    }

    val subSeq = list groupBy identity filter{ case (e, list) => filter(e, list) }

    subSeq.foreach{ case (e, list) => list.foreach(e => e.support = list.length)}

    //subSeq.foreach{ case (e, list) => println(e.seq) }

    subSeq
  }

  private def firstLevelSubSequences(splits:ArrayBuffer[ArrayBuffer[Int]])
  :ArrayBuffer[CMinerSubsequence] = {
    splits.flatMap( split => {
      split.zipWithIndex.map{ case (access, pos) => {
        new CMinerSubsequence(ArrayBuffer(access), split.toArray, pos, 0)
      }}
    })
  }

  private def nextLevelSubSequence(list:ArrayBuffer[CMinerSubsequence])
  :ArrayBuffer[CMinerSubsequence] = {
    list.flatMap( father => {
      for ( pos <- father.pos + 1 to father.split.length - 1 ) yield {
        val seq = father.seq :+ father.split(pos)
        new CMinerSubsequence(seq, father.split, pos, father.support)
      }
    })
  }

  private def miningNext(list:ArrayBuffer[CMinerSubsequence], minSupport:Int, depth:Int)
  :Map[CMinerSubsequence, ArrayBuffer[CMinerSubsequence]] = {
    val subSeq = if( depth == this.depth ) {
      frequentSubsequence(
        list,
        minSupport
      )
    } else {
      frequentSubsequence(
        nextLevelSubSequence(list),
        minSupport
      )
    }

    if ( depth == 1 || subSeq.size == 0) {
      subSeq
    } else {
      subSeq.map{ case (e, seq) => {miningNext(seq, minSupport, depth - 1)} }
      .fold(Map[CMinerSubsequence, ArrayBuffer[CMinerSubsequence]]())( _ ++ _)
    }
  }

  protected def mineSplits(splits:ArrayBuffer[ArrayBuffer[Int]])
  :ArrayBuffer[ArrayBuffer[Int]] = {
    miningNext(
      firstLevelSubSequences(splits),
      minSupport,
      depth
    ).keys.to[ArrayBuffer].map(_.seq)
  }

  def mine(seq:ArrayBuffer[Int]):ArrayBuffer[ArrayBuffer[Int]] = {
    mineSplits(seq.grouped(splitSize).to[ArrayBuffer])
  }

  assert(splitSize >= depth)

}
