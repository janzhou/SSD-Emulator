package org.janzhou.ftl

object Test {
  def main (args: Array[String]) {
    val device = new Device(0, "nodelay")
    val ftl = new DFTL(device)

    println(device.SectorsPerPage)

    //for ( i <- 1 to 8195 ) {
    //  println("write " + 1 + " ppn " + ftl.write(1))
    //}
    //println("write " + 0 + " ppn " + ftl.write(0))
    //println("write " + 1 + " ppn " + ftl.write(1))
    //ftl.gc
  }
}
