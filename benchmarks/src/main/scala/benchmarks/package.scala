package object benchmarks {
  
  implicit class OptionListOps[A](private val self: List[Option[A]]) {
    def flattenOptions = self.flatten // TODO optim
  }
  
}
