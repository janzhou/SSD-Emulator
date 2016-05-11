package org.janzhou.ftl

import java.util.concurrent.TimeUnit
import orestes.bloomfilter._
import scala.collection.JavaConverters._
import java.util.concurrent.Semaphore

import org.janzhou.cminer._

class CPFTL(
  val device:Device,
  val miner:Miner = new CMiner(),
  val accessSequenceLength:Int = 4096,
  val false_positive_rate:Double = 0.001
) extends DFTL(device) with Runnable {

  println("CPFTL")

  private var accessSequence = List[Int]()
  private var correlations = List[(BloomFilter[Int], List[Int])]()
  private var bf:BloomFilter[Int] = new FilterBuilder(0, false_positive_rate).buildBloomFilter()

  private val do_mining = new Semaphore(0);

  def prefetch(lpn:Int) = {
    val tmp = this.synchronized{
      (correlations, bf)
    }

    val tmp_correlations = tmp._1
    val tmp_bf = tmp._2

    if ( !dftl_table(lpn).cached ) {
      if( false_positive_rate > 0 ) {
        if ( tmp_bf.contains(lpn) ) {
          for( (bf, correlation) <- tmp_correlations ) {
            if ( bf.contains(lpn) ) {
              correlation.foreach( lpn => cache(lpn) )
            }
          }
        }
      } else {
        tmp_correlations.map(_._2).filter( _.contains(lpn) )
        .foreach( seq =>
          seq.foreach( lpn => cache(lpn) )
        )
      }
    }
  }

  override def read(lpn:Int):Int = {
    Static.prefetchStart
    val sequence_length = this.synchronized{
      accessSequence = accessSequence :+ lpn
      accessSequence.length
    }

    if ( sequence_length >= accessSequenceLength ) {
      if ( do_mining.availablePermits == 0 ) do_mining.release
    }

    prefetch(lpn)
    Static.prefetchStop
    super.read(lpn)
  }

  private def miningFrequentSubSequence (accessSequence:List[Int]):List[List[Int]] = {
    miner.mine(accessSequence)
  }

  override def run = {

    do_mining.acquire

    val tmp_accessSequence = this.synchronized {
      val sequence = accessSequence
      accessSequence = List[Int]()
      sequence
    }

    val (tmp_correlations, tmp_bf) = if( false_positive_rate > 0 ) {
      val tmp_correlations = miningFrequentSubSequence(tmp_accessSequence)
      .map(seq => {
        val bf:BloomFilter[Int] = new FilterBuilder(seq.length, false_positive_rate).buildBloomFilter()
        bf.addAll(seq.asJava)
        (bf, seq)
      })

      val full = tmp_correlations.map(_._2).fold(List[Int]())( _ ::: _ )
      val tmp_bf:BloomFilter[Int] = new FilterBuilder(full.length, false_positive_rate).buildBloomFilter()
      tmp_bf.addAll(full.asJava)

      (tmp_correlations, tmp_bf)
    } else {
      val tmp_correlations = miningFrequentSubSequence(tmp_accessSequence)
      .map(seq => {
        val bf:BloomFilter[Int] = null
        (bf, seq)
      })

      (tmp_correlations, null)
    }

    this.synchronized {
      correlations = tmp_correlations
      bf = tmp_bf
    }
  }

  val thread = new Thread(this)
  thread.start()

}
