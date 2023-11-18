package axi

import chisel3._
import chiseltest._

object LiteTestHelpers {

  implicit class LiteTester(axil: Lite) {

     def init() = {
      axil.arvalid.poke(false.B)
      axil.rready.poke(false.B)
      axil.awvalid.poke(false.B)
      axil.wvalid.poke(false.B)
      axil.bready.poke(false.B)
    }

    def wait_for_arready()(implicit dut: Module) = {
      while( axil.arready.peek().litToBoolean == false ) {
        dut.clock.step(1)
      }
    }

    def wait_for_rvalid()(implicit dut: Module) = {
      while( axil.rvalid.peek().litToBoolean == false ) {
        dut.clock.step(1)
      }
    }

    def read(addrs: Seq[UInt])(implicit dut: Module): Seq[UInt] = {
      
      var data = Seq[UInt]()

      fork {
        addrs.foreach( addr => {
          axil.arvalid.poke(true.B)
          axil.araddr.poke(addr)

          axil.wait_for_arready()
          dut.clock.step(1)
          axil.arvalid.poke(false.B)
        })
      }.fork {
        addrs.foreach( addr => {
          axil.rready.poke(true.B)
          axil.wait_for_rvalid()

          data = data ++ Seq(axil.rdata.peek())
          dut.clock.step(1)
          axil.rready.poke(false.B)
        })
      }.join()

      data
    }

    def read(addr: UInt)(implicit dut: Module): UInt = {
      val res = read(Seq(addr))
      res(0)
    }

    def wait_for_awready()(implicit dut: Module) = {
      while( axil.awready.peek().litToBoolean == false ) {
        dut.clock.step(1)
      }
    }

    def wait_for_wready()(implicit dut: Module) = {
      while( axil.wready.peek().litToBoolean == false ) {
        dut.clock.step(1)
      }
    }

    def wait_for_bvalid()(implicit dut: Module) = {
      while( axil.bvalid.peek().litToBoolean == false ) {
        dut.clock.step(1)
      }
    }

    def write(addrs: Seq[UInt], data: Seq[UInt])(implicit dut: Module) = {

      require( addrs.length == data.length )

      fork {
        addrs.foreach( addr => {
          axil.awvalid.poke(true.B)
          axil.awaddr.poke(addr)
          axil.wait_for_awready()
          dut.clock.step(1)
          axil.awvalid.poke(false.B)
        })
      }.fork {
        data.foreach( x => {
          axil.wvalid.poke(true.B)
          axil.wdata.poke(x)
          axil.wait_for_wready()
          dut.clock.step(1)
          axil.wvalid.poke(false.B)
        })
      }.fork {
        addrs.foreach( x => {
          axil.bready.poke(true.B)
          axil.wait_for_bvalid()
          dut.clock.step(1)
          axil.bready.poke(false.B)
        })
      }.join()
    }

    def write(addr: UInt, data: UInt)(implicit dut: Module) : Unit = {
      write(Seq(addr), Seq(data))
    }
  }
}
