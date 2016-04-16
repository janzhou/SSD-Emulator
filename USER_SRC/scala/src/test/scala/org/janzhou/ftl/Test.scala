package org.janzhou.ftl

object Test {
  def main (args: Array[String]) {
    val ftl = new DFTL(new Device())

    for ( i <- 1 to 8195 ) {
      println("write " + 1 + " ppn " + ftl.write(1))
    }
    println("write " + 0 + " ppn " + ftl.write(0))
    println("write " + 1 + " ppn " + ftl.write(1))
    ftl.gc
  }
}
