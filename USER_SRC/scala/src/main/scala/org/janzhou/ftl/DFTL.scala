package org.janzhou.ftl

class DFTL(device:Device) extends FTL(device) {

  println("DFTL")

  class dftl_block(val id:Int) {
    var lpns = List[Int]()
    var clean:Int = device.PagesPerBlock
    var dirty:Int = 0
  }

  protected val dftl_blocks = for( i <- 0 to device.NumberOfBlocks - 1 ) yield {
    new dftl_block(i)
  }
  protected var free_block = dftl_blocks.toList
  protected var gc_block = List[dftl_block]()
  protected var full_block = List[dftl_block]()

  class dftl_mapping_entry(val lpn:Int) {
    var block:dftl_block = null
    var ppn:Int = device.TotalPages
    var cached:Boolean = false
    var dirty:Boolean = false
  }

  protected val dftl_table = for ( i <- 0 to device.TotalPages - 1 ) yield {
    val block_id = i / device.PagesPerBlock
    new dftl_mapping_entry(i)
  }
  protected var dftl_cache = List[dftl_mapping_entry]()

  protected def clean_cache = {
    while( dftl_cache.length > device.CacheSize ) {
      val cache = dftl_cache.head

      if ( cache.dirty ) {
        device.PageReadDelay
        device.PageWriteDelay

        dftl_cache = dftl_cache.drop(1)
      }
    }
  }

  def read(lpn:Int):Int = {
    if ( dftl_table(lpn).cached == false ) {
      dftl_table(lpn).cached = true
      dftl_table(lpn).dirty = false

      dftl_cache = dftl_cache :+ dftl_table(lpn)

      device.PageReadDelay
    }

    device.PageReadDelay //data
    dftl_table(lpn).ppn
  }

  protected def move = {
    val block = {
      val candidate_blocks = full_block

      var gc = candidate_blocks.head
      for ( b <- candidate_blocks ) {
        if ( b.dirty > gc.dirty ) {
          gc = b
        }
      }
      gc
    }

    for ( lpn <- block.lpns ) {
      val from = read(lpn)
      val to = write_withoutGC(lpn)
      device.move(from, to)
    }
  }

  def gc = {
    if ( gc_block.isEmpty ) {
      move
    }

    val block = gc_block.head
    block.lpns = List[Int]()
    block.clean = device.PagesPerBlock
    block.dirty = 0
    gc_block = gc_block.drop(1)
    free_block = free_block :+ block

    device.BlockEraseDelay
  }

  def trim(lpn:Int):Unit = {
    if ( dftl_table(lpn).block != null ) {
      if ( dftl_table(lpn).cached == false ) {
        device.PageReadDelay
      }

      val block = dftl_table(lpn).block
      block.lpns = block.lpns.filter(_ != lpn)
      block.dirty += 1

      if ( block.dirty >= device.PagesPerBlock ) {
        gc_block = gc_block :+ block
        full_block = full_block.filter( _ != block )
      }

      dftl_table(lpn).block = null
      dftl_table(lpn).ppn = device.TotalPages
      dftl_table(lpn).cached = true
      dftl_table(lpn).dirty = true

      dftl_cache = dftl_cache :+ dftl_table(lpn)
    }
  }

  def write(lpn:Int):Int = {
    if( free_block.length <= 2 ) gc
    write_withoutGC(lpn)
  }

  private def write_withoutGC(lpn:Int):Int = {
    val block = free_block.head

    if ( dftl_table(lpn).block != null ) trim(lpn)

    val ppn = ( block.id + 1 ) * device.PagesPerBlock - block.clean
    block.clean -= 1
    if( block.clean <= 0 ) {
      free_block = free_block.drop(1)
      full_block = full_block :+ block
    }
    block.lpns = block.lpns :+ lpn

    dftl_table(lpn).block = block
    dftl_table(lpn).ppn = ppn

    device.PageWriteDelay //data
    ppn
  }
}
