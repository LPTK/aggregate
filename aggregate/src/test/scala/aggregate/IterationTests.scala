package aggregate

import utest._
import IterationOps._

object IterationTests extends TestSuite {
  val tests = Tests {
    
    val ls0 = {
      var i = 0
      List.fill(20){i+=1;i}
    }
    val ls1 = {
      var i = 0.0
      List.fill(10){i+=0.5;i}
    }
    
    'buffers - {
      
      val res = for {
        a <- ls0.buffering//[Int]
        b <- ls1.buffering//[Double]//.filter(_.isWhole)
        //c <- ls.count
      } yield (a,b)
      
      println(res.run)
      
    }
    
    'current - {
      
      val res0 = for {
        a <- ls0.current
        b <- ls1.current
      } yield (a,b)
      println(res0.run)
      
      val res1 = for {
        a <- ls0.buffering
        b <- ls1.thenNone.buffering
        //b <- Iteration(print("Ite!"))
        //res <- Iteration()
        _ <- Iteration(print(s"Ite[${a.size}] "))
      } yield (a,b)
      println(res1.run)
      // Q: why repeated: Ite[0] Ite[0] Ite[1] Ite[2]...
      
      val res2 = for {
        //Some(a) <- ls0.current
        a <- ls0.current
        //b <- ls1.current
        //b <- ls1.thenNone.current
        b <- ls1.current.thenNone
        //b <- ls1.thenNone.flattening.current
        _ <- Iteration(print(s"[$a,$b] "))
        res <- Iteration(b.orElse(a)).buffer.flatten
      } yield res
      println
      println({val res = res2.run; println; res})
      
      
      
    }
    
  }
}
