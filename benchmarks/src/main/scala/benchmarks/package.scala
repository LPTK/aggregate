import uoption._

import scala.collection.mutable

package object benchmarks {
  
  implicit class OptionListOps[A](private val self: List[Option[A]]) {
    //def flattenOptions = self.flatten // TODO optim
    def flattenOptions = {
      val buff = new mutable.ListBuffer[A]
      var xs = self
      while (xs.nonEmpty) {
        val x = xs.head
        xs = xs.tail
        if (x.nonEmpty) buff += x.get
      }
      buff.result()
    }
  }
  implicit class UOptionListOps[A](private val self: List[UOption[A]]) {
    def flattenOptions = {
      val buff = new mutable.ListBuffer[A]
      var xs = self
      while (xs.nonEmpty) {
        val x = xs.head
        xs = xs.tail
        if (!x.isEmpty) buff += x.get
      }
      buff.result()
    }
  }
  
}
