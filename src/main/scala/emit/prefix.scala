package sifive.enterprise.firrtl
import firrtl.annotations.{Named, SingleTargetAnnotation}

case class NestedPrefixModulesAnnotation(target: Named, prefix: String, inclusive: Boolean) extends SingleTargetAnnotation[Named] {
    def duplicate(n: Named): NestedPrefixModulesAnnotation = this.copy(target = n)
}
