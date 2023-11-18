package stream

import chisel3._
import chisel3.util._
import chiseltest._
import chisel3.reflect.DataMirror
import chisel3.experimental.Direction

object StreamTestHelpers {

  implicit class VecStreamTester[T <: Data](vstrm: Vec[Stream[T]]) {
    def init() = {
      vstrm.foreach( _.init() )
    }
  }

  implicit class StreamTester[T <: Data](strm: Stream[T]) {

    def is_slave() = {
      DataMirror.directionOf(strm.valid) == Direction.Input
    }

    def init() = {
      if( is_slave() ) {
        strm.valid.poke(false.B)
      } else {
        strm.ready.poke(false.B)
      }
    }

    def wait_for_ready()(implicit dut: Module) = {
      require( is_slave() )
      while( strm.ready.peek().litToBoolean == false ) {
        dut.clock.step(1)
      }
    }

    def enq(gen: T)(implicit dut: Module) = {
      require( is_slave() )

      strm.valid.poke(true.B)
      strm.data.poke(gen)

      strm.wait_for_ready()
      dut.clock.step(1)

      strm.valid.poke(false.B)
    }

    def stalled()(implicit dut: Module) = {
      strm.valid.peek().litToBoolean && strm.ready.peek().litToBoolean
    }

    def peek_valid()(implicit dut: Module) = {
      require( !is_slave() )
      strm.valid.peek().litToBoolean
    }

    def deq()(implicit dut: Module) = {
      require( !is_slave() )
      strm.ready.poke(true.B)

      while( strm.valid.peek().litToBoolean == false ) {
        dut.clock.step(1)
      }

      val data = strm.data.peek()
      dut.clock.step(1)

      strm.ready.poke(false.B)
      data
    }

    def require_not_available(n: Int)(implicit dut: Module) = {
      require( !is_slave() )
      strm.ready.poke(true.B)

      for(i<-1 to n) {
        require( !strm.valid.peek().litToBoolean );
        dut.clock.step(1)
      }
    }
  }

}
