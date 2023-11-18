package common

import org.scalatest.freespec.AnyFreeSpec

import chisel3._
import chisel3.reflect.DataMirror

import chiseltest._

import chiseltest.simulator.VerilatorFlags
import chiseltest.ChiselScalatestTester

import firrtl.transforms.NoDCEAnnotation

import stream._
import axi._

import StreamTestHelpers._
import LiteTestHelpers._

abstract class BaseSpec extends AnyFreeSpec with ChiselScalatestTester {

  def init[M <: Module](testFn: M => Unit) = {
    dut:M => {
      for((name, p) <- DataMirror.modulePorts(dut) ) {
        if( p.isInstanceOf[Stream[_<:Data]] ) {
          p.asInstanceOf[Stream[_<:Data]].init()
        }
        else if( p.isInstanceOf[Lite] ) {
          p.asInstanceOf[Lite].init()
        }
      }
      testFn(dut)
    }
  }

  def verilator_test[T <: Module](dutGen: => T)(testFn: T => Unit): Unit = {
    var annotations = Seq(
        VerilatorBackendAnnotation,
        VerilatorFlags(Seq("--assert")),
        WriteFstAnnotation,
        NoDCEAnnotation
    )
    test(dutGen).withAnnotations(annotations)(init(testFn))
  }
  
  def simple_test[T <: Module](dutGen: => T)(testFn: T => Unit): Unit = {
    test(dutGen).withAnnotations(Seq(WriteVcdAnnotation))(init(testFn))
  }

}
