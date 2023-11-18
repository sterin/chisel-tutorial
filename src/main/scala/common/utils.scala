package common

object utils {
  implicit class AddMethodsToSeq[T](s: Seq[T]) {
    def all_but(i: Int) = {
      s.zipWithIndex.filter{ case (x, j) => i != j }.map(_._1)
    }
  }
}
