package org.janzhou.ftl

import akka.actor.{ActorSystem, Props}
import akka.actor.{ActorLogging, Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

import org.janzhou.bloom.BloomFilterTree

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import org.janzhou.console
import org.janzhou.cminer._
import java.io._

class CPFTL(
  val device:Device,
  val miner:Miner = new CMiner(),
  val accessSequenceLength:Int = 4096,
  val false_positive_rate:Double = 0.001
) extends DFTL(device) {

  console.debug("CPFTL")

  private val mineActor = ActorSystem("Miner").actorOf(Props(new MineActor()))

  override def read(lpn:Int):Int = {
    if ( dftl_table(lpn).cached == false ) {
      Static.cacheMiss
    } else {
      Static.cacheHit
    }

    Static.prefetchStart
    mineActor ! NewAccess(lpn)
    Static.prefetchStop(prefetch(lpn))

    realRead(lpn)
  }

  case class NewAccess(lpn:Int)
  case class NewSequence(seq:ArrayBuffer[Int])

  private var tree = new BloomFilterTree[Int](false_positive_rate)

  private def prefetch(lpn:Int):Int = {
    var fetched = 0
    if ( dftl_table(lpn).cached == false ) {
      tree.search(lpn).foreach( seq => {
        seq.foreach( lpn => {
          cache(lpn)
          if ( dftl_table(lpn).cached == false ) fetched += 1
        })
      })
    }
    fetched
  }

  class MineActor extends Actor with ActorLogging {
    private var accessSequence = ArrayBuffer[Int]()

    def receive = {
      case NewAccess(lpn) => {
        accessSequence += lpn
        if ( accessSequence.length >= accessSequenceLength ) {
          val tmp_accessSequence = accessSequence
          accessSequence = ArrayBuffer[Int]()
          self ! NewSequence(tmp_accessSequence)
        }
      }
      case NewSequence(tmp_accessSequence) => {
        val oos = new ObjectOutputStream(new FileOutputStream("/tmp/accessSequence." + tmp_accessSequence.##))
        oos.writeObject(tmp_accessSequence)
        oos.close

        Static.miningStart(tmp_accessSequence)
        val raw_correlations = miningFrequentSubSequence(tmp_accessSequence)
        Static.miningStop(raw_correlations)

        tree = new BloomFilterTree[Int](false_positive_rate) ++ raw_correlations
      }
    }


    private def miningFrequentSubSequence (accessSequence:ArrayBuffer[Int]):ArrayBuffer[ArrayBuffer[Int]] = {
      miner.mine(accessSequence)
    }
  }
}
