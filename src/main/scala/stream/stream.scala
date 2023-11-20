package stream

import chisel3._
import chisel3.util._

import chisel3.util.experimental.InlineInstance

import common.utils._
import common.BundleUtils._

class Empty extends Bundle {
}

class Stream[+T <: Data](gen: T) extends Bundle {
  val valid = Output(Bool())
  val ready = Input(Bool())
  val data = Output(gen.cloneType)
}

class AXI4Stream(val WIDTH: Int) extends Bundle {
  require( WIDTH % 8 == 0 )
  val tvalid = Output(Bool())
  val tready = Input(Bool())
  val tdata = Output(UInt(WIDTH.W))
}

object AXI4Stream {
  def Buffer(width: Int, reverse: Boolean = false) = {
    val m_buffer = Module(new AXI4StreamBufferMod(width))
    if(reverse) {
      (m_buffer.m_axis, m_buffer.s_axis)
    } else {
      (m_buffer.s_axis, m_buffer.m_axis)
    }
  }

  def buf[T<:Data](tvalid: Bool, tready: Bool, tdata: T, reverse: Boolean = false) = {
    val (axis0, axis1) = Buffer(tdata.getWidth, reverse)
    axis0.tvalid <> tvalid
    axis0.tready <> tready
    if(reverse) {
      tdata := axis0.tdata.asTypeOf(tdata)
    } else {
      axis0.tdata := tdata.asUInt
    }
    axis1
  }
}

class AXI4StreamBufferMod(width: Int) extends Module with InlineInstance {
  val s_axis = IO(Flipped(new AXI4Stream(width)))
  val m_axis = IO(new AXI4Stream(width))
  s_axis <> m_axis
}

class Stage[T0 <: Data, T1 <: Data](gen0: T0, gen1: T1) extends Module {
  val s_enq = Stream.Input(gen0)
  val s_deq = Stream.Output(gen1)
}

object Stream {

  def Input[T <: Data](data: T) = {
    IO( Flipped (new Stream(data.cloneType) ) ).nodeq()
  }

  def VecInput[T <: Data](n: Int, data: T) = {
    IO(Flipped(Vec(n, new Stream(data.cloneType)))).nodeq()
  }

  def Output[T <: Data](data: T) = {
    IO(new Stream(data.cloneType)).noenq()
  }

  def VecOutput[T <: Data](n: Int, data: T) = {
    IO(Vec(n, new Stream(data.cloneType))).noenq()
  }

  def Buffer[T <: Data](data: T, reverse: Boolean = false) = {
    val buf = Module(new StreamBufferMod(data))
    buf.s_enq.noenq()
    buf.s_deq.nodeq()
    if(reverse) {
      (buf.s_deq, buf.s_enq)
    } else {
      (buf.s_enq, buf.s_deq)
    }
  }

  def buf[T <: Data](valid: Bool, ready: Bool, data: T, reverse: Boolean = false) = {
    val (enq, deq) = Buffer(chiselTypeOf(data), reverse)
    valid <> enq.valid
    enq.ready <> ready
    data <> enq.data
    deq
  }
  
  def rbuf[T <: Data](valid: Bool, ready: Bool, data: T) = {
    buf(valid, ready, data, true)
  }

  implicit class AddMethodsToStream[T <: Data](strm: Stream[T]) {

    def fire() = strm.valid && strm.ready

    def on_fire(block: T => Unit) = {
      when( strm.valid && strm.ready ) {
        block(strm.data)
      }
    }

    def enq(x: T) = {
      strm.valid := true.B
      strm.data := x
    }

    def on_enq(data:T)(block: => Any) = {
      strm.enq(data)
      when( fire() ) {
        block
      }
    }

    def deq() = {
      strm.ready := true.B
      strm.data
    }

    def on_deq(block: T => Any) = {
      val x = strm.deq()
      when( fire() ) {
        block(x)
      }
    }

    def noenq() = {
      strm.valid := false.B
      strm.data := DontCare
      strm
    }

    def nodeq() = {
      strm.ready := false.B
      strm
    }

    def buf() = {
      val (enq, deq) = Stream.Buffer(strm.data)
      enq <> strm
      deq
    }

    def axis() = {
      AXI4Stream.buf(strm.valid, strm.ready, strm.data)
    }

    def from_axis() = {
      AXI4Stream.buf(strm.valid, strm.ready, strm.data, true)
    }

    def transform[R <: Data](f: T => R) = {
      val s0 = strm.buf()
      Stream.buf(s0.valid, s0.ready, f(s0.data))
    }

    def stage[T0 <: Data, T1 <: Data](block: => Stage[T0, T1]) = {
      val m = Module(block)
      strm <> m.s_enq
      m.s_deq
    }

    def split(n: Int) = {
      val m = Module(new StreamSplitMod(n, chiselTypeOf(strm.data)))
      strm <> m.s_enq
      m.v_deq
    }

    def join[T1 <: Data, R <: Data](rhs: Stream[T1])(f: (T, T1) => R) = {
      val m = Module(new StreamJoinMod(chiselTypeOf(strm.data), chiselTypeOf(rhs.data))(f))
      strm <> m.s_enq_0
      rhs <> m.s_enq_1
      m.s_deq
    }

    def skid() = {
      strm.stage(new StreamSkidBuf(strm.data))
    }

    def skid3() = {
      strm.stage(new StreamSkid3Buf(strm.data))
    }

    def fifo(bits: Int) = {
      if( bits == 0 ) {
        strm
      } else {
        strm.stage(new StreamFIFO(bits, chiselTypeOf(strm.data)))
      }
    }

    def empty() = {
      strm.transform( x => Wire(new Empty()) )
    }

    def iterator[R <: Data](cond: T=>Bool)(advance: T=>(T, R)) = {
      val m_iterator = Module(new StreamIterator(chiselTypeOf(strm.data))(cond)(advance) )
      strm <> m_iterator.s_enq
      m_iterator.s_deq
    }

    def reduce[R <: Data](initial: => R)(done: R => Bool)(consume: (R, T)=>R) = {
      val m_reduce = Module(new StreamReduceMod(chiselTypeOf(strm.data))(initial)(done)(consume))
      m_reduce.s_enq <> strm
      m_reduce.s_deq
    }
  }

