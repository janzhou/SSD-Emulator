package org.janzhou.ftl

abstract class FTL(device:Device) {
  def read(lpn:Int):Int
  def write(lpn:Int):Int
  def trim(lpn:Int):Unit
  def gc
}
