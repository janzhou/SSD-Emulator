package org.janzhou.ftl

import akka.actor.{ActorSystem, Props}
import akka.actor.{ActorLogging, Actor, ActorRef}
import akka.pattern.ask

import java.util.concurrent.TimeUnit
import orestes.bloomfilter._
import scala.collection.JavaConverters._
import java.util.concurrent.Semaphore

import scala.collection.mutable.ArrayBuffer

import org.janzhou.cminer._

class CPFTL(
  val device:Device,
  val miner:Miner = new CMiner(),
  val accessSequenceLength:Int = 4096,
  val false_positive_rate:Double = 0.001
) extends DFTL(device) {

  println("CPFTL")

  private val MineSystem = ActorSystem("Miner")
  private val mineActor = MineSystem.actorOf(Props(new MineActor()))
  private val prefetchActor = MineSystem.actorOf(Props(new PrefetchActor()))

  private var correlations = List[(BloomFilter[Int], List[Int])]()
  private var bf:BloomFilter[Int] = new FilterBuilder(0, false_positive_rate).buildBloomFilter()

  override def read(lpn:Int):Int = {
    Static.prefetchStart
    prefetchActor ! NewAccess(lpn)
    Static.prefetchStop

    if ( dftl_table(lpn).cached == false ) {
      Static.cacheMiss
    } else {
      Static.cacheHit
    }
    super.realRead(lpn)
  }

  case class NewAccess(lpn:Int)
  case class NewSequence(seq:List[Int])

  class PrefetchActor extends Actor with ActorLogging {
    private var accessSequence = ArrayBuffer[Int]()

    private def prefetch(lpn:Int) = {
      val (tmp_correlations, tmp_bf) = miner.synchronized{
        (correlations, bf)
      }

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

    def receive = {
      case NewAccess(lpn) => {
        prefetch(lpn)
        mineActor ! NewAccess(lpn)
      }
    }
  }

  class MineActor extends Actor with ActorLogging {
    private var accessSequence = ArrayBuffer[Int]()

    def receive = {
      case NewAccess(lpn) => {
        accessSequence = accessSequence :+ lpn
        if ( accessSequence.length >= accessSequenceLength ) {
          val tmp_accessSequence = accessSequence
          accessSequence = ArrayBuffer[Int]()
          self ! NewSequence(tmp_accessSequence.toList)
        }
      }
      case NewSequence(tmp_accessSequence) => {
        val tmp_correlations = miningFrequentSubSequence(tmp_accessSequence)
        .map(seq => {
          val bf:BloomFilter[Int] = new FilterBuilder(seq.length, false_positive_rate).buildBloomFilter()
          bf.addAll(seq.asJava)
          (bf, seq)
        })

        val tmp_bf = if( false_positive_rate > 0 ) {
          val full = tmp_correlations.map(_._2).fold(List[Int]())( _ ::: _ )
          val tmp_bf:BloomFilter[Int] = new FilterBuilder(full.length, false_positive_rate).buildBloomFilter()
          tmp_bf.addAll(full.asJava)
          tmp_bf
        } else {
          null
        }

        miner.synchronized{
          correlations = tmp_correlations
          bf = tmp_bf
        }
      }
    }


    private def miningFrequentSubSequence (accessSequence:List[Int]):List[List[Int]] = {
      miner.mine(accessSequence.toList)
    }
  }
}
