package org.janzhou.ftl

import com.typesafe.config.Config
import java.util.concurrent.TimeUnit
import org.janzhou.native._

class Device(fd:Int = 0, config:Config) {

  val NumberOfBlocks = config.getInt("SSD.NumberOfBlocks")
  val PagesPerBlock = config.getInt("SSD.PagesPerBlock")
  val SectorsPerPage = config.getInt("SSD.PageSize") / config.getInt("SSD.SectorSize")

  val ReserveBlocks = config.getInt("SSD.ReserveBlocks")
  val TotalPages = ( NumberOfBlocks - ReserveBlocks ) * PagesPerBlock

  private def sleep(time:Int):Unit = {
    TimeUnit.MICROSECONDS.sleep(time)
  }

  def PageReadDelay   = sleep(config.getInt("SSD.PageReadDelay"))
  def PageWriteDelay  = sleep(config.getInt("SSD.PageWriteDelay"))
  def BlockEraseDelay = sleep(config.getInt("SSD.BlockEraseDelay"))

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
