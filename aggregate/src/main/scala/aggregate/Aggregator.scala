package aggregate

import scala.annotation.compileTimeOnly
import scala.annotation.unchecked.uncheckedVariance
import scala.collection.mutable.{ListBuffer, Buffer}

trait Aggregator[-A, +To] { outer =>
  def wantsNext(): Boolean
  def next(a: A): Unit
  def result(): To
  
  /* This implementation accumulates changes eagerly into a local variable, and make it possible to define aggregators
   * recursively, for example based on the primitive `current` aggregator. */
  def map[R](f: To => R): Aggregator[A, R] = new Aggregator[A, R] {
    var result: R = f(outer.result) // Q: legit?!
    def wantsNext: Boolean = outer.wantsNext
    def next(x: A): Unit = { assert(wantsNext); outer next x; result = f(outer.result)  }
  }
  /* This is another possible implementation of map, more lazy but which prevents the use case described above. */
  def map2[R](f: (=>To) => R): Aggregator[A, R] = new Aggregator[A, R] {
    def result: R = f(outer.result)
    def wantsNext: Boolean = outer.wantsNext
    def next(x: A): Unit = { assert(wantsNext); outer next x }
  }
  
  def flatMap[A0<:A,R](f: (=> To) => Aggregator[A0,R]): Aggregator[A0,R] = new Aggregator[A0,R] {
    val inner = f(outer.result())
    def wantsNext: Boolean = inner.wantsNext || outer.wantsNext
    def result(): R = inner.result()
    def next(elem: A0): Unit = {
      assert(wantsNext)
      if (outer.wantsNext) outer.next(elem); if (inner.wantsNext) inner.next(elem) }
  }
  
  def when(cond: => Boolean) = new Aggregator[A, To] {
    def wantsNext() = outer.wantsNext()
    def result() = outer.result()
    def next(x: A): Unit = if (cond) outer.next(x)
  }
  
  def apply(xs: Iterable[A]) = {
    val it = xs.iterator
    while (wantsNext && it.hasNext) next(it.next)
    result
  }
  
  // Having this method allowed is too confusing (can evaluate too eagerly!):
  @compileTimeOnly("`if` in for comprehension is not supported by Aggregator; use `when` conditions instead")
  def withFilter(pred: To => Boolean): Aggregator[A, To] = ???
  
  //def withFilter(pred: To => Boolean) = new Aggregator[A, To] {
  //  def wantsNext() = outer.wantsNext() && pred(outer.result())
  //  def result() = outer.result()
  //  def next(x: A): Unit = if (pred(outer.result())) outer.next(x)
  //}
  /* ^ sometimes evaluates the condition too early in for comprehensions */
  
  //def withFilter(pred: (A@uncheckedVariance) => Boolean) = new Aggregator[A, To] {
  //  def wantsNext() = outer.wantsNext()
  //  def result() = outer.result()
  //  def next(x: A): Unit = if (pred(x)) outer.next(x)
  //}
  /* ^ makes error in:
        ls <- toList[Double]
        if ls.size < 3  // value size is not a member of Double */
  
}
object Aggregator {
  
  //def sum[N](implicit N: scala.Numeric[N]): Aggregator[N,N] = new Aggregator[N,N] {
  //  def wantsNext: Boolean = true
  //  var a = N.zero
  //  def next(x: N): Unit = a = N.plus(a,x)
  //  def result = a
  //}
  case class sum[N:Numeric]() extends Aggregator[N,N] {
    def wantsNext: Boolean = true
    var result = implicitly[Numeric[N]].zero
    def next(a: N): Unit = result = implicitly[Numeric[N]].plus(result,a)
  }
  
  def count: Aggregator[Any,Int] = new Aggregator[Any,Int] {
    def wantsNext: Boolean = true
    var result = 0
    def next(x: Any): Unit = result += 1
  }
  def current[A]: Aggregator[A,Option[A]] = new Aggregator[A,Option[A]] {
    def wantsNext: Boolean = true
    var result = Option.empty[A]
    def next(x: A): Unit = result = Some(x)
  }
  def max[N](implicit N: scala.Numeric[N]): Aggregator[N,Option[N]] = new Aggregator[N,Option[N]] {
    def wantsNext: Boolean = true
    var a = Option.empty[N]
    def next(x: N): Unit = a = Some(a.fold(x)(a => if (N.compare(x,a) > 0) x else a))
    def result = a
  }
  
  def simpleInstance[A,R](z: R)(c: (R,A) => R): Aggregator[A,R] = new Aggregator[A,R] {
    def wantsNext: Boolean = true
    var cur = z
    def next(x: A): Unit = cur = c(cur,x)
    def result = cur
  }
  def statefulInstance[A,S,R](z: S)(c: (S,A) => Unit)(get: S => R): Aggregator[A,R] = new Aggregator[A,R] {
    def wantsNext: Boolean = true
    val cur = z
    def next(x: A): Unit = c(cur,x)
    def result = get(cur)
  }
  
  def toRevList[A] = simpleInstance[A,List[A]](Nil)(_.::(_))
  def toList[A] = statefulInstance[A,ListBuffer[A],List[A]](ListBuffer.empty)(_ += _)(_.toList)
  def toBuffer[A] = simpleInstance[A,Buffer[A]](Buffer.empty)(_ += _)
  
}
