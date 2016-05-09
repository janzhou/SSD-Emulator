package org.janzhou.cminer

import info.debatty.java.lsh.LSHSuperBit

class LSHMiner (
  override val minSupport:Double = 0.1,
  override val splitSize:Int = 512,
  override val depth:Int = 64,
  val buckets:Int = 128,
  val stages:Int = 8
) extends CMiner (minSupport, splitSize, depth) {
  private val lsh = new LSHSuperBit(stages, buckets, splitSize)

  override def mine(seq:List[Int]):List[List[Int]] = {
    val groups = seq.grouped(splitSize).toList

    val lsh_buckets = groups.map( seq =>
      (lsh.hash(seq.toArray)(stages - 1), seq)
    ).groupBy( _._1 ) // Map( group -> ( group, List[List[Int]] ) )

    val keys = lsh_buckets.mapValues(_.size).toList // ( key, count )

    val hot = {
      val min = groups.length * minSupport
      keys.filter( _._2 >= min ).map(_._1)
    }

    val hot_buckets = hot.map(
      lsh_buckets(_).map(_._2).fold(List[Int]())( _ ::: _ )
    )

    val freqInBucket = hot_buckets.map( seq => super.mine(seq) )

    freqInBucket.fold(List[List[Int]]())( _ ::: _ ).distinct
  }
}
