package org.janzhou.ftl

import scala.collection.mutable.ArrayBuffer

import org.janzhou.console

object Static {

  def now:Long = {
    System.nanoTime()
  }

  private var _flashDelayAvg = 0L
  private var _flashDelayCount = 0L
  def flashDelay(delay:Long) = {
    _flashDelayCount += 1
    _flashDelayAvg = _flashDelayAvg + (delay - _flashDelayAvg)/_flashDelayCount;
  }
  def flashDelayPrint = {
    console.debug("flash delay " + _flashDelayAvg + " ns")
  }


  private var _requestIn = 0L
  private var _requestAvg = 0L
  private var _requestCount = 0L
  private var _requestNum = 0L

  def requestIn = {
    _requestIn = now
  }

  def requestOut(num:Int) = {
    val delay = (now - _requestIn)
    _requestCount += 1
    _requestAvg = _requestAvg + (delay - _requestAvg)/_requestCount;

    _requestNum = _requestNum + (num - _requestNum)/_requestCount;
    _requestIn = 0L
  }

  def requestPrint = {
    console.debug("request " + _requestNum + " avg. delay " + _requestAvg + " s")
  }

  private var _prefetchStart:Long = 0L
  private var _prefetchDelay:Long = 0L
  private var _prefetchCount:Long = 0L
  private var _prefetchNumber:Long = 0L

  def prefetchStart = {
    _prefetchStart = now

  }

  def prefetchStop(num:Int) = {
    _prefetchDelay += ( now - _prefetchStart )
    _prefetchCount += 1
    _prefetchNumber += num
    printPrefetch
  }

  private def printPrefetch = {
    if ( _prefetchCount >= 8192 ) {
      console.info("prefetch " + _prefetchNumber + " delay " + ( _prefetchDelay / _prefetchCount ) + " ns" )
      _prefetchDelay = 0L
      _prefetchCount = 0L
      _prefetchNumber = 0L
    }
  }

  def prefetchDelay = _prefetchDelay

  private var _cacheHit = 0
  def cacheHit = {
    _cacheHit += 1
    printCache
  }

  private var _cacheMiss = 0
  def cacheMiss = {
    _cacheMiss += 1
    printCache
  }

  private def printCache = {
    if( _cacheMiss + _cacheHit >= 8192 ) {
      val ratio:Double =   _cacheHit  * 100 / ( _cacheHit + _cacheMiss )
      console.info("cache hit: " + ratio )
      _cacheHit = 0
      _cacheMiss = 0
    }
  }

  private var _miningStart:Long = 0L
  def miningStart(seq:ArrayBuffer[Int]) = {
    _miningStart = now
    console.info("mining start: " + seq.length)
  }

  def miningStop(correlations:ArrayBuffer[ArrayBuffer[Int]]) = {
    console.info("mining stop: " + correlations.length + " time spend: " + (now - _miningStart) + " ns")
  }

}
