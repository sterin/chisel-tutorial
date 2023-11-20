# C:/Xilinx/Vivado/2022.2/bin/vivado.bat -mode tcl -source create_project.tcl -tclargs <project_location>

# set variables

set scripts_location [file normalize [file dirname [info script]]]
set workspace [file normalize [file dirname $scripts_location]]

source $workspace/tcl/u96v2.tcl
source $workspace/tcl/common.tcl

set source_location [file join $workspace out]

bs_create_project [file normalize [lindex $argv 0]] $source_location \
  Memset.v

create_bd_cell -type module -reference Memset Memset

bs_create_zynq 1 1 6

bs_create_interrupt_controller \
  InterruptController \
  Zynq/pl_ps_irq0 \
  {intr_ioc Memset/intr_ioc}

create_bd_cell -type ip -vlnv xilinx.com:ip:axi_datamover:5.1 DataMover

set_property -dict [list \
  CONFIG.c_enable_mm2s {0} \
  CONFIG.c_s2mm_btt_used {23} \
] [get_bd_cells DataMover]

bs_create_smartconnect \
  SmartConnect \
  { Zynq/M_AXI_HPM0_FPD } \
  { Memset/s_axi_lite InterruptController/s_axi_lite }

bs_create_smartconnect \
  SmartConnect2 \
  { DataMover/M_AXI_S2MM } \
  { Zynq/S_AXI_HP0_FPD }

bs_connect_intf Memset/m_axis_datamover_cmd DataMover/S_AXIS_S2MM_CMD
bs_connect_intf Memset/m_axis_datamover_data DataMover/S_AXIS_S2MM
bs_connect_intf Memset/s_axis_datamover_sts DataMover/M_AXIS_S2MM_STS

bs_create_system_ila \
    ILA \
    4096 \
    {Memset/s_axi_lite DataMover/M_AXI_S2MM} \
    {Memset/m_axis_datamover_cmd Memset/m_axis_datamover_data Memset/s_axis_datamover_sts} \
    {Memset/intr_ioc}

bs_create_clock Clock

bs_connect Zynq/pl_clk0 Clock/clk_in
bs_connect Zynq/pl_resetn0 Clock/resetn_in

bs_connect Clock/clock_0 */clock */*clk
bs_connect Clock/resetn_0 */*resetn */*aresent

bs_finalize

start_gui
