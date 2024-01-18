package project.memset

import chisel3._
import chiseltest._

import chisel3.experimental.BundleLiterals._

import common.BaseSpec
import common.utils._

import axi.LiteTestHelpers._
import stream.StreamTestHelpers._

class MemsetSpec extends BaseSpec {
  
  "xxx" in {
    verilator_test(new MemsetStream) { 
      implicit dut => {
        
        val n_words = 1024
        val max_words = 8
        val dm_cmds = n_words /+ max_words
        val values = Seq(0xAAAA_AAAAL.U)
        val n_commands = 1

        fork {
          for(value <- values) {
            dut.s_axi_lite.write((Registers.addr.id*4).U, 0x2000_0000L.U)
            dut.s_axi_lite.write((Registers.value.id*4).U, value)
            dut.s_axi_lite.write((Registers.btt.id*4).U, (n_words << 2).U)
          }
        }.fork {
          for(i<-1 to n_commands*dm_cmds*values.length) {
            println(f"CMD: ${dut.datamover.cmd.deq()}")
          }
        }.fork {
          for(i<-1 to n_words*values.length) {
            println(f"DATA: ${dut.datamover.data.deq()}")
          }
        }.fork {
          for(i<-1 to values.length*dm_cmds) {
            dut.datamover.sts.enq( (chiselTypeOf(dut.datamover.sts.data).Lit(
              _.okay -> true.B,
              _.slverr -> false.B,
              _.decerr -> false.B,
              _.interr -> false.B,
              _.tag -> 0.U
            )))
          }
        }.join()

        dut.clock.step(100)
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
