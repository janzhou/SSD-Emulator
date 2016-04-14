package org.janzhou.ftl

import java.util.concurrent.TimeUnit

class DFTL(device:Device) extends FTL(device) {

  println("DFTL")

  class dftl_block(val id:Int) {
    var lpns = List[Int]()
    var clean:Int = device.PagesPerBlock
    var dirty:Int = 0
  }

  val dftl_blocks = for( i <- 0 to device.NumberOfBlocks - 1 ) yield {
    new dftl_block(i)
  }
  var free_block = dftl_blocks.toList
  var gc_block = List[dftl_block]()

  class dftl_mapping_entry(val lpn:Int) {
    var block:dftl_block = null
    var ppn:Int = 0
    var cached:Boolean = false
    var dirty:Boolean = false
  }

  val dftl_table = for ( i <- 0 to ( device.TotalPages - ( device.TotalPages / 100 ) * device.ReserveSpace ) ) yield {
    val block_id = i / device.PagesPerBlock
    new dftl_mapping_entry(i)
  }
  var dftl_cache = List[dftl_mapping_entry]()

  private def sleep(time:Int):Unit = {
    TimeUnit.MICROSECONDS.sleep(time)
  }

  def clean_cache = {
    while( dftl_cache.length > device.CacheSize ) {
      val cache = dftl_cache.head

      if ( cache.dirty ) {
        sleep(device.PageReadDelay)
        sleep(device.PageWriteDelay)

        dftl_cache = dftl_cache.drop(1)
      }
    }
  }

  def read(lpn:Int):Int = {
    if ( dftl_table(lpn).cached == false ) {
      dftl_table(lpn).cached = true
      dftl_table(lpn).dirty = false

      dftl_cache = dftl_cache :+ dftl_table(lpn)

      sleep(device.PageReadDelay)
    }

    sleep(device.PageReadDelay) //data
    dftl_table(lpn).ppn
  }

  def gc = {}

  def trim(lpn:Int):Unit = {
    if ( dftl_table(lpn).block != null ) {
      if ( dftl_table(lpn).cached == false ) {
        sleep(device.PageReadDelay)
      }

      val block = dftl_table(lpn).block
      block.lpns = block.lpns.filter(_ != lpn)
      block.dirty += 1

      if ( block.dirty >= device.PagesPerBlock ) {
        gc_block = gc_block :+ block
      }

      dftl_table(lpn).block = null
      dftl_table(lpn).ppn = 0
      dftl_table(lpn).cached = true
      dftl_table(lpn).dirty = true

      dftl_cache = dftl_cache :+ dftl_table(lpn)
    }
  }

  def write(lpn:Int):Int = {
    if( free_block.isEmpty ) gc
    val block = free_block.head

    if ( dftl_table(lpn).block != null ) trim(lpn)

    val ppn = ( block.id + 1 ) * device.PagesPerBlock - block.clean
    block.clean -= 1
    if( block.clean <= 0 ) {
      free_block = free_block.drop(1)
    }
    block.lpns = block.lpns :+ lpn

    dftl_table(lpn).block = block
    dftl_table(lpn).ppn = ppn

    sleep(device.PageWriteDelay) //data
    ppn
  }
}
