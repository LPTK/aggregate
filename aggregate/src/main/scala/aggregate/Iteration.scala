package aggregate

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import collection.{Seq=>CSeq}

private[aggregate] trait ReifiedAggregator[+R] { outer =>
  type Source
  val src: Iterable[Source]
  val aggr: Aggregator[Source,R]
  def run = {
    //aggr ++= src
    //src.foreach(aggr.next)
    val it = src.iterator
    while (aggr.wantsNext && it.hasNext) aggr next it.next
    aggr.result
  }
  def map[S](f: R => S) = new ReifiedAggregator[S] {
    type Source = outer.Source
    val src: Iterable[Source] = outer.src
    val aggr: Aggregator[Source,S] = outer.aggr.map(f)
  }
  //def pipe[S](aggr2: Aggregator[R,S]) = new ReifiedAggregator[S] {
  //}
}
private[aggregate] case class ReifiedAggregatorImpl[S,+R](src: Iterable[S], aggr: Aggregator[S,R]) 
extends ReifiedAggregator[R] {
  type Source = S
}

abstract class Iteration[+A] { outerIteration =>
  def reify: ReifiedAggregator[A]
  def run = reify.run
  
  //def run: A = ???
  def iterate: Iterable[A] = ???
  
  
  def map[S](f: A => S): Iteration[S] = new Iteration[S] {
    def reify: ReifiedAggregator[S] = {
      val r = outerIteration.reify
      ReifiedAggregatorImpl(r.src, r.aggr.map(f))
    }
  }
  def flatMap[S](f: (=> A) => Iteration[S]): Iteration[S] = {
    new Iteration[S] {
      def reify: ReifiedAggregator[S] = {
        val r = outerIteration.reify
        val newAggr = f(r.aggr.result)
        val r2 = newAggr.reify
        val it = r.src.iterator
        val fil = r2.aggr.asLongAs(
          {//println(s"<${it.hasNext}>");
          it.hasNext && { r.aggr next it.next; r.aggr.wantsNext }})
        ReifiedAggregatorImpl(r2.src,fil)
      }
    }
  }
  
  //def collect[B](f: PartialFunction[A,B]): Iteration[B] = new Iteration[B] {
  //  def reify: ReifiedAggregatorImpl[_,B] = {
  //    val r = thisIteration.reify
  //    ReifiedAggregatorImpl(r.src, r.aggr.map(f))
  //  }
  //}
  
  // TODO – forbid?
  def withFilter(p: A => Boolean): Iteration[A] = ???
  
  def buffer: Iteration[CSeq[A]] = new Iteration[CSeq[A]] {
    //def reify = ReifiedAggregatorImpl[A,CSeq[A]](self, Aggregator.toBuffer[A])
    //def reify = outerIteration.reify.map()
    def reify: ReifiedAggregator[CSeq[A]] = {
      val ra = outerIteration.reify
      ReifiedAggregatorImpl[ra.Source,CSeq[A]](ra.src, ra.aggr.pipe(Aggregator.toBuffer))
    }
  }
  
  ////def flatten[B](implicit asTraversable: A => TraversableOnce[B]): Iteration[B] =
  //def flatten[B](implicit asTraversable: A => Iterable[B]): Iteration[B] =
  //  //map(_.flatten)
  //  map(a => asTraversable(a).flatten[B](asTraversable))
  
}
object Iteration {
  // TODO name `continually`?
  def apply[A](a: => A): Iteration[A] = new Iteration[A] {
    //def reify = ReifiedAggregatorImpl[A,A](Iterator.continually(()), Aggregator.continually(a))
    def reify = ReifiedAggregatorImpl[A,A](IterableWith(Iterator.continually(a)),
      Aggregator.current[A].map(_.getOrElse(a))) // could be more efficient
      //Aggregator.current[A].map(_.get)) // could be more efficient
  }
  implicit class OptionIterationOps[A](private val self: Iteration[Option[A]]) {
    def thenNone = new Iteration[Option[A]] {
      def reify: ReifiedAggregator[Option[A]] = {
        val ra = self.reify
        //ReifiedAggregatorImpl[ra.Source,Option[A]](ra.src, ra.aggr.pipe(Aggregator.))
        ???
      }
    }
  }
  implicit class IterableIterationOps[A](private val self: Iteration[Iterable[A]]) {
    //def flatten[B](implicit asTraversable: A => Iterable[B]): Iteration[B] =
    def flatten[B](implicit asTraversable: A => Iterable[B]): Iteration[Iterable[B]] =
      //self.flatMap(ita => ita.flatten[B])
      self.map(ita => ita.flatten[B])
  }
}

class IterableWith[A](mkIterator: => Iterator[A]) extends Iterable[A] {
  def iterator = mkIterator
}
object IterableWith { def apply[A](mkIterator: => Iterator[A]) = new IterableWith(mkIterator) }

trait RichIterable[+A] extends Iterable[A] { self =>
  
  // not satisfiable
  //def iter: Iteration[A] = ???
  
  def current: Iteration[Option[A]] = new Iteration[Option[A]] {
    def reify = ReifiedAggregatorImpl[A,Option[A]](self, Aggregator.current[A])
  }
  def collecting[B](pf: PartialFunction[A,B]): RichIterable[B] = new RichIterable[B] {
    def iterator = self.iterator.collect(pf)
  }
  // Q: useful at all??
  def thenNone: RichIterable[Option[A]] = new RichIterable[Option[A]] {
    def iterator = self.iterator.map(Some.apply) ++ Iterator.continually(Option.empty[A])
  }
  
}

object IterationOps {
  implicit class IterableOps[A](private val self: Iterable[A]) extends RichIterable[A] {
    def iterator = self.iterator
    
    //def thenNone: Iteration[Option[A]] = new Iteration[Option[A]] {
    //  def reify = ReifiedAggregatorImpl[A,Option[A]](self, Aggregator.thenNone[A])
    //}
    
    // can't be in RichIterable because of covariance
    def buffering: Iteration[mutable.Buffer[A]] = new Iteration[ArrayBuffer[A]] {
      def reify = ReifiedAggregatorImpl[A,ArrayBuffer[A]](self, Aggregator.toArrayBuffer[A])
    }
    
    
  }
}