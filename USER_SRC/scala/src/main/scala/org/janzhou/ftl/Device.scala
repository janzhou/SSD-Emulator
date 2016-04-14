package org.janzhou.ftl

import com.typesafe.config.ConfigFactory

class Device(config:String = "default") {
  private val _config = ConfigFactory.load(config)

  val NumberOfBlocks = _config.getInt("SSD.NumberOfBlocks")
  val PagesPerBlock = _config.getInt("SSD.PagesPerBlock")

  val TotalPages = NumberOfBlocks * PagesPerBlock
  val ReserveSpace = _config.getInt("SSD.ReserveSpace")

  val PageReadDelay = _config.getInt("SSD.PageReadDelay")
  val PageWriteDelay = _config.getInt("SSD.PageWriteDelay")
  val BlockEraseDelay = _config.getInt("SSD.BlockEraseDelay")

  val CacheSize = _config.getInt("SSD.CacheSize")

  def move(from_ppn:Int, to_ppn:Int):Unit = {}
}
