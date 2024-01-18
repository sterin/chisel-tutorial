package stream

import chisel3._
import chiseltest._

import common.BaseSpec
import common.utils._

import stream.StreamTestHelpers._

class SkidSpec extends BaseSpec {
  
  "xxx" in {
    verilator_test(new StreamSkid3Buf(UInt(8.W))) { 
      implicit dut => {
        dut.s_enq.valid.poke(true)
        dut.s_enq.data.poke(0.U)

        dut.clock.step(1)

        dut.s_enq.valid.poke(true)
        dut.s_enq.data.poke(1.U)
      }
    }
  }
}


class CoreSpec extends BaseSpec {
  
  "xxx" in {
    verilator_test(new Core) { 
      implicit dut => {
        
        val n_words = 1024
        val n_commands = 3

        fork {
          dut.s_enq_cmd.enq( (chiselTypeOf(dut.s_enq_cmd.data).Lit(
            _.addr -> 0x2000_000.U,
            _.words -> n_words.U,
            _.value -> 0x1234_5678L.U
          )))
        }.fork {
          for(i<-1 to n_commands) {
            println(f"CMD: ${dut.datamover.cmd.deq()}")
          }
        }.fork {
          for(i<-0 to n_words-1) {
            println(f"DATA: ${dut.datamover.data.deq()}")
          }
        }.fork {
          for(i<-1 to n_commands) {
            dut.datamover.sts.enq( (chiselTypeOf(dut.datamover.sts.data).Lit(
              _.okay -> true.B,
              _.slverr -> false.B,
              _.decerr -> false.B,
              _.interr -> false.B,
              _.tag -> 0.U
            )))
          }
        }.fork {
          print(dut.s_deq_sts.deq())
        }.join()

        dut.clock.step(100)
      }
    }
  }
}
