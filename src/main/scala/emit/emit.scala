package emit

import chisel3._
import chisel3.experimental.ChiselAnnotation
import firrtl.annotations.Annotation
import firrtl.annotations.ComponentName
import firrtl.AttributeAnnotation

import firrtl.transforms.NoDCEAnnotation

object emitVerilog {

  def apply(name: String, gen: => RawModule) : Unit = {
    emitVerilog(new ActiveLowResetWrapper(name, gen), prefix=Some(f"${name}_Prefix__"))
  }

  def apply(gen: => RawModule, dce: Boolean = true, prefix: Option[String] = None): Unit = {
    val stage = new chisel3.stage.ChiselStage
    var annotations: Array[Annotation] = Array.empty;
    prefix match {
      case Some(prefix) => { annotations = annotations ++ Prefix.annotation(prefix) }
      case None => {}
    }
    if( !dce ) {
      annotations = annotations :+ NoDCEAnnotation
    }

    val args = Array(
      "--emission-options", "disableMemRandomization,disableRegisterRandomization",
      "--target-dir", "out/"
    )

    stage.emitVerilog(gen, args, annotations=annotations.toSeq)
  }

}
