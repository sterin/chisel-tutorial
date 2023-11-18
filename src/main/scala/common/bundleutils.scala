package common

import chisel3._

object BundleUtils {

  implicit class AddMethodsToBundle[T<:Bundle](xx: T) {
    def apply(args: Data*) = {
      require( args.length == xx.elements.size )
      val ww = Wire(xx.cloneType)
      for( ((name, e), a) <- ww.elements.zip(args.reverse) ) {
        e := a
      }
      ww
    }
    def apply[T1 <: Bundle](default_value: T1)(ff: T=>(Data, Data)*) = {
      val ww = Wire(xx.cloneType)
      for( (n, e) <- default_value.elements ) {
        if( ww.elements.contains(n) ) {
          ww.elements(n) := e
        }
      }
      for( f <- ff ) {
        val (w, x) = f(ww)
        w := x
      }
      ww
    }
    def apply(ff0: T=>(Data, Data), ff: T=>(Data, Data)*) = {
      val ww = Wire(chiselTypeOf(xx))
      ww := xx
      for( f <- ff0 +: ff ) {
        val (w, x) = f(ww)
        w := x
      }
      ww
    }
  }
}
