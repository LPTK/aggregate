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
    var xs = this.xs
    while (xs.nonEmpty) {
      val x = xs.head
      xs = xs.tail
      var ys = this.ys
      while (ys.nonEmpty) {
        val y = ys.head
        ys = ys.tail
        var zs = this.zs
        while (zs.nonEmpty) {
          val z = zs.head
          zs = zs.tail
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
    var xs = this.xs
    while (xs.nonEmpty) {
      val x = xs.head
      xs = xs.tail
      sum += x
      var ys = this.ys
      while (ys.nonEmpty) {
        val y = ys.head
        ys = ys.tail
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
  @Benchmark
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
