package benchmarks

import org.openjdk.jmh.annotations._
import uoption._

import scala.collection.mutable

@State(Scope.Benchmark)
class FunctionalForComprehensions {
  
  @Param(Array("4", "16", "128"))
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
  def simple_baseline() : List[Int] = {
    var res: List[Int] = Nil // TODO use ListBuffer?
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
          res ::= x + y + z
        }
      }
    }
    res.reverse
  }
  @Benchmark
  def simple_for() : List[Int] = {
    for {
      x <- xs
      y <- ys
      z <- zs
    } yield x + y + z
  }
  @Benchmark
  def simple_lazyfor() : List[Int] = {
    xs.map { x =>
      ys.map { y =>
        zs.map { z =>
          x + y + z
        }
      }.flatten
    }.flatten
  }
  @Benchmark
  def simple_lazyfused() : List[Int] = {
    xs.flatMap { x =>
      ys.flatMap { y =>
        zs.map { z =>
          x + y + z
        }
      }
    }
  }
  
  @Benchmark
  def one_binding_baseline() : List[Int] = {
    //val res = mutable.ListBuffer.newBuilder[Int]
    val res = new mutable.ListBuffer[Int]
    var xs = this.xs
    while (xs.nonEmpty) {
      val x = xs.head
      xs = xs.tail
      val x0 = x + 1
      var ys = this.ys
      while (ys.nonEmpty) {
        val y = ys.head
        ys = ys.tail
        res += x0 + y
      }
    }
    res.result()
  }
  @Benchmark
  def one_binding_for() : List[Int] = {
    for {
      x <- xs
      x0 = x + 1
      y <- ys
    } yield x0 + y
  }
  @Benchmark
  def one_binding_lazyfor() : List[Int] = {
    xs.map { x =>
      val x0 = x + 1
      ys.map { y =>
        x0 + y
      }
    }.flatten
  }
  @Benchmark
  def one_binding_lazyfused() : List[Int] = {
    xs.flatMap { x =>
      val x0 = x + 1
      ys.map { y =>
        x0 + y
      }
    }
  }
  
  @Benchmark
  def binding_filtering_for() : List[Int] = {
    for {
      x <- xs
      x0 = x + 1
      if x0 > 0
      y <- ys
      y0 = y + 1
      if y0 < x0
    } yield x0 + y0
  }
  @Benchmark
  def binding_filtering_lazyfor() : List[Int] = {
    xs.map { x =>
      val x0 = x + 1
      if (x0 > 0) Some(
        ys.map { y =>
          val y0 = y + 1
          if (y0 < x0) Some(x0 + y)
          else None
        }.flattenOptions
      ) else None
    }.flattenOptions.flatten
  }
  @Benchmark
  def binding_filtering_lazyunboxed() : List[Int] = {
    xs.map { x =>
      val x0 = x + 1
      if (x0 > 0) USome(
        ys.map { y =>
          val y0 = y + 1
          if (y0 < x0) USome(x0 + y)
          else UNone:UOption[Int]
        }.flattenOptions
      ) else UNone:UOption[List[Int]]
    }.flattenOptions.flatten
  }
  
}