  implicit class AddMethodsToVecStream[T <: Data](vstrm: Vec[Stream[T]]) {
    def noenq() = {
      vstrm.foreach( _.noenq() )
      vstrm
    }
    def nodeq() = {
      vstrm.foreach( _.nodeq() )
      vstrm
    }
    def join() = {
      val m = Module(new StreamVecJoin(vstrm.length, vstrm(0).data))
      vstrm <> m.v_enq
      m.s_deq
    }
  }
}

object IOPIN {
  def apply[T<:Data](x: T) = {
    val io = IO( chiselTypeOf(x) )
    io <> x
    io
  }
}

object IOIN {
  def apply[T<:Data](x: T) = {
    val io = IO( Input(chiselTypeOf(x)) )
    io <> x
    io
  }
}

object IOOUT {
  def apply[T<:Data](x: T) = {
    val io = IO( Output(chiselTypeOf(x)) )
    io <> x
    io
  }
}

object IOInit {
  def apply[T<:Data](x: T) = {
    val io = IO( Output(chiselTypeOf(x)) )
    io := x
    io
  }
}

class StreamBufferMod[T <: Data](data: T) extends Stage(data, data) with InlineInstance {
  s_enq <> s_deq
}

class StreamSplitMod[T <: Data](n: Int, data: T) extends Module {
  val s_enq = Stream.Input(data.cloneType)
  val v_deq = Stream.VecOutput(n, data.cloneType)
  s_enq.ready := Cat(v_deq.map(_.ready)).andR
  for( (deq, i) <- v_deq.zipWithIndex ) {
    deq.data := s_enq.data
    deq.valid := s_enq.valid && Cat( v_deq.map(_.ready).all_but(i) ).andR
  }
}

class StreamVecJoin[T <: Data](n: Int, data: T) extends Module{
  val v_enq = Stream.VecInput(n, data.cloneType)
  val s_deq = Stream.Output(Vec(n, data.cloneType))
  for( (strm, i) <- v_enq.zipWithIndex) {
    strm.ready := s_deq.ready && Cat( v_enq.map(_.valid) ).andR
  }
  s_deq.valid := VecInit(v_enq.map( _.valid )).asUInt.andR
  s_deq.data := VecInit( v_enq.map( _.data ) )
}

class StreamJoinMod[T0 <: Data, T1 <: Data, R <: Data](data0: T0, data1: T1)(f: (T0, T1) => R) extends Module{
  val s_enq_0 = Stream.Input(data0.cloneType)
  val s_enq_1 = Stream.Input(data1.cloneType)

  val res = f(s_enq_0.data, s_enq_1.data)
  val s_deq = Stream.Output(chiselTypeOf(res))

  s_deq.valid := s_enq_0.valid && s_enq_1.valid
  s_deq.data := res

  s_enq_0.ready := s_deq.ready && s_enq_1.valid
  s_enq_1.ready := s_deq.ready && s_enq_0.valid
}

// http://fpgacpu.ca/fpga/Pipeline_Skid_Buffer.html
// https://zipcpu.com/blog/2019/05/22/skidbuffer.html
class StreamSkidBuf[T<:Data](gen: T) extends Stage(gen, gen) {
  
  val buf = Reg( gen.cloneType )
  val out = Reg( gen.cloneType )

  val s_empty = "b01".U(2.W)
  val s_busy = "b11".U(2.W)
  val s_full = "b10".U(2.W)

  val state = RegInit(s_empty)

  s_enq.ready := state(0)
  s_deq.valid := state(1)
  s_deq.data := Mux(state(1), out, DontCare)

