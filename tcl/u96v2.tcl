proc bs_create_project {path source_location args} {
 
    upvar project_location project_location
    set project_location [file normalize $path]

    upvar project_name project_name
    set project_name [file tail ${project_location}]

    create_project ${project_name} ${project_location} -part xczu3eg-sbva484-1-i
    set_property board_part avnet.com:ultra96v2:part0:1.2 [current_project]

    # create 
    
    upvar bd_name bd_name
    set bd_name "design_1"

    create_bd_design ${bd_name}

    # add sources

    foreach file $args {
        add_files -norecurse [file normalize $source_location/$file]
    }
}

proc bs_create_zynq { {slaves 0} {masters 1} {clk_divisor 15} } {

    create_bd_cell \
        -type ip \
        -vlnv xilinx.com:ip:zynq_ultra_ps_e:3.5 Zynq

    apply_bd_automation \
        -rule xilinx.com:bd_rule:zynq_ultra_ps_e \
        -config {apply_board_preset "1" } \
        [get_bd_cells /Zynq]

    set_property CONFIG.PSU__CRL_APB__PL0_REF_CTRL__DIVISOR0 $clk_divisor [get_bd_cells Zynq]

    for { set i 0 } { $i < 2 } { incr i } {
        set_property CONFIG.PSU__USE__M_AXI_GP$i [expr $i < $masters] [get_bd_cells Zynq]
    }

    for { set i 0 } { $i < 4 } { incr i } {
        set_property CONFIG.PSU__USE__S_AXI_GP[expr $i + 2] [expr $i < $slaves] [get_bd_cells Zynq]
    }
}

