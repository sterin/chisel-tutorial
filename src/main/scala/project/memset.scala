package project.memset

import chisel3._
import chisel3.util._

import stream._
import axi._

import common.BundleUtils._
import project.datamover.{S2MM, Command}

object P {
  val MAX_WORDS = 1 << 16
  val COMMAND_FIFO_BITS = 8
  val CMD_DATA_FIFO = 4
  val CMD_CMD_FIFO = 4
  val CMD_STS_FIFO = 4
}

class CoreCommand extends Bundle {
  val addr = UInt(32.W)
  val words = UInt(32.W)
  val value = UInt(32.W)
}

class CoreStatus extends Bundle {
  val words = UInt(32.W)
  val last = Bool()
  val slverr = Bool()
  val decerr = Bool()
  val interr = Bool()
}

class Core extends Module {

  val s_enq_cmd = Stream.Input(new CoreCommand)
  val datamover = IO( new S2MM(32, 32) )

  val cmd_cmd :: cmd_data :: Nil = s_enq_cmd.fifo(P.COMMAND_FIFO_BITS).split(2).toList

  class CommandGen extends CoreCommand {
    val last = Bool()
  }

  // cmd_cmd
  val cmd_cmd_cmd :: cmd_cmd_sts :: Nil = cmd_cmd
    .skid()
    .iterator(_.words > 0.U)( cmd => (
      cmd(
        _.words -> Mux(cmd.words > P.MAX_WORDS.U, cmd.words - P.MAX_WORDS.U, 0.U),
        _.addr -> (cmd.addr + (P.MAX_WORDS << 2).U)
      ),
      (new CommandGen)(cmd)(
        _.last -> (cmd.words <= P.MAX_WORDS.U),
        _.words -> Mux(cmd.words > P.MAX_WORDS.U, P.MAX_WORDS.U, cmd.words)
      )
    ))
    .split(2).toList

  cmd_cmd_cmd
    .fifo(P.CMD_CMD_FIFO)
    .transform( cmd => Command(cmd.addr, cmd.words << 2) )
    .skid() <> datamover.cmd

  cmd_data
    .fifo(P.CMD_DATA_FIFO)
    .iterator(_.words > 0.U)( cmd => (
      cmd(_.words -> (cmd.words - 1.U)),
      cmd.value
    ))
    .skid() <> datamover.data

  class Sts extends Bundle {
    val words = UInt(32.W)
    val last = Bool()
  }

  val s_deq_sts = IOPIN( 
    cmd_cmd_sts
      .fifo(P.CMD_STS_FIFO)
      .join( datamover.sts.skid() ) { case (cmd, sts) =>
        (new CoreStatus)(cmd.words, cmd.last, sts.slverr, sts.decerr, sts.interr)
      }
  )
}

object Registers extends Enumeration {
  type Register = Value

  val addr = Value
  val value = Value
  val btt = Value

  val status = Value
  val bytes_written = Value

  val commands_completed = Value
  val commands_inflight = Value
}

class MemsetStream extends Module {

  val slave = Module(new BaseSlave(Registers.maxId))
  val s_axi_lite = IOPIN( slave.s_axi_lite )

  // configuration registers

  val addr = RegInit(0.U(32.W))
  val value = RegInit(0.U(32.W))
  val btt = RegInit(0.U(32.W))

  // configuration errors

  val addr_unaligned = addr(1, 0) =/= 0.U
  val btt_unaligned = btt(1, 0) =/= 0.U
  val btt_is_zero = btt === 0.U

  // error registers

  val submitted_when_not_ready = RegInit(false.B)
  val clear_when_busy = RegInit(false.B)
  val sts_slverr = RegInit(false.B)
  val sts_decerr = RegInit(false.B)
  val sts_interr = RegInit(false.B)

  // interrupt

  val interrupt = RegInit(false.B)
  val intr_ioc = IOOUT( interrupt )

  // status

  val bytes_written = RegInit(0.U(32.W))
  val commands_completed = RegInit(0.U(32.W))
  val commands_inflight = RegInit(0.U(32.W))

  val ready = Wire(Bool())
  val busy = commands_inflight =/= 0.U

  // register read/write

  slave.r_registers(Registers.addr.id) := addr
  slave.on_write(Registers.addr.id){ x => addr:=x }

  slave.r_registers(Registers.value.id) := value
  slave.on_write(Registers.value.id){ x => value:=x }

  slave.r_registers(Registers.btt.id) := btt
  slave.on_write(Registers.btt.id){ x => btt:=x }

  slave.r_registers(Registers.bytes_written.id) := bytes_written
  slave.r_registers(Registers.commands_completed.id) := commands_completed
  slave.r_registers(Registers.commands_inflight.id) := commands_inflight

  // status register

  slave.r_registers(Registers.status.id) := Cat(
    
    // memory errors
    sts_slverr,
    sts_decerr,
    sts_interr,
    
    // configuration errors
    0.U(1.W),
    btt_is_zero,
    btt_unaligned,
    addr_unaligned,

    // protocol errors
    0.U(2.W),
    clear_when_busy,
    submitted_when_not_ready,

    // status
    0.U(1.W),
    interrupt,
    busy,
    ready
  )

  slave.on_write(Registers.status.id)( x => {
    when( x(2) ) { 
      interrupt := false.B 
    }
    when( x.andR ) {
      when( busy ) {
        clear_when_busy := true.B
      }.otherwise {
        sts_slverr := false.B
        sts_decerr := false.B
        sts_interr := false.B
        clear_when_busy := false.B
        submitted_when_not_ready := false.B   
        commands_completed := 0.U
        bytes_written := 0.U
      }
    }
  })

  // core interface

  val core = Module(new Core)
  core.s_enq_cmd.noenq()
  ready := core.s_enq_cmd.ready

  val datamover = IOPIN( core.datamover )

  val command_submitted = WireInit( false.B )
  val command_retired = WireInit( false.B )

  val submitted_btt = WireInit(0.U(32.W))

  slave.on_write(Registers.btt.id)( btt => {
    submitted_btt := btt
    submitted_when_not_ready := !ready
    command_submitted := ready && !btt_unaligned && !addr_unaligned 
  })

  when( command_submitted ) {
    core.s_enq_cmd.enq( (new CoreCommand)(addr, submitted_btt >> 2, value) )
  }

  core.s_deq_sts.on_deq( sts => {
    bytes_written := bytes_written + (sts.words << 2)
    sts_slverr := sts_slverr || sts.slverr
    sts_decerr := sts_decerr || sts.decerr
    sts_interr := sts_interr || sts.interr
    when( sts.last ) {
      interrupt := true.B
      command_retired := true.B
      commands_completed := commands_completed + 1.U
    }
  })

  commands_inflight := commands_inflight + command_submitted.asUInt - command_retired.asUInt
}

class MemsetX extends Module {
  val memset = Module(new MemsetStream)
  val s_axi_lite = IOPIN( memset.s_axi_lite )
  val m_axis_datamover_cmd = IOPIN( memset.datamover.cmd.axis() )
  val m_axis_datamover_data = IOPIN( memset.datamover.data.axis() )
  val s_axis_datamover_sts = IOPIN( memset.datamover.sts.from_axis() )
  val intr_ioc = IOPIN( memset.intr_ioc )
}

object Emit {
  def main(args: Array[String]): Unit = {
    emit.emitVerilog.firrtl("Memset", new MemsetX)
    emit.emitVerilog("Memset", new MemsetX)
  }
}
