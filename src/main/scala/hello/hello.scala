package hello

import chisel3._
import chisel3.util._

class Hello1 extends Module {
  val x = IO(Input( Bool() ))
  val z = IO(Output( UInt(32.W) )) 
  z := x
}

class Hello2 extends Module {
  val x = IO(Input( UInt(32.W) ))
  val y = IO(Input( UInt(32.W) ))
  val z = IO(Output( UInt(32.W) )) 
  z := x+y
}

object Emit {
  def main(args: Array[String]): Unit = {
    emit.emitVerilog(new Hello1)
    emit.emitVerilog(new Hello2)
    // stage.emitVerilog(new Hello3)
    // stage.emitVerilog(new Hello4)
    // stage.emitVerilog(new Hello5)
  }
}