  switch(state) {
    is(s_empty) {
      when( s_enq.valid ) {
        out := s_enq.data
        state := s_busy
      }
    }
    is(s_busy) {
      when( s_enq.valid && s_deq.ready ) {
        out := s_enq.data
      }.elsewhen( s_enq.valid && !s_deq.ready ) {
        buf := s_enq.data
        state := s_full
      }.elsewhen( s_deq.ready ) {
        state := s_empty
      }
    }
    is( s_full ) {
      when( s_deq.ready ) {
        out := buf
        state := s_busy
      }
    }
  }
}

// http://fpgacpu.ca/fpga/Pipeline_Skid_Buffer.html
// https://zipcpu.com/blog/2019/05/22/skidbuffer.html
class StreamSkid3Buf[T<:Data](gen: T) extends Stage(gen, gen) {
  
  val buf = Vec(3, gen.cloneType)

  val s_000 = "b010".U(4.W)
  val s_100 = "b110".U(4.W)
  val s_101 = "b111".U(4.W)
  val s_001 = "b011".U(4.W)
  val s_111 = "b101".U(4.W)

  val state = RegInit(s_000)

  s_enq.ready := state(1)
  s_deq.valid := state(0)
  s_deq.data := buf(2)

  when( s_enq.valid && state(1) ) {
    buf(0) := s_enq.data
  }

  switch(state) {
    is(s_000) {
      when( s_enq.valid ) {
        state := s_100
      }
    }
    is(s_100) {
      buf(2) := buf(0)
      when( s_enq.valid ) {
        state := s_101
      }.otherwise {
        state := s_001
      }
    }
    is(s_001) {
      when( s_enq.valid && s_deq.ready ) {
        state := s_100
      }.elsewhen(s_enq.valid ) {
        state := s_101
      }.elsewhen(s_deq.ready ) {
        state := s_100
      }
    }
    is(s_101) {
      when( s_enq.valid && s_deq.ready ) {
        buf(2) := buf(0)
      }.elsewhen(s_enq.valid ) {
        state := s_111
        buf(1) := buf(0)
      }.elsewhen(s_deq.ready ) {
        state := s_001
        buf(2) := buf(0)
      }
    }
    is(s_111) {
      when( s_deq.ready ) {
        state := s_101
        buf(2) := buf(1)
      }
    }
  }
}

class StreamIterator[T <: Data, R <: Data](gen: T)(cond: T => Bool)(advance: T => (T, R)) extends Module {
  
  val s_idle :: s_iterating :: Nil = Enum(2)
  val state = RegInit(s_idle)
  
  val s_enq = Stream.Input(gen)
  
  val task = Reg(gen.cloneType)
  val (next_task, cur_output) = advance(Mux(state===s_idle, s_enq.data, task))
  val cond_next = cond(next_task)
  
  val s_deq = Stream.Output(chiselTypeOf(cur_output))

  val cnd = cond(Mux(state===s_idle, s_enq.data, next_task))

  switch( state ) {

    is( s_idle ) {
      when( s_enq.valid ) {
        when( cond(s_enq.data ) ) {
          Stream.buf(s_enq.valid, s_enq.ready, cur_output) <> s_deq
          when( s_deq.ready && cond_next) {
            task := next_task
            state := s_iterating
          }
        }.otherwise {
          s_enq.ready := true.B
        }
      }
    }

    is( s_iterating ) {
      s_deq.on_enq(cur_output) {
        task := next_task
        when( !cond_next ) {
          state := s_idle
        }
      }
    }
  }

}

class StreamFIFO[T <: Data](val addr_width: Int, gen: T) extends Stage(gen, gen) {

  val ram = SyncReadMem(1 << addr_width, UInt(gen.getWidth.W))
  
  val head = RegInit(0.U((addr_width+1).W))
  val tail = RegInit(0.U((addr_width+1).W))

  val empty = head === tail
  val full = head === Cat(~tail(addr_width), tail(addr_width-1, 0))

  val read_addr = WireInit(tail)
  val read_data = ram.read(read_addr)

  when( !full )
  {
    s_enq.on_deq( data => {
      ram.write(head, data.asUInt)
      head := head + 1.U
    })
  }
  
  when( !empty ) {
    s_deq.on_enq(read_data.asTypeOf(gen)) {
      read_addr := tail + 1.U
      tail := read_addr
    }
  }
}

class StreamReduceMod[T<:Data, R<:Data](gen: T)(initial: =>R)(done: R => Bool)(reduce: (R, T) => R) extends Module {

  val init = initial

  val s_enq = Stream.Input( gen.cloneType )
  val s_deq = Stream.Output( chiselTypeOf(init) )

  val data = Reg(chiselTypeOf(init))
  val data_valid = RegInit(false.B)

  when( s_enq.valid ) {
    val r = reduce(Mux(data_valid, data, init), s_enq.data)
    when( done(r) ) {
      Stream.buf(s_enq.valid, s_enq.ready, r) <> s_deq
      s_deq.on_fire { x => {
        data_valid := false.B
      }}
    }.otherwise {
      s_enq.ready := true.B
      data := r
      data_valid := true.B
    }
  }
}
