package org.janzhou.ftl

import java.util.concurrent.TimeUnit
import orestes.bloomfilter._
import scala.collection.JavaConverters._

class CPFTL(device:Device) extends DFTL(device) {

  println("CPFTL")

  private var accessSequence = List[Int]()
  private var subSequence = List[List[Int]]()
  private var bfs = List[BloomFilter[Int]]()

  private def miningFrequentSubSequence = {
    subSequence = accessSequence.grouped(64).toList
    bfs = subSequence.map(seq => {
      val bf:BloomFilter[Int] = new FilterBuilder(seq.length, 0.01).buildBloomFilter()
      bf.addAll(seq.asJava)
      bf
    })
  }

  def prefetch(lpn:Int) = {
    if ( !dftl_table(lpn).cached ) {
      for ( i <- 0 to bfs.length - 1 ) {
        val bf = bfs(i)
        if ( bf.contains(lpn) ) {
          subSequence(i).foreach( lpn => cache(lpn) )
        }
      }
      cache(lpn)
    }
  }

  override def read(lpn:Int):Int = {
    accessSequence = accessSequence :+ lpn
    if ( accessSequence.length >= 512 ) {
      miningFrequentSubSequence
      accessSequence = List[Int]()
    }

    prefetch(lpn)
    super.read(lpn)
  }

}
