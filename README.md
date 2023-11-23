# Generate Verilog from Chisel

## Setup Chisel

* Install Java 11 JDK. On Ubuntu, for example, this can be done by:

On Ubuntu:

```bash
sudo apt-get install openjdk-11-jdk
```

* Install SBT (Scala Build Tool) by following the instructions at:

https://www.scala-sbt.org/download.html

(On Ubuntu, follow the instructions under "Linux (deb)")

* Download firtool and put it somewhere on your PATH:

https://github.com/llvm/circt/releases/tag/firtool-1.43.0

On Ubuntu, download `firtool` from:

https://github.com/llvm/circt/releases/download/firtool-1.43.0/firrtl-bin-ubuntu-20.04.tar.gz

Extract and put it, for example, at ~/.local/bin/

```bash
wget firrtl-bin-ubuntu-20.04.tar.gz
tar xvf firrtl-bin-ubuntu-20.04.tar.gz
mkdir -p ~/.local/bin
mv firtool-1.43.0/bin/firtool ~/.local/bin/
```

## Generate Verilog

From `chisel-tutorial`

```bash
sbt 'runMain axi.Emit'
sbt 'runMain project.memset.Emit'
```

Now the `out/` subdirectory should contain the files `RegFile.v` and `Memset.v`

## Generate Project

```bash
~/tools/Xilinx/Vivado/2023.2/bin/vivado -mode batch -source tcl/create_memset_project.tcl -tclargs xilinx/memset_1
```

(Replace 2023.2 with your version of Vivado. )

It should generate a vivado project under `xilinx/memset_1` and start Vivado when done.

## Generate a bitstream

Genearte the bitstream. Once finished, copy the files into the device:

```
xilinx/memset_1/memset_1.runs/impl_1/design_1_wrapper.bit
xilinx/memset_1/memset_1.gen/sources_1/bd/design_1/hw_handoff/design_1.hwh
```

## Setup the device

Follow the instructions at

* `docs/u96v2-board-setup.md`
* `docs/vivado-setup.md`

Then copy chisel-tutorial.tgz into the device

```bash
scp chisel-tutorial.tgz xilinx@192.168.3.1:
```

And from the device

```bash
tar xvf chisel-tutorial.tgz
```

## Copy the bitstream to the device

```bash
scp xilinx/memset_1/memset_1.runs/impl_1/design_1_wrapper.bit xilinx@192.168.3.1:chisel-tutorial/py/memset_1.bit
xilinx/memset_1/memset_1.gen/sources_1/bd/design_1/hw_handoff/design_1.hwh xilinx@192.168.3.1:chisel-tutorial/py/memset_1.hwh
```

Then from the device

```bash
cd chisel-tutorial/py
./run_pynq.sh memset.py
```
