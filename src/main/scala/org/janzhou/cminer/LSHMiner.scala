package org.janzhou.cminer

import info.debatty.java.lsh.LSHSuperBit

class LSHMiner (
  override val minSupport:Int = 2,
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

    /*
    val keys = lsh_buckets.mapValues(_.size).toList // ( key, count )

    val min = groups.length * minSupport

    val hot = keys.filter( _._2 >= min ).map(_._1)

    val hot_buckets = hot.map(
      lsh_buckets(_).map(_._2)
    )

    val freqInBucket = hot_buckets.map( split => mineSplit(split) )
    */
    val freqInBucket = lsh_buckets.map(_._2).map( splits => mineSplits(splits.map(_._2)) )

    freqInBucket.fold(List[List[Int]]())( _ ::: _ ).distinct
  }
}
