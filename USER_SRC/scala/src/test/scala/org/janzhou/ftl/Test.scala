package org.janzhou.ftl

object Test {
  def main (args: Array[String]) {
    val ftl = new DFTL(new Device())

    println("read " + 1 + " ppn " + ftl.read(1))
    println("write " + 1 + " ppn " + ftl.write(1))
    println("write " + 2 + " ppn " + ftl.write(2))
    println("read " + 1 + " ppn " + ftl.read(1))
    println("write " + 1 + " ppn " + ftl.write(1))
    println("read " + 1 + " ppn " + ftl.read(1))
  }
}
