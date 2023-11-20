package axi

import chisel3._
import chiseltest._

import common.BaseSpec

import LiteTestHelpers._

class LiteSpec extends BaseSpec {
  
  "Read and write should work" in {
    verilator_test(new RegFileX(8)) { 
      implicit dut => {
        fork {
          dut.s_axi_lite.write(for(i<-0 to 7) yield (i*4).U, for(i<-0 to 7) yield i.U)
        }.fork {
          dut.clock.step(2)
          dut.s_axi_lite.read(for(i <- 0 to 7) yield (i*4).U).zipWithIndex.foreach( x => {
            println(f"reg ${x._2} = ${x._1.litValue}")
          })
        }.join()
      }
    }
  }
}
