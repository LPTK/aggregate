package benchmarks

import org.openjdk.jmh.annotations._

@State(Scope.Benchmark)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@BenchmarkMode(Array(Mode.AverageTime))
//@State(Scope.Thread)
//@Fork(1)
class ImperativeForComprehensions {
  
  @Param(Array("4", "16", "128")) // "4096" is pretty slow
  var size: Int = _
  
  var xs: List[Int] = _
  var ys: List[Int] = _
  var zs: List[Int] = _
  
  def fresh(n: Int) = List((1 to n): _*)
  
  @Setup(Level.Trial)
  def initTrial(): Unit = {
    xs = fresh(size)
    ys = fresh(size / 2 max 2).map(-_)
    zs = fresh((size / 10) max 2)
  }
  
  @Benchmark
  def simple_baseline() : Int = {
    var sum = 0
    val xi = xs.iterator
    while (xi.hasNext) {
      val x = xi.next
      val yi = ys.iterator
      while (yi.hasNext) {
        val y = yi.next
        val zi = zs.iterator
        while (zi.hasNext) {
          val z = zi.next
          sum += x + y + z
        }
      }
    }
    sum
  }
  @Benchmark
  def simple_for() : Int = {
    var sum = 0
    for {
      x <- xs
      y <- ys
      z <- zs
    } sum += x + y + z
    sum
  }
  @Benchmark
  def simple_lazyfor() : Int = {
    var sum = 0
    xs.foreach { x =>
      ys.foreach { y =>
        zs.foreach { z =>
          sum += x + y + z
        }
      }
    }
    sum
  }
  
  @Benchmark
  def one_binding_baseline() : Int = {
    var sum = 0
    val xi = xs.iterator
    while (xi.hasNext) {
      val x = xi.next
      sum += x
      val yi = ys.iterator
      while (yi.hasNext) {
        val y = yi.next
        sum += x + y
      }
    }
    sum
  }
  @Benchmark
  def one_binding_for() : Int = {
    var sum = 0
    for {
      x <- xs
      () = sum += x
      y <- ys
    } sum += x + y
    sum
  }
  @Benchmark // this one seems faster than baseline! probably because it doesn't use an iterator
  def one_binding_lazyfor() : Int = {
    var sum = 0
    xs.foreach { x =>
      val () = sum += x
      ys.foreach { y =>
          sum += x + y
      }
    }
    sum
  }
  
  
}
