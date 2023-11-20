import os
import asyncio
import time
import numpy as np
from pprint import pprint

class timer:
    def __init__(self):
        self.start = time.time()
    def elapsed(self):
        return time.time() - self.start
    def __repr__(self):
        return f'{self.elapsed():0.2f}s'

class Registers:

    def __init__(self):
        self.next_reg = 0

    def __call__(self, n=1):
        if n == 1:
            return self.reg()
        else:
            return self.regs(n)

    def __len__(self):
        return self.next_reg

    def reg(self):
        tmp, self.next_reg = self.next_reg, self.next_reg+1
        return tmp
    
    def regs(cls, n):
        return [cls.reg() for i in range(n)]


def run(*coro):
    loop = asyncio.get_event_loop()
    loop.run_until_complete( asyncio.wait(coro) )


def progress(c):
    print(c, end='', flush=True)


def format(x):
    if type(x) == np.uint8:
        return '0x%02x'%x
    if type(x) == np.uint16:
        return '0x%04x'%x
    if type(x) == np.uint32:
        return '0x%08x'%x
    if type(x) == np.uint64:
        return '0x%016x'%x
    return 'XXX'


np.set_printoptions(formatter={'int':format})


import pynq


_metadata_cache = os.path.join(os.path.dirname(pynq.__file__), 'pl_server', '_current_metadata.pkl')
if os.path.exists( _metadata_cache ):
    os.unlink(_metadata_cache)


def alloc(n, dtype=np.uint32):
    return pynq.allocate(shape=n, dtype=dtype)


class RegFile(pynq.DefaultIP):

    bindto = ['xilinx.com:module_ref:RegFile:1.0']

    N_CHANNELS = 4

    R = Registers()

    INTR_IOC = R()
    REG_1 = R()
    REG_2 = R()
    REG_3 = R()
    REG_4 = R()
    REG_5 = R()
    REG_6 = R()

    def __init__(self, description):
        super().__init__(description)
        self.description = description

    def ack(self):
        self.set_intr(0)

    def set_intr(self, val):
        self.mmio.write_reg(self.INTR_IOC, val)
        return self.get_intr()

    def get_intr(self):
        return self.mmio.read(self.INTR_IOC*4)

    async def wait(self):
        await self.intr.wait()
        self.ack()


class Memset(pynq.DefaultIP):

    bindto = ['xilinx.com:module_ref:Memset:1.0']

    R = Registers()

    ADDR = R()
    VALUE = R()
    BTT = R()
    STATUS = R()
    BYTES_WRITTEN = R()
    COMMMANDS_COMPLETED = R()
    COMMANDS_INFLIGHT = R()

    def __init__(self, description):
        super().__init__(description)
        self.description = description

    def write_reg(self, r, v):
        self.mmio.write_reg(r*4, v)

    def read_reg(self, r):
        return self.mmio.read(r*4)

    def clear_interrupt(self):
        self.write_reg(self.STATUS, 4)

    def memset(self, buf, value):
        self.write_reg(self.ADDR, buf.device_address)
        self.write_reg(self.VALUE, value)
        self.write_reg(self.BTT, buf.size * buf.itemsize)

    def status(self):
        return self.read_reg(self.STATUS)

    def ready(self):
        return self.status() & 1

    def busy(self):
        return (self.status() >> 1) & 1

    def bytes_written(self):
        return self.read_reg(self.BYTES_WRITTEN)

    def commands_completed(self):
        return self.read_reg(self.COMMMANDS_COMPLETED)

    def commands_inflight(self):
        return self.read_reg(self.COMMANDS_INFLIGHT)

    def print_status(self, tag=""):
        print(f'[Memset] {tag:12} addr={self.read_reg(self.ADDR):08X} value={self.read_reg(self.VALUE):08X} btt={self.read_reg(self.BTT):08X} status={self.status():08X} bytes_written={self.bytes_written():08X} commands_completed={self.commands_completed():08X} commands_inflight={self.commands_inflight():08X}')

    async def wait(self):
        await self.intr_ioc.wait()
        self.clear_interrupt()
