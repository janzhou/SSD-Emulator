package org.janzhou.cminer

import info.debatty.java.lsh.LSHSuperBit

class LSHMiner (
  override val minSupport:Double = 0.1,
  override val splitSize:Int = 512,
  override val depth:Int = 64,
  val vectorSize:Int = 512,
  val stages:Int = 8,
  val buckets:Int = 128
) extends CMiner (minSupport, splitSize, depth) {
  private val lsh = new LSHSuperBit(stages, buckets, vectorSize)

  override def mine(seq:List[Int]):List[List[Int]] = {
    val lsh_hash_groups = seq.grouped(splitSize).toList.map( seq =>
      (lsh.hash(seq.toArray)(stages - 1), seq)
    ).groupBy( _._1 ) // Map( group -> ( group, List[List[Int]] ) )

    val sorted_keys = lsh_hash_groups.keys.toList.sortWith( _ > _ )
    val (hot, cold) = sorted_keys.splitAt ( sorted_keys.length / 2 )
    val hot_groups = hot.map( group => {
      lsh_hash_groups(group).map( _._2 ).reduce( _ ::: _ )
    }) // List[List[Int]]
    hot_groups.map( seq => super.mine(seq) ).reduce( _ ::: _ ).distinct
  }
}
