# An Idiosyncratic Guide to Chisel

## Prerequisites


## Primitives


```mermaid
stateDiagram-v2
    
    direction LR

    state "[000] \n R" as 000
    state "[100] \n R" as 100
    state "[001] \n RV" as 001
    state "[101] \n RV" as 101
    state "[111] \n V" as 111

    [*] --> 000
    000 --> 100 : e
    001 --> 000 : d
    001 --> 100 : ed
    100 --> 001 : !e
    100 --> 101 : e
    101 --> 001 : d
    101 --> 101 : ed
    101 --> 111 : e



    111 --> 101 : d
```



### Skid Buffer


```mermaid
stateDiagram
    
    state Empty "s_empty \n [s_enq.ready]" as E
    state Busy "s_busy \n [s_enq.ready && s_deq.valid]" as B
    state Full "s_full \n [s_deq.valid]" as F

    direction LR

    [*] --> E
    E --> B : [s_enq.valid] \n out = s_enq.data
    B --> E : [s_deq.ready] \n s_deq.data = out
    B --> F : [s_enq.valid] \n buf = s_enq.data
    F --> B : [s_deq.ready] \n s_deq.data = out \n out = buf
    B --> B : [s_enq.valid && s_deq.ready] \n out = s_enq.data \n s_deq.data = out
```



## Final Project

As the final project, we will implement a `memset` block. Given a start addres, size 

Memset block. 

* AXI4-Lite register interface
* Command FIFO
* Interrupt 
* Write to memory using Xilinx' AXI DataMover



In the next iteration, maybe even implement a full-fledged high-performance bursting AXI4 master.


Chisel Cheatsheet

# Scala

Objects
Classes
Companion objects
Implicit classes (adding methods to existing classes)
Functions
Lambdas and closures
Types
Type Inference
Seq
.foreach
.map


# Basic features to demonstrate

Inputs
Outputs
Types:
    * Bool
    * UInt
    * Bundles
    * Vec/VecInit
    * .U
    * .W
Directions:
    * Input
    * Output
    * Flipped
Modules:
    * Instantiation (Module(new ...))
Combinational stuff:
    * Boolean operators
    * Mux
    * Cat
    * extract bits
    * asUInt(), asTypeOf(), 
    * when/elsewhen/otherwise
    * Wire/WireInit/
    * arithmetic
Sequential
    * RegInit
    * state machines

Connect:
    <>
    :=

Last connection takes precedence


mention memories


difference between chisel and scala types (a chilsel type is an instance of a scala type)

.cloneType
chiselTypeOf
log2Ceil

# Generators

syntactic sugar for generators:

e.g. 

when( c0 ) {
    when( c1 ) {
        a := b
    }.otherwise {
        a := c
    }
}.otherwise {
    a := d
}

maintains a context of the current condition

Difference between if and when
That `for` runs during generation, not in hardware

# Streams

Basic bundle
Basic functions (enq, deq)
Add basic sources (Source, Input, Output)
Buffer // InlineInstance
.buf/.rbuf
.transform
.transform (do on the first cycle of beat)
.split()
Vec.join() 
.join_with()
.join(s){}
.skid()

.axis()
.from_axis()

.iterator
.reduce

.fifo (no need to implement)

Consider:
    Stage
    .staget()

# Simple AXI Stream Design



# sequence

* Hello world (trivial module z := x + y)
* 
* Streams
* 


# AXI4-Lite Register File 

## Requirements:

* High throughput: sustaine 1 read per cycle 
* 


# Projects:

Wrapper around DataMover (split a transaction into multiple DM commands)

## memset 1

DMA -> memset <-> DataMover 

## memset 2

axi lite -> memset <-> datamover

