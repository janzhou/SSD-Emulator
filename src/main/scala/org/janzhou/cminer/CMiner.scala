package org.janzhou.cminer

import collection.mutable.HashMap
import scala.util.control.Breaks

trait Miner {
  def mine(seq:List[Int]):List[List[Int]]
}

class CMiner (
  val minSupport:Int = 2,
  val splitSize:Int = 512,
  val depth:Int = 64
) extends Miner {
  class CMinerSubsequence(
    val seq:List[Int],
    val split:Array[Int],
    val pos:Int,
    val father:CMinerSubsequence
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

  private def frequentSubsequence(list:List[CMinerSubsequence], minSupport:Int)
  :List[CMinerSubsequence] = {
    val support = list groupBy identity mapValues (_.length)

    list.foreach { element =>
      element.support = support(element)
    }

    list.filter( _.support >= minSupport )
  }

  private def firstLevelSubSequences(splits:List[List[Int]])
  :List[CMinerSubsequence] = {
    splits.flatMap( split => {
      split.zipWithIndex.map{ case (access, pos) => {
        new CMinerSubsequence(List(access), split.toArray, pos, null)
      }}
    })
  }

  private def nextLevelSubSequence(list:List[CMinerSubsequence])
  :List[CMinerSubsequence] = {
    list.flatMap( father => {
      for ( pos <- father.pos + 1 to father.split.length - 1 ) yield {
        val seq = father.seq :+ father.split(pos)
        new CMinerSubsequence(seq, father.split, pos, father)
      }
    })
  }

  protected def mineSplits(splits:List[List[Int]])
  :List[List[Int]] = {
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

  def mine(seq:List[Int]):List[List[Int]] = {
    mineSplits(seq.grouped(splitSize).toList)
  }

  assert(splitSize >= depth)

}
