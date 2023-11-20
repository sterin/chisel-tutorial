proc bs_map {fun list} {
    set res {}
    foreach element $list {
        lappend res [$fun $element]
    }
    set res
}

proc bs_connect {args} {
    connect_bd_net {*}[bs_map get_bd_pins $args]
}

proc bs_connect_intf {args} {
    connect_bd_intf_net {*}[bs_map get_bd_intf_pins $args]
}

proc bs_finalize {} {

  upvar project_name project_name
  upvar project_location project_location
  upvar bd_name bd_name

  update_compile_order -fileset sources_1

  regenerate_bd_layout
  assign_bd_address
  validate_bd_design

  # make wraper and set top

  make_wrapper -top -files [get_files "${project_location}/${project_name}.srcs/sources_1/bd/${bd_name}/${bd_name}.bd" ]
  add_files -norecurse ${project_location}/${project_name}.gen/sources_1/bd/${bd_name}/hdl/${bd_name}_wrapper.v
  set_property top ${bd_name}_wrapper [current_fileset]

  save_bd_design
}

proc bs_create_clock {hier args} {
  create_bd_cell -type hier ${hier}

  create_bd_pin -dir I ${hier}/clk_in
  create_bd_pin -dir I ${hier}/resetn_in

  set n [expr [llength $args] + 1]

  proc suffix {name i} {
    upvar n n
    if { $n == 1 } { return $name } { return ${name}_$i }
  }

  create_bd_cell -type ip -vlnv xilinx.com:ip:proc_sys_reset:5.0 ${hier}/[suffix Reset 0]
  create_bd_pin -dir O ${hier}/[suffix clock 0]
  create_bd_pin -dir O ${hier}/[suffix reset 0]
  create_bd_pin -dir O ${hier}/[suffix resetn 0]

  bs_connect ${hier}/clk_in ${hier}/[suffix clock 0] ${hier}/[suffix Reset 0]/slowest_sync_clk
  bs_connect ${hier}/resetn_in ${hier}/[suffix Reset 0]/ext_reset_in
  bs_connect ${hier}/[suffix Reset 0]/peripheral_reset ${hier}/[suffix reset 0]
  bs_connect ${hier}/[suffix Reset 0]/peripheral_aresetn ${hier}/[suffix resetn 0]

  if { $n == 1 } {
    return
  }

  for { set i 1 } { $i < $n } { incr i } {
    set freq [lindex $args $i]

    create_bd_pin -dir O ${hier}/[suffix clock $i]
    create_bd_pin -dir O ${hier}/[suffix reset $i]
    create_bd_pin -dir O ${hier}/[suffix resetn $i]

    create_bd_cell -type ip -vlnv xilinx.com:ip:clk_wiz:6.0 ${hier}/[suffix ClockWizard $i]

    set_property -dict [list \
      CONFIG.CLKOUT1_REQUESTED_OUT_FREQ {350} \
      CONFIG.RESET_TYPE {ACTIVE_LOW} \
    ] [get_bd_cells ${hier}/[suffix ClockWizard $i]]

    create_bd_cell -type ip -vlnv xilinx.com:ip:proc_sys_reset:5.0 ${hier}/[suffix Reset $i] 

    bs_connect ${hier}/clk_in ${hier}/[suffix ClockWizard $i]/clk_in1
    bs_connect ${hier}/resetn_in ${hier}/[suffix ClockWizard $i]/resetn ${hier}/[suffix Reset $i]/ext_reset_in
    bs_connect ${hier}/[suffix ClockWizard $i]/locked ${hier}/[suffix Reset $i]/dcm_locked 
    bs_connect ${hier}/[suffix ClockWizard $i]/clk_out1 ${hier}/[suffix Reset $i]/slowest_sync_clk ${hier}/[suffix clock $i]
    bs_connect ${hier}/[suffix Reset $i]/peripheral_reset ${hier}/[suffix reset $i]
    bs_connect ${hier}/[suffix Reset $i]/peripheral_aresetn ${hier}/[suffix resetn $i]
  }
}

