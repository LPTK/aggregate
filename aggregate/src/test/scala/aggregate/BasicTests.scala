package aggregate

import utest._

object BasicTests extends TestSuite{
  val tests = Tests {
    
    'iterators - {
      def ex0(xs: List[Int], ys: List[Int]) = {
        def res = for { x <- xs.iterator; y <- ys.iterator; if x < y } yield (x,y)
        assert(res.toList == 
          xs.iterator.flatMap(x => ys.iterator.withFilter(y => x < y).map(y => (x,y))).toList)
        res
      }
      assert(ex0(List(1,2,3),List(0,2,4)).toList == List((1,2), (1,4), (2,4), (3,4)))
    }
    
    import Aggregator._
    
    val myls = List(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
    
    'sumEvenIdx - {
      def sumEvenIdx = for {
        idx <- count.map(_ - 1)
        
        // doesn't work!!
        //c <- count
        //idx = c - 1
        //idx = {println(s"> $c");c - 1}
        
        s <- sum[Double] when (idx % 2 == 0)
      } yield s
      assert(sumEvenIdx(myls) == (for { (x,i) <- myls.zipWithIndex if i % 2 == 0 } yield x).sum)
      
    }
    
    'aggregators - {
      
      def average = for { s <- sum[Double]; c <- count } yield s/c
      
      val ls = List(1.0, 2.0, 3.0)
      assert(average(ls) == 2.0)
      
      def avgmax = for {
         avg <- average
         m   <- max[Double]
      } yield (avg,m)
      assert(avgmax(ls) == (2.0, Some(3.0)))
      
    }
    
    'primitive - {
      
      def count[A] = {
        lazy val cnt: Aggregator[A,Int] =
          for { x <- current[A] } yield x.fold(0)(_ => cnt.result()+1)
        cnt
      }
      assert(count(myls) == myls.size)
      
      def sum = {
        lazy val s: Aggregator[Double,Double] =
          for { x <- current[Double] } yield x.fold(0.0)(_ + s.result())
        s
      }
      assert(sum(myls) == myls.sum)
      
    }
    
  }
}
