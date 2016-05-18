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
  :ArrayBuffer[CMinerSubsequence] = {
    val support = list groupBy identity mapValues (_.length)

    list.foreach { element =>
      element.support = support(element)
    }

    def filter(e:CMinerSubsequence):Boolean = {
      if( e.father_support >= minSupport ) {
        e.support >= minSupport || e.support < (e.father_support / 2)
      } else {
        e.support >= minSupport
      }
    }

    list.filter( filter(_) )
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

  protected def mineSplits(splits:ArrayBuffer[ArrayBuffer[Int]])
  :ArrayBuffer[ArrayBuffer[Int]] = {
    var subSequence = frequentSubsequence(
      firstLevelSubSequences(splits),
      minSupport
    )

    for ( i <- 1 to depth - 1 ) {
      subSequence = frequentSubsequence(
        nextLevelSubSequence(subSequence),
        minSupport
      )
    }

    subSequence.map( _.seq ).distinct
  }

  def mine(seq:ArrayBuffer[Int]):ArrayBuffer[ArrayBuffer[Int]] = {
    mineSplits(seq.grouped(splitSize).to[ArrayBuffer])
  }

  assert(splitSize >= depth)

}
