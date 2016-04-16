package org.janzhou.ftl

import com.typesafe.config.ConfigFactory
import java.util.concurrent.TimeUnit
import org.janzhou.native._

class Device(fd:Int = 0, config:String = "default") {
  private val _config = ConfigFactory.load(config)

  val NumberOfBlocks = _config.getInt("SSD.NumberOfBlocks")
  val PagesPerBlock = _config.getInt("SSD.PagesPerBlock")

  val TotalPages = NumberOfBlocks * PagesPerBlock
  val ReserveSpace = _config.getInt("SSD.ReserveSpace")

  private def sleep(time:Int):Unit = {
    TimeUnit.MICROSECONDS.sleep(time)
  }

  def PageReadDelay   = sleep(_config.getInt("SSD.PageReadDelay"))
  def PageWriteDelay  = sleep(_config.getInt("SSD.PageWriteDelay"))
  def BlockEraseDelay = sleep(_config.getInt("SSD.BlockEraseDelay"))

  val CacheSize = _config.getInt("SSD.CacheSize")

  def move(from:Int, to:Int):Unit = {
    //libc.call.ioctl(fd, 0x00, Array(from, to))
  }
}
