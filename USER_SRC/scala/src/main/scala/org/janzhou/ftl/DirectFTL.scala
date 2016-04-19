package org.janzhou.ftl

class DirectFTL(device:Device) extends FTL(device) {

  println("DirectFTL")

  def read(lpn:Int):Int = {
    lpn
  }

  def write(lpn:Int):Int = {
    lpn
  }

  def trim(lpn:Int):Unit = {}
  def gc = {}
}
