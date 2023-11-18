# C:/Xilinx/Vivado/2022.2/bin/vivado.bat -mode tcl -source create_project.tcl -tclargs <project_location>

# set variables

set scripts_location [file normalize [file dirname [info script]]]
set workspace [file normalize [file dirname $scripts_location]]

source $workspace/tcl/u96v2.tcl
source $workspace/tcl/common.tcl

set source_location [file join $workspace out]

bs_create_project [file normalize [lindex $argv 0]] $source_location \
  RegFile.v

create_bd_cell -type module -reference RegFile RegFile_0
create_bd_cell -type module -reference RegFile RegFile_1

bs_create_zynq 0 1 6

bs_create_interrupt_controller \
  InterruptController \
  Zynq/pl_ps_irq0 \
  {intr_0 RegFile_0/intr} \
  {intr_1 RegFile_1/intr}

bs_create_smartconnect \
  SmartConnect \
  { Zynq/M_AXI_HPM0_FPD } \
  { RegFile_0/s_axi_lite RegFile_1/s_axi_lite InterruptController/s_axi_lite }

bs_create_clock Clock

bs_connect Zynq/pl_clk0 Clock/clk_in
bs_connect Zynq/pl_resetn0 Clock/resetn_in

bs_connect Clock/clock_0 */clock */*clk
bs_connect Clock/resetn_0 */resetn */aresent

bs_finalize

start_gui

# # launch_runs impl_1 -to_step write_bitstream -jobs 16
