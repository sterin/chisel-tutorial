package common

object utils {

  implicit class AddMethodsToSeq[T](s: Seq[T]) {
    def all_but(i: Int) = {
      s.zipWithIndex.filter{ case (x, j) => i != j }.map(_._1)
    }
  }

  implicit class AddMethodsInt(x: Int) {
  
    // round up integer division
    def /+(d: Int) : Int = {
      (x + d - 1) / d
    }

    def next_multiple(d: Int) = {
      (x /+ d) * d
    }
  }
}
