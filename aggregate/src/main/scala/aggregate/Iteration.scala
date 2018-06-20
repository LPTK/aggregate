package aggregate

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

private[aggregate] trait ReifiedAggregator[+R] {
  type Source
  val src: Iterable[Source]
  val aggr: Aggregator[Source,R]
  def run = {
    //aggr ++= src
    src.foreach(aggr.next)
    aggr.result
  }
}
private[aggregate] case class ReifiedAggregatorImpl[S,+R](src: Iterable[S], aggr: Aggregator[S,R]) 
extends ReifiedAggregator[R] {
  type Source = S
}

abstract class Iteration[+A] { thisIteration =>
  def reify: ReifiedAggregator[A]
  def run = reify.run
  
  //def run: A = ???
  def iterate: Iterable[A] = ???
  
  
  def map[S](f: A => S): Iteration[S] = new Iteration[S] {
    def reify: ReifiedAggregatorImpl[_,S] = {
      val r = thisIteration.reify
      ReifiedAggregatorImpl(r.src, r.aggr.map(f))
    }
  }
  def flatMap[S](f: (=> A) => Iteration[S]): Iteration[S] = {
    new Iteration[S] {
      def reify: ReifiedAggregatorImpl[_,S] = {
        val r = thisIteration.reify
        val newAggr = f(r.aggr.result)
        val r2 = newAggr.reify
        val it = r.src.iterator
        val fil = r2.aggr.doFilter[r2.Source](x => it.hasNext && { r.aggr next it.next; true })
        ReifiedAggregatorImpl(r2.src,fil)
      }
    }
  }
  
}


object IterationOps {
  implicit class IterableOps[A](private val self: Iterable[A]) {
    
    def iter: Iteration[A] = ???
    def buffering: Iteration[mutable.Buffer[A]] = new Iteration[ArrayBuffer[A]] {
      def reify = ReifiedAggregatorImpl[A,ArrayBuffer[A]](self, Aggregator.toArrayBuffer[A])
    }
    def current: Iteration[Option[A]] = ???
    
  }
}