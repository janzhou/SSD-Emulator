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
    val next:Int,
    val split:ArrayBuffer[Int],
    val pos:Int,
    val father_support:Int
  ) {
    var support = 0

    override def equals(o: Any) = o match {
      case that:CMinerSubsequence => this.seq.equals(that.seq) && this.next.equals(that.next)
      case _ => false
    }

    override def hashCode():Int = {
      (this.seq, this.next).hashCode
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

    //subSeq.keys.foreach( x => println(x.seq) )

    subSeq
  }

  protected def firstLevelSubSequences(
    input:ArrayBuffer[Int],
    minSupport:Int,
    splitSize:Int
  ):ArrayBuffer[CMinerSubsequence] = {
    val count = input.groupBy(identity).mapValues(_.size)
    val filtered = input.filter( count(_) >= minSupport )
    val ret = filtered.grouped(splitSize).flatMap( split => {
      split.zipWithIndex.flatMap{ case (access, pos) => {
        for ( p <- pos + 1 to split.length - 1 ) yield {
          new CMinerSubsequence(ArrayBuffer(access), split(p), split, p, count(access))
        }
      }}
    }).to[ArrayBuffer]
    //ret.map(x => println(x.seq))
    ret
  }

  private def nextLevelSubSequence(list:ArrayBuffer[CMinerSubsequence])
  :ArrayBuffer[CMinerSubsequence] = {
    val seq = list.head.seq :+ list.head.next
    list.flatMap( father => {
      for ( pos <- father.pos + 1 to father.split.length - 1 ) yield {
        new CMinerSubsequence(seq, father.split(pos), father.split, pos, father.support)
      }
    })
  }

  private def miningNext(list:ArrayBuffer[CMinerSubsequence], minSupport:Int, depth:Int)
  :Iterable[CMinerSubsequence] = {
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
      subSeq.keys
    } else {
      subSeq.map{ case (e, seq) => {miningNext(seq, minSupport, depth - 1)} }
      .fold(Iterable[CMinerSubsequence]())( _ ++ _)
    }
  }

  protected def mine(input:ArrayBuffer[Int], minSupport:Int, splitSize:Int)
  :ArrayBuffer[ArrayBuffer[Int]] = {
    miningNext(
      firstLevelSubSequences(input, minSupport, splitSize),
      minSupport,
      depth - 2
    ).map(x => x.seq :+ x.next).to[ArrayBuffer]
  }

  def mine(seq:ArrayBuffer[Int]):ArrayBuffer[ArrayBuffer[Int]] = {
    mine(seq, minSupport, splitSize)
  }

  assert(splitSize >= depth)

}
