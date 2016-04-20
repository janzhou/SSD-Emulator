package org.janzhou.ftl

import com.typesafe.config.ConfigFactory
import java.util.concurrent.TimeUnit
import org.janzhou.native._

class Device(fd:Int = 0, config:String = "default") {
  private val _config = ConfigFactory.load(config).withFallback(
    ConfigFactory.load("default")
  )

  val NumberOfBlocks = _config.getInt("SSD.NumberOfBlocks")
  val PagesPerBlock = _config.getInt("SSD.PagesPerBlock")
  val SectorsPerPage = _config.getInt("SSD.PageSize") / _config.getInt("SSD.SectorSize")

  val ReserveBlocks = _config.getInt("SSD.ReserveBlocks")
  val TotalPages = ( NumberOfBlocks - ReserveBlocks ) * PagesPerBlock

  private def sleep(time:Int):Unit = {
    TimeUnit.MICROSECONDS.sleep(time)
  }

  def PageReadDelay   = sleep(_config.getInt("SSD.PageReadDelay"))
  def PageWriteDelay  = sleep(_config.getInt("SSD.PageWriteDelay"))
  def BlockEraseDelay = sleep(_config.getInt("SSD.BlockEraseDelay"))

  val CacheSize = _config.getInt("SSD.CacheSize")

  private val moveArgs = libc.run().malloc(16)
  def move(from:Int, to:Int):Unit = {
    moveArgs.setLong(0, from)
    moveArgs.setLong(8, to)
    libc.call.ioctl(fd, 0x40107804, moveArgs)
  }

  override def finalize = {
    libc.call.free(moveArgs)
  }
}
