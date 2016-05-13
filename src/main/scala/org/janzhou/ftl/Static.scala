package org.janzhou.ftl

object Static {

  def now = {
    System.nanoTime()
  }

  private var _prefetchStart:Long = 0L
  private var _prefetchDelay:Long = 0L
  private var _prefetchCount:Long = 0L

  def prefetchStart = {
    _prefetchStart = now

  }

  def prefetchStop = {
    _prefetchDelay += ( now - _prefetchStart )
    _prefetchCount += 1
    printPrefetch
  }

  private def printPrefetch = {
    if ( _prefetchCount >= 4096 ) {
      println("prefetch delay " + ( _prefetchDelay / _prefetchCount ) + " ns" )
      _prefetchDelay = 0L
      _prefetchCount = 0L
    }
  }

  def prefetchDelay = _prefetchDelay

  private var _cacheHit = 0
  def cacheHit = {
    _cacheHit += 1
  }

  private var _cacheMiss = 0
  def cacheMiss = {
    _cacheMiss += 1
  }

  private def printCache = {
    if( _cacheMiss + _cacheHit >= 4096 ) {
      println("cache Hit/Miss " + _cacheHit + "/" + _cacheMiss)
    }
  }

}
