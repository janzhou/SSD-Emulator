package org.janzhou.bloom

import breeze.util.BloomFilter
import scala.reflect.ClassTag
import java.util.BitSet

class BloomFilterNew[@specialized(Int, Long) T](
  val numBuckets: Int,
  val numHashFunctions: Int,
  val bits: BitSet
) {
  def this(numBuckets: Int, numHashFunctions: Int) = this(numBuckets, numHashFunctions, new BitSet(numBuckets))
  def this(numBuckets: Int) = this(numBuckets, 3)

  val bf = new BloomFilter[T](numBuckets, numHashFunctions, bits)

  def containKeys(keys:Iterable[Int]):Boolean = {
    keys.forall(i => bf.bits.get(i))
  }

  def contains(o: T) = bf.contains(o)

   def +=(o: T): this.type = {
     bf += o
     this
   }

   def +=(o: Iterable[T]): this.type = {
     o.foreach( bf += _ )
     this
   }

   def &=(that: BloomFilterNew[T]):this.type = {
     this.bf &= that.bf
     this
   }

   def genKyes(o: T) = {
     bf.activeBuckets(o)
   }
}

object BloomFilterNew {
  /**
   * Returns the optimal number of buckets  (m) and hash functions (k)
   *
   * The formula is:
   * {{{
   * val m = ceil(-(n * log(p)) / log(pow(2.0, log(2.0))))
   * val k = round(log(2.0) * m / n)
   * }}}
   *
   * @param expectedNumItems
   * @param falsePositiveRate
   * @return
   */
  def optimalSize(expectedNumItems: Double, falsePositiveRate: Double): (Int, Int) = {
    val n = expectedNumItems
    val p = falsePositiveRate
    import scala.math._
    val m = ceil(-(n * log(p)) / log(pow(2.0, log(2.0))))
    val k = round(log(2.0) * m / n)
    (m.toInt, k.toInt)
  }

  /**
   * Returns a BloomFilter that is optimally sized for the expected number of inputs and false positive rate
   * @param expectedNumItems
   * @param falsePositiveRate
   * @tparam T
   * @return
   */
  def optimallySized[T](expectedNumItems: Double, falsePositiveRate: Double): BloomFilterNew[T] = {
    val (buckets, funs) = optimalSize(expectedNumItems, falsePositiveRate)
    new BloomFilterNew(buckets, funs)
  }
}

class BloomFilterTreeNew[T:ClassTag](
  override val false_positive_rate:Double = 0.001
) extends BloomFilterTree(false_positive_rate) {

  private var leaf = Iterable[(BloomFilterNew[T], Iterable[T])]()
  private var root:BloomFilterNew[T] = null

  override def add(that:Iterable[Iterable[T]]):this.type = {
    val input = (leaf.map(_._2) ++ that)
    val size = input.map( _.size ).fold(0)(_ + _)
    leaf = input.map(array => {
      val bf:BloomFilterNew[T] = BloomFilterNew.optimallySized[T](
        size, false_positive_rate
      )

      bf += array
      (bf, array)
    })

    root = BloomFilterNew.optimallySized[T]( size, false_positive_rate )

    leaf.foreach( root &= _._1 )

    this
  }

  override def search(element:T):Iterable[Iterable[T]] = {
    if( root != null ) {
      val keys = root.genKyes(element)
      if ( root.containKeys(keys) ) {
        leaf.filter( x =>
          x._1.containKeys(keys)
        ).map( _._2 )
      } else  Iterable[Iterable[T]]()
    } else  Iterable[Iterable[T]]()
  }
}
