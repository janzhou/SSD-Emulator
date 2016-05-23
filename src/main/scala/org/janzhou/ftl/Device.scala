package org.janzhou.ftl

import com.typesafe.config.Config
import org.janzhou.native._

class Device(fd:Int = 0, config:Config) {

  val NumberOfBlocks = config.getInt("SSD.NumberOfBlocks")
  val PagesPerBlock = config.getInt("SSD.PagesPerBlock")
  val SectorsPerPage = config.getInt("SSD.PageSize") / config.getInt("SSD.SectorSize")

  val ReserveBlocks = config.getInt("SSD.ReserveBlocks")
  val TotalPages = ( NumberOfBlocks - ReserveBlocks ) * PagesPerBlock

  private def delay(micros:Long) = {
    val in = System.nanoTime()
    val waitUntil = in + (micros * 1000)
    var out = System.nanoTime()
    while(waitUntil > out){
      out = System.nanoTime()
    }
    Static.flashDelay(out - in)
  }

  private val _PageReadDelay   = config.getLong("SSD.PageReadDelay")
  private val _PageWriteDelay  = config.getLong("SSD.PageWriteDelay")
  private val _BlockEraseDelay = config.getLong("SSD.BlockEraseDelay")

  def PageReadDelay   = delay(_PageReadDelay)
  def PageWriteDelay  = delay(_PageWriteDelay)
  def BlockEraseDelay = delay(_BlockEraseDelay)

  val CacheSize = config.getInt("SSD.CacheSize")

  private val moveArgs = libc.run().malloc(16)
  def move(from:Int, to:Int):Unit = {
    moveArgs.setLong(0, from)
    moveArgs.setLong(8, to)
    if( from != to ) libc.call.ioctl(fd, 0x40107804, moveArgs)
  }

  override def finalize = {
    libc.call.free(moveArgs)
  }
}
