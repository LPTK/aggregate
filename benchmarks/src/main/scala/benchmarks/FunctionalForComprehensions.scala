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
  
  lazy val xso = xs.map(Some.apply)
  lazy val yso = ys.map(Some.apply)
  lazy val zso = zs.map(Some.apply)
  
  def fresh(n: Int) = List((1 to n): _*)
  
  @Setup(Level.Trial)
  def initTrial(): Unit = {
    xs = fresh(size)
    ys = fresh(size / 2 max 2).map(-_)
    zs = fresh((size / 10) max 2)
  }
  
  @Benchmark
  def simple_baseline() : List[Int] = {
    val res = new mutable.ListBuffer[Int]
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
          res += x + y + z
        }
      }
    }
    res.result()
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
  def first_binding_baseline() : List[Int] = {
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
  def first_binding_for() : List[Int] = {
    for {
      x <- xs
      x0 = x + 1
      y <- ys
    } yield x0 + y
  }
  @Benchmark
  def first_binding_lazyfor() : List[Int] = {
    xs.map { x =>
      val x0 = x + 1
      ys.map { y =>
        x0 + y
      }
    }.flatten
  }
  @Benchmark
  def first_binding_lazyfused() : List[Int] = {
    xs.flatMap { x =>
      val x0 = x + 1
      ys.map { y =>
        x0 + y
      }
    }
  }
  
  @Benchmark
  def second_binding_for() : List[Int] = {
    for {
      x <- xs
      y <- ys
      x0 = x + 1
    } yield x0 + y
  }
  @Benchmark
  def second_binding_lazyfor() : List[Int] = {
    xs.map { x =>
      ys.map { y =>
        val x0 = x + 1
        x0 + y
      }
    }.flatten
  }
  @Benchmark
  def second_binding_lazyfused() : List[Int] = {
    xs.flatMap { x =>
      ys.map { y =>
        val x0 = x + 1
        x0 + y
      }
    }
  }
  
  @Benchmark
  def first_filter_for() : List[Int] = {
    for {
      x <- xs
      if x > 0
      y <- ys
    } yield x + y
  }
  @Benchmark
  def first_filter_lazyfor() : List[Int] = {
    xs.map { x =>
      if (x > 0) Some(
        ys.map { y =>
          x + y
        }
      ) else None
    }.flattenOptions.flatten
  }
  
  @Benchmark
  def second_filter_for() : List[Int] = {
    for {
      x <- xs
      y <- ys
      if x > 0
    } yield x + y
  }
  @Benchmark
  def second_filter_lazyfor() : List[Int] = {
    xs.map { x =>
      ys.map { y =>
        if (x > 0) Some(x + y) else None
      }.flattenOptions
    }.flatten
  }
  @Benchmark
  def second_filter_lazyfused() : List[Int] = {
    xs.flatMap { x =>
      ys.map { y =>
        if (x > 0) Some(x + y) else None
      }.flattenOptions
    }
  }
  
  @Benchmark
  def both_filters_for() : List[Int] = {
    for {
      x <- xs
      if x > 0
      y <- ys
      if y < 0
    } yield x + y
  }
  @Benchmark
  def both_filters_lazyfor() : List[Int] = {
    xs.map { x =>
      if (x > 0) Some(
        ys.map { y =>
          if (y < 0) Some(x + y)
          else None
        }.flattenOptions
      ) else None
    }.flattenOptions.flatten
  }
  
  @Benchmark
  def many_filters_for() : List[Int] = {
    for {
      x <- xs
      if x > 0
      if x % 2 == 0
      y <- ys
      if y < 0
      if y % 2 == 0
    } yield x + y
  }
  @Benchmark
  def many_filters_lazyfor() : List[Int] = {
    xs.map { x =>
      if (x > 0) if (x % 2 == 0) Some(
        ys.map { y =>
          if (y < 0) if (y % 2 == 0) Some(x + y)
          else None else None
        }.flattenOptions
      ) else None else None
    }.flattenOptions.flatten
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
          if (y0 < x0) Some(x0 + y0)
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
          if (y0 < x0) USome(x0 + y0)
          else UNone:UOption[Int]
        }.flattenOptions
      ) else UNone:UOption[List[Int]]
    }.flattenOptions.flatten
  }
  
  @Benchmark
  def filtering_binding_baseline() : List[Int] = {
    val res = new mutable.ListBuffer[Int]
    var xs = this.xs
    while (xs.nonEmpty) {
      val x = xs.head
      xs = xs.tail
      if (x > 0) {
        val x0 = x + 1
        var ys = this.ys
        while (ys.nonEmpty) {
          val y = ys.head
          ys = ys.tail
          if (y < x0) {
            val y0 = y + 1
            var zs = this.zs
            while (zs.nonEmpty) {
              val z = zs.head
              zs = zs.tail
              res += x + y + z
            }
          }
        }
      }
    }
    res.result()
  }
  @Benchmark
  def filtering_binding_for() : List[Int] = {
    for {
      x <- xs
      if x > 0
      x0 = x + 1
      y <- ys
      if y < x0
      y0 = y + 1
    } yield x0 + y0
  }
  @Benchmark
  def filtering_binding_lazyfor() : List[Int] = {
    xs.map { x =>
      if (x > 0) Some {
        val x0 = x + 1
        ys.map { y =>
          if (y < x0) Some {
            val y0 = y + 1
            x0 + y0
          }
          else None
        }.flattenOptions
      } else None
    }.flattenOptions.flatten
  }
  
  @Benchmark
  def filtering_binding_matching_baseline() : List[Int] = {
    val res = new mutable.ListBuffer[Int]
    var xs = this.xso
    while (xs.nonEmpty) {
      val Some(x) = xs.head
      xs = xs.tail
      if (x > 0) {
        val x0 = x + 1
        var ys = this.yso
        while (ys.nonEmpty) {
          val Some(y) = ys.head
          ys = ys.tail
          if (y < x0) {
            val y0 = y + 1
            var zs = this.zso
            while (zs.nonEmpty) {
              val Some(z) = zs.head
              zs = zs.tail
              res += x + y + z
            }
          }
        }
      }
    }
    res.result()
  }
  @Benchmark
  def filtering_binding_matching_for() : List[Int] = {
    for {
      Some(x) <- xso
      if x > 0
      x0 = x + 1
      Some(y) <- yso
      if y < x0
      y0 = y + 1
      Some(z) <- zso
    } yield x0 + y0 + z
  }
  @Benchmark
  def filtering_binding_matching_lazyfor() : List[Int] = {
    xso.map { case Some(x) =>
      if (x > 0) Some {
        val x0 = x + 1
        yso.map { case Some(y) =>
          if (y < x0) Some {
            val y0 = y + 1
            zso.map { case Some(z) =>
              x0 + y0 + z
            }
          }
          else None
        }.flattenOptions.flatten
      } else None
    }.flattenOptions.flatten
  }
  //@Benchmark
  //def filtering_binding_matching_lazyfused() : List[Int] = {}
  // ^ can't really fuse
  
}
