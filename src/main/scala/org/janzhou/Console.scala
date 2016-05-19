package org.janzhou

object console {

  def print_level(s:String) = level(s)

  def level(s:String) = {
    val (p, l) = prefix.zipWithIndex.filter{ case (p, l) => p == "["+s+"]" }.head
    _print_level = l
  }

  def error(s:Any) = console_print(s, 0)
  def warn(s:Any) = console_print(s, 1)
  def info(s:Any) = console_print(s, 2)
  def info1(s:Any) = console_print(s, 3)
  def info2(s:Any) = console_print(s, 4)
  def info3(s:Any) = console_print(s, 5)
  def log(s:Any) = info(s)
  def debug(s:Any) = console_print(s, 6)
  def debug1(s:Any) = console_print(s, 7)
  def debug2(s:Any) = console_print(s, 8)
  def debug3(s:Any) = console_print(s, 9)

  private val print_error = 0
  private val print_warn  = 1
  private val print_info  = 2
  private val print_debug = 3

  private var _print_level = print_info

  private val prefix = Array(
    "[error] ",
    "[warn] ",
    "[info] ",
    "[info1] ",
    "[info2] ",
    "[info3] ",
    "[debug] ",
    "[debug1] ",
    "[debug2] ",
    "[debug3] "
  )

  private def console_print(s:Any, level:Int) {
    if ( _print_level >= level ) {
      print(prefix(level))
      println(s)
    }
  }
}
