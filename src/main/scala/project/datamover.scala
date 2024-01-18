package project.datamover

import chisel3._
import chisel3.util._

import stream._

class Command(val ADDR_WIDTH: Int = 32) extends Bundle {
  val rsvd = UInt(4.W)
  val tag = UInt(4.W)
  val saddr = UInt(ADDR_WIDTH.W)
  val drr = Bool()
  val eof = Bool()
  val dsa = UInt(6.W)
  val incr = Bool()
  val btt = UInt(23.W)
}

object Command {
  def apply(saddr: UInt, btt: UInt, incr: Bool = true.B) = {
    val w = Wire(new Command(saddr.getWidth))
    w := 0.U.asTypeOf(w)
    w.incr := incr
    w.saddr := saddr
    w.btt := btt
    w
  }
}

class Status extends Bundle {
  val okay = Bool()
  val slverr = Bool()
  val decerr = Bool()
  val interr = Bool()
  val tag = UInt(4.W)
}

class S2MM(val ADDR_WIDTH: Int, val DATA_WIDTH: Int) extends Bundle {
  val cmd = new Stream(new Command(ADDR_WIDTH))
  val sts = Flipped(new Stream(new Status()))
  val data = new Stream(UInt(DATA_WIDTH.W))
}

class MM2S(val ADDR_WIDTH: Int, val DATA_WIDTH: Int) extends Bundle {
  val cmd = new Stream(new Command(ADDR_WIDTH))
  val sts = Flipped(new Stream(new Status()))
  val data = Flipped(new Stream(UInt(DATA_WIDTH.W)))
}
