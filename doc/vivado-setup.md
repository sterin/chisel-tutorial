# Vivado Setup

## Vivado board definition file for Ultra96-V2

To work with the board, you'll need to install the Board Deifnition Files.

Download board definition for the Ultra 96 V2 and install according to the instructions in the readme file.

https://github.com/Avnet/bdf.git

Requires restarting Vivado to recognize the board.

## Hardware manager and Integrated Logic Analzyer (Probably Not Needed)

This happened with previous versions, I don't think you'll need it, but in case there are problems with ILA it migh be worth trying.

The board hangs when trying to use the ILA. The following steps resolve the problem.

1. Find out the bootargs for the kernel on the board

```
cat /proc/cmdline
```

2. Create a file uEnv.txt 
```
bootargs=<contents of /proc/cmdline> cpuidle.off=1
```

3. Put the file on the boot partition of the SD card

