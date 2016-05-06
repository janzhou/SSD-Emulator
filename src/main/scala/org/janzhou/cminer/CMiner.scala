package org.janzhou.cminer

import collection.mutable.HashMap
import scala.util.control.Breaks

class CMiner(
  val minSupport:Double = 0.1,
  val splitSize:Int = 512,
  val depth:Int = 64
) {
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

  private def frequentSubsequence(list:List[CMinerSubsequence])
  :List[CMinerSubsequence] = {
    var support = new HashMap[CMinerSubsequence, Int]()
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

    val min = list.length * minSupport
    list.foreach { element =>
      element.support = support(element)
    }
    list.filter( _.support >= min )
  }

  private def firstLevelSubSequences(seq:List[Int])
  :List[CMinerSubsequence] = {
    val splits = seq.grouped(splitSize).toList

    splits.flatMap( split => {
      var pos = -1
      split.map( access => {
        pos += 1
        new CMinerSubsequence(List(access), split.toArray, pos, null)
      })
    })
  }

  private def nextLevelSubSequence(list:List[CMinerSubsequence])
  :List[CMinerSubsequence] = {
    list.flatMap( father => {
      for ( pos <- father.pos + 1 to father.split.length ) yield {
        val seq = father.seq :+ father.split(pos)
        new CMinerSubsequence(seq, father.split, pos, father)
      }
    })
  }

  def mine(seq:List[Int]):List[List[Int]] = {
    var subSequence = frequentSubsequence(firstLevelSubSequences(seq))

    for ( i <- 1 to depth ) {
      subSequence = frequentSubsequence(nextLevelSubSequence(subSequence))
    }

    subSequence.map( _.seq ).distinct
  }

  assert(splitSize >= depth)

}
