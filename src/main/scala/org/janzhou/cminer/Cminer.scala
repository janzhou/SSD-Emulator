package org.janzhou.cminer

import collection.mutable.HashMap
import scala.util.control.Breaks

class Cminer(
  val minSupport:Double = 0.1,
  val splitSize:Int = 512,
  val depth:Int = 64
) {
  class CminerSubsequence(
    val seq:List[Int],
    val split:Array[Int],
    val pos:Int,
    val father:CminerSubsequence
  ) {
    var support = 0

    override def equals(o: Any) = o match {
      case that:CminerSubsequence => this.seq.equals(that.seq)
      case _ => false
    }

    override def hashCode():Int = {
      this.seq.hashCode
    }
  }

  def frequentSubsequence(list:List[CminerSubsequence]):List[CminerSubsequence] = {
    var support = new HashMap[CminerSubsequence, Int]()
    for ( element <- list ) {
      val count = {
        if ( support.contains(element) ) {
          val count = support(element)
          support -= element
          count + 1
        } else {
          1
        }
      }
      support += ( element -> count )
    }

    list.foreach { element =>
      element.support = support(element)
    }

    val min = list.length * minSupport
    list.filter( _.support >= min )
  }

  def firstLevelSubSequences(seq:List[Int]):List[CminerSubsequence] = {
    val splits = seq.grouped(splitSize).toList

    splits.flatMap( split => {
      var pos = -1
      split.map( access => {
        pos += 1
        new CminerSubsequence(List(access), split.toArray, pos, null)
      })
    })
  }

  def nextLevelSubSequence(list:List[CminerSubsequence])
  :List[CminerSubsequence] = {
    list.flatMap( father => {
      for ( pos <- father.pos + 1 to father.split.length ) yield {
        val seq = father.seq :+ father.split(pos)
        new CminerSubsequence(seq, father.split, pos, father)
      }
    })
  }

  def mine(seq:List[Int]):List[List[Int]] = {
    var subSequence = frequentSubsequence(firstLevelSubSequences(seq))

    for ( i <- 1 to depth ) {
      subSequence = frequentSubsequence(nextLevelSubSequence(subSequence))
    }

    subSequence.map( _.seq )
  }

  assert(splitSize >= depth)

}