proc bs_create_interrupt_controller {hier irq args} {
  
  set n [llength $args]

  # hierarchy

  create_bd_cell -type hier ${hier}
  
  create_bd_pin -dir I ${hier}/clock
  create_bd_pin -dir I ${hier}/resetn
  create_bd_pin -dir O ${hier}/irq
  create_bd_intf_pin -mode Slave -vlnv xilinx.com:interface:aximm_rtl:1.0 ${hier}/s_axi_lite

  # controller 

  create_bd_cell -type ip -vlnv xilinx.com:ip:axi_intc:4.1 ${hier}/Controller
  set_property CONFIG.C_KIND_OF_INTR {0xFFFFFFFF} [get_bd_cells ${hier}/Controller]
  set_property CONFIG.C_IRQ_CONNECTION {1} [get_bd_cells ${hier}/Controller]

  foreach pin { {clock s_axi_aclk} {resetn s_axi_aresetn} {irq irq} } {
    bs_connect ${hier}/[lindex $pin 0] ${hier}/Controller/[lindex $pin 1]
  }

  bs_connect_intf ${hier}/s_axi_lite ${hier}/Controller/s_axi

  # concat

  create_bd_cell -type ip -vlnv xilinx.com:ip:xlconcat:2.1 ${hier}/Concat
  set_property CONFIG.NUM_PORTS $n [get_bd_cells ${hier}/Concat]

  bs_connect ${hier}/Concat/dout ${hier}/Controller/intr

  for { set i 0 } { $i < $n } { incr i } {
    set name [lindex [lindex $args $i] 0]
    create_bd_pin -dir I ${hier}/$name
    bs_connect "${hier}/$name" "${hier}/Concat/In$i"
  }

  # connect 

  for { set i 0 } { $i < $n } { incr i } {
    set name [lindex [lindex $args $i] 0]
    set pin [lindex [lindex $args $i] 1]
    bs_connect $pin ${hier}/${name}
  }
  
  bs_connect $irq ${hier}/Controller/irq
}

proc bs_create_smartconnect {name slaves masters {n_clocks 1}} {

  create_bd_cell -type ip -vlnv xilinx.com:ip:smartconnect:1.0 $name

  set_property -dict [list \
    CONFIG.NUM_SI [llength $slaves] \
    CONFIG.NUM_MI [llength $masters] \
  ] [get_bd_cells $name]

  for { set i 0 } { $i < [llength $slaves] } { incr i } {
    bs_connect_intf [lindex $slaves $i] [format "%s/S%02d_AXI" $name $i]
  }
  
  for { set i 0 } { $i < [llength $masters] } { incr i } {
    bs_connect_intf [format "%s/M%02d_AXI" $name $i] [lindex $masters $i]
  }

  if {$n_clocks > 1} {
    set_property CONFIG.NUM_CLKS $n_clocks [get_bd_cells $name]
  }
}

proc bs_create_system_ila {name depth {axi {}} {axis {}} {probes {}}} {

    create_bd_cell -type ip -vlnv xilinx.com:ip:system_ila:1.1 ${name}

    set has_interfaces [expr [llength $axi] + [llength $axis] > 0]
    set has_probes [expr [llength $probes] > 0]

    if { $has_interfaces && $has_probes } {
        set type MIX
    } elseif { $has_interfaces && !$has_probes } {
        set type INTERFACE
    } elseif { !$has_interfaces && $has_probes } {
        set type NATIVE
    } else {
        error "bs_create_system_ila: no probes or interfaces"
    }

    set_property CONFIG.C_MON_TYPE ${type} [get_bd_cells $name]
    set_property CONFIG.C_NUM_MONITOR_SLOTS [expr [llength $axi] + [llength $axis]] [get_bd_cells $name]
    set_property CONFIG.C_NUM_OF_PROBES [llength $probes] [get_bd_cells $name]
    set_property CONFIG.C_DATA_DEPTH ${depth} [get_bd_cells $name]

    set probe_idx 0
    for { set i 0 } { $i < [llength $axi] } { incr i } {
        set_property CONFIG.C_SLOT_${probe_idx}_INTF_TYPE {xilinx.com:interface:aximm rtl:1.0} [get_bd_cells $name]
        bs_connect_intf [lindex $axi $i] [format "%s/SLOT_%d_AXI" $name $i]

        set probe_idx [expr $probe_idx + 1]
    }
    
    for { set i 0 } { $i < [llength $axis] } { incr i } {
        set_property CONFIG.C_SLOT_${probe_idx}_INTF_TYPE {xilinx.com:interface:axis_rtl:1.0} [get_bd_cells $name]
        bs_connect_intf [lindex $axis $i] [format "%s/SLOT_%d_AXIS" $name $probe_idx]
        set probe_idx [expr $probe_idx + 1]
    }

    for { set i 0 } { $i < [llength $probes] } { incr i } {
        bs_connect [lindex $probes $i] [format "%s/probe%d" $name $i]
        set probe_idx [expr $probe_idx + 1]
    }
}

