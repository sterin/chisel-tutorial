package axi

import chisel3._
import chisel3.util._

import stream._
import common.BundleUtils._

class Lite(val ADDR_WIDTH: Int) extends Bundle {

  require( ADDR_WIDTH > 0 )

  // read address channel
  val arvalid = Input(Bool())
  val arready = Output(Bool())
  val araddr = Input(UInt(ADDR_WIDTH.W))

  // read data channel
  val rvalid = Output(Bool())
  val rready = Input(Bool())
  val rdata = Output(UInt(32.W))
  val rresp = Output(UInt(2.W))

  // write address channel
  val awvalid = Input(Bool())
  val awready = Output(Bool())
  val awaddr = Input(UInt(ADDR_WIDTH.W))

  // write data channel
  val wvalid = Input(Bool())
  val wready = Output(Bool())
  val wdata = Input(UInt(32.W))

  // write response channel
  val bvalid = Output(Bool())
  val bready = Input(Bool())
}

object Lite {

  def Input(addr_width: Int) = {
    IO( new Lite(addr_width) ).init()
  }

  implicit class AddMethodsToLite(axil: Lite) {

    def init() = {
      axil.arready := false.B
      axil.rvalid := false.B
      axil.rdata := DontCare
      axil.rresp := 0.U
      axil.awready := false.B
      axil.wready := false.B
      axil.bvalid := false.B
      axil
    }

    def ar_stream() = Stream.buf(axil.arvalid, axil.arready, axil.araddr)
    def r_stream() = Stream.rbuf(axil.rvalid, axil.rready, axil.rdata)
    def aw_stream() = Stream.buf(axil.awvalid, axil.awready, axil.awaddr)
    def w_stream() = Stream.buf(axil.wvalid, axil.wready, axil.wdata)
    def b_stream() = Stream.rbuf(axil.bvalid, axil.bready, Wire(new Empty()))

  }
}

class WriteBundle(width: Int) extends Bundle {
  val addr = UInt(width.W)
  val data = UInt(32.W)
}

class BaseSlave(n: Int) extends Module {

  val s_axi_lite = Lite.Input(log2Ceil(n)+2)
  
  val r_registers = IO(Input(Vec(n, UInt(32.W))))
  val w_registers = IO(Vec(n, Valid(UInt(32.W))))
  
  w_registers.foreach( r => {
    r.valid := false.B
    r.bits := DontCare
  })

  s_axi_lite.ar_stream()
    .transform( _ >> 2 )
    .skid()
    .transform( x => r_registers(x) )
    .skid() <> s_axi_lite.r_stream()

  val write = s_axi_lite.aw_stream()
    .transform( _ >> 2 )
    .skid()
    .join( s_axi_lite.w_stream().skid() ) { case (aw, w) => {
      (new WriteBundle(log2Ceil(n)))(aw, w)
    }}

  write
    .empty()
    .skid() <> s_axi_lite.b_stream()

  write.on_fire( ww => {
    w_registers(ww.addr).valid := true.B
    w_registers(ww.addr).bits := ww.data
  })

  def on_write(n: Int)(f: UInt => Unit) = {
    when( w_registers(n).valid ) {
      f( w_registers(n).bits )
    }
  }
}

class RegFileX(n: Int) extends Module {

  require( n > 0 )

  val slave = Module(new BaseSlave(n))
  val s_axi_lite = IOPIN(slave.s_axi_lite)

  val regs = RegInit(VecInit.fill(n)(0.U(32.W)))
  slave.r_registers := regs

  for(i<-0 to n-1) {
    slave.on_write(i){ x => {
      regs(i) := x
    }}
  }

  val intr = IOOUT( regs(0)(0) )
}

object Emit {
  def main(args: Array[String]): Unit = {
    emit.emitVerilog("RegFile", new RegFileX(7))
  }
}
