package org.janzhou.ftl

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

  def read(lpn:Int):Int = {
    dftl_table(lpn).ppn
  }

  def gc = {}

  def trim(lpn:Int):Unit = {
    if ( dftl_table(lpn).block != null ) {
      val block = dftl_table(lpn).block
      block.lpns = block.lpns.filter(_ != lpn)
      block.dirty += 1

      if ( block.dirty >= device.PagesPerBlock ) {
        gc_block = gc_block :+ block
      }

      dftl_table(lpn).block = null
      dftl_table(lpn).ppn = 0
      dftl_table(lpn).cached = false
      dftl_table(lpn).dirty = false
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

    dftl_table(lpn).ppn = ppn
    dftl_table(lpn).block = block
    dftl_table(lpn).cached = true
    dftl_table(lpn).cached = false

    ppn
  }
}
