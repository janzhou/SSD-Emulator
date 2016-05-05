package org.janzhou.ftl

import java.util.concurrent.TimeUnit
import orestes.bloomfilter._
import scala.collection.JavaConverters._
import java.util.concurrent.Semaphore
import info.debatty.java.lsh.LSHSuperBit

class CPFTL(device:Device) extends DFTL(device) with Runnable {

  println("CPFTL")

  private val n = 100 // Size of vectors
  private val stages = 2 // the number of stages is also sometimes called thge number of bands
  private val buckets = 10;
  private val lsh = new LSHSuperBit(stages, buckets, n)

  private val false_positive_rate = 0.001

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
      if ( tmp_bf.contains(lpn) ) {
        for( correlation <- correlations ) {
          val bf = correlation._1
          val seq = correlation._2
          if ( bf.contains(lpn) ) {
            seq.foreach( lpn => cache(lpn) )
          }
        }
      }
    }
  }

  override def read(lpn:Int):Int = {
    val sequence_length = this.synchronized{
      accessSequence = accessSequence :+ lpn
      accessSequence.length
    }

    if ( sequence_length >= 512 ) {
      if ( do_mining.availablePermits == 0 ) do_mining.release
    }

    prefetch(lpn)
    super.read(lpn)
  }

  private def miningFrequentSubSequence (accessSequence:List[Int]):List[List[Int]] = {
    accessSequence.grouped(64).toList
  }

  override def run = {

    do_mining.acquire

    val tmp_accessSequence = this.synchronized {
      val sequence = accessSequence
      accessSequence = List[Int]()
      sequence
    }

    val tmp_correlations = miningFrequentSubSequence(tmp_accessSequence)
    .map(seq => {
      val bf:BloomFilter[Int] = new FilterBuilder(seq.length, false_positive_rate).buildBloomFilter()
      bf.addAll(seq.asJava)
      (bf, seq)
    })

    tmp_correlations.foreach( seq => {
      print(lsh.hash(seq._2.toArray))
      println(seq._2)
    })

    val full = tmp_correlations.map(_._2).reduce( _ ::: _ )
    val tmp_bf:BloomFilter[Int] = new FilterBuilder(full.length, false_positive_rate).buildBloomFilter()
    tmp_bf.addAll(full.asJava)

    this.synchronized {
      correlations = tmp_correlations
      bf = tmp_bf
    }

  }

  val thread = new Thread(this)
  thread.start()

}
