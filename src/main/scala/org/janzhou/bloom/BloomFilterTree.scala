package org.janzhou.bloom

import breeze.util.BloomFilter
import scala.reflect.ClassTag

class BloomFilterTree[T:ClassTag](
  val false_positive_rate:Double = 0.001
) {
  private var array = Iterable[(BloomFilter[T], Iterable[T])]()
  private var bf:BloomFilter[T] = null

  def ++(that:Iterable[Iterable[T]]):this.type = add(that)
  def +=(that:Iterable[Iterable[T]]):this.type = add(that)
  
  def add(that:Iterable[Iterable[T]]):this.type = {
    array = array ++ that.map(array => {
      val bf:BloomFilter[T] = BloomFilter.optimallySized[T](
        array.size, false_positive_rate
      )

      array.foreach( bf += _ )
      (bf, array)
    })

    val size = array.map( _._2.size ).fold(0)(_ + _)

    bf = BloomFilter.optimallySized[T]( size, false_positive_rate )

    array.foreach( _._2.foreach(
      bf += _
    ))

    this
  }

  def search(element:T):Iterable[Iterable[T]] = {
    if( bf != null && bf.contains(element) ) {
      array.filter( x =>
        x._1.contains(element)// && x._2.contains(element)
      ).map( _._2 )
    } else  Iterable[Iterable[T]]()
  }
}
