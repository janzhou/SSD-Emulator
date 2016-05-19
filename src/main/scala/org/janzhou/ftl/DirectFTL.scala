package org.janzhou.ftl

import org.janzhou.console

class DirectFTL(device:Device) extends FTL(device) {

  console.debug("DirectFTL")

  def read(lpn:Int):Int = {
    lpn
  }

  def write(lpn:Int):Int = {
    lpn
  }

  def trim(lpn:Int):Unit = {}
  def gc:Boolean = false
}
