package org.janzhou.test

import org.janzhou.console
import org.janzhou.ftl._
import org.janzhou.native._
import org.janzhou.cminer._
import com.typesafe.config.{ Config, ConfigFactory }

import scala.collection.mutable.ArrayBuffer
import java.io._

object FTLTest {
  private def load_config(config: Array[String]):Config = {
    val _config = config.reverse ++ Array("default", "cminer", "lshminer", "cpftl", "console")
    _config.map(config => if( config.endsWith(".conf") ) {
      ConfigFactory.parseFile(new File(config))
    } else ConfigFactory.load(config) )
    .reduce(_.withFallback(_))
  }

  class GCThread(ftl:FTL) extends Thread {
    var _heartbeat = 0

    def heartbeat = {
      _heartbeat = 0
    }

    var _stop = false
    def stopGC = {
      _stop = true
    }

    override def run() {
      while ( !_stop ) {
        this.synchronized {
          _heartbeat += 1
          if ( _heartbeat >= 1024 ) {
            console.info("background gc")
            if (!ftl.gc) {
              _heartbeat = 0
            }
          }
        }

        if ( _heartbeat < 1024 ) {
          Thread.sleep( 1000 )
        }
      }
    }
  }

  def main (args: Array[String]) {
    assert( args.length >= 2 )
    val config = {
      val ( _, config ) = args splitAt 2
      load_config(config)
    }

    console.print_level(config.getString("Console.print_level"))

    val device = new Device(0, config)

    val ftl:FTL = {
      args(0) match {
        case "dftl" => new DFTL(device)
        case "simplecpftl" => new CPFTL(device,
          new SimpleMiner(
            config.getInt("SimpleMiner.splitSize")
          ),
          config.getInt("CPFTL.accessSequenceLength")
        )
        case "cpftl" => new CPFTL(device,
          new CMiner(
            config.getInt("CMiner.minSupport"),
            config.getInt("CMiner.splitSize"),
            config.getInt("CMiner.depth")
          ),
          config.getInt("CPFTL.accessSequenceLength")
        )
        case "lshftl" => new CPFTL(device,
          new LSHMiner(
            config.getInt("LSHMiner.minSupport"),
            config.getInt("LSHMiner.splitSize"),
            config.getInt("LSHMiner.depth")
          ),
          config.getInt("CPFTL.accessSequenceLength")
        )
        case _ => new DirectFTL(device)
      }
    }

    val gc_thread = new GCThread(ftl)
    gc_thread.start()

    val workloads = {
      val oos = new ObjectInputStream(new FileInputStream(args(1)))
      val seq = oos.readObject().asInstanceOf[ArrayBuffer[Int]]
      oos.close
      seq
    }

    workloads.foreach( lpn => gc_thread.synchronized {
      ftl.read(lpn)
    })
    
    workloads.foreach( lpn => gc_thread.synchronized {
      ftl.read(lpn)
    })
  }
}
