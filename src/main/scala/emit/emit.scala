package emit

import chisel3._
import chisel3.experimental.ChiselAnnotation
import firrtl.annotations.Annotation
import circt.stage.{FirtoolOption}


import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{CIRCTTarget, CIRCTTargetAnnotation, ChiselStage}
import firrtl.{AnnotationSeq, EmittedVerilogCircuitAnnotation}
import firrtl.options.{Dependency, PhaseManager}


object emitVerilog {

  def apply(name: String, gen: => RawModule): Unit = {
    emitVerilog(new ActiveLowResetWrapper(name, gen), prefix=Some(f"${name}_Prefix__"))
  }

  def apply(gen: => RawModule, dce: Boolean = true, prefix: Option[String] = None): Unit = {
    
    val args = Array(
      // "--help",
      // "--log-level", "debug",
      "--target-dir", "out/"
    )

    var firtool_args = Seq(
        FirtoolOption("--disable-all-randomization"),
        FirtoolOption("--lowering-options=disallowLocalVariables,disallowPackedArrays,noAlwaysComb,locationInfoStyle=none")
    )

    if( dce ) {
      firtool_args = firtool_args :+ FirtoolOption("-O=release")
    } else {
      firtool_args = firtool_args :+ FirtoolOption("-O=debug")
      firtool_args = firtool_args :+ FirtoolOption("-O=debug")
    }

    emit({
      val m = gen
      prefix match {
        case Some(name) => {
          chisel3.experimental.annotate(new ChiselAnnotation {
              def toFirrtl: Annotation = sifive.enterprise.firrtl.NestedPrefixModulesAnnotation(m.toNamed, f"${m.name}__", false)
          })
        }
        case None => {}
      }
      m
      }, 
      args,
      firtool_args
    )
  }

  def emit(gen: => RawModule, args: Array[String] = Array.empty, annotations: AnnotationSeq = Seq.empty): String = {
    (new ChiselStage)
      .execute(
        Array("--target", "verilog") ++ args,
        // Array("--target", "systemverilog") ++ args,
        ChiselGeneratorAnnotation(() => gen) +: annotations
      )
      .collectFirst {
        case EmittedVerilogCircuitAnnotation(a) => a
      }
      .get
      .value
  }
}
