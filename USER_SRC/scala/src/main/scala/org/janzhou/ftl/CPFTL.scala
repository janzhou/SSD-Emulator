package org.janzhou.ftl

import java.util.concurrent.TimeUnit

class CPFTL(device:Device) extends DFTL(device) {

  println("CPFTL")

  private var accessSequence = List[Int]()

  private def frequentSubSequence(sequence:List[Int]):List[List[Int]] = {
    sequence.grouped(500).toList
  }

  override def read(lpn:Int):Int = {
    accessSequence = accessSequence :+ lpn
    super.read(lpn)
  }

}
