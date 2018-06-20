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
    
    'basics - {
      
      val res = for {
        a <- ls0.buffering//[Int]
        b <- ls1.buffering//[Double]//.filter(_.isWhole)
        //c <- ls.count
      } yield (a,b)
      
      println(res.run)
      
    }
    
  }
}
