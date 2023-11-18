package emit

import chisel3._
import chisel3.util._
import chisel3.reflect.DataMirror

class ActiveLowResetWrapper(name: String, block: => RawModule) extends RawModule {

  val clock = IO(Input(Clock()))
  val resetn = IO(Input(Bool()))

  withClockAndReset( clock, ~resetn ) {
    val m = Module(block)
    for((name, p) <- DataMirror.modulePorts(m) ) {
      if( name != "reset" && name != "clock") {
        val port = IO( chiselTypeOf(p) )
        port.suggestName(name)
        port <> p
      }
    }
  }

  override val desiredName = name
}

object ActiveLowResetWrapper {
  def apply(name: String)(block: => RawModule) = {
    new ActiveLowResetWrapper(name, block)
  }
}
