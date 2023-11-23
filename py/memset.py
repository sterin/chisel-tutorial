import time
from components import *

overlay = pynq.Overlay('memset_1.bit')
memset = overlay.Memset

print("LOADED. Waiting ... ")
input()

buf = alloc(1<<24, dtype=np.uint32)

async def do_run(buf, n):
    
    run_timer = timer()
    memset.print_status("BEFORE")

    for i in range(n):
        
        if not memset.ready():
            t = timer()
            await memset.wait()
            memset.print_status(f"W0 {t}")
        
        memset.memset(buf, i)
        memset.print_status("SUBMITTED")

    while memset.busy():
        
        t = timer()
        await memset.wait()
        memset.print_status(f"W1 {t}")

    memset.print_status("DONE")
    seconds = run_timer.elapsed()
    bytes = buf.size*buf.itemsize*n
    cycles = seconds * 250e6
    words = bytes >> 2

    print(f"TOTAL: {seconds:0.2f}sec {n/seconds:0.2f}iter/sec {seconds/n:0.2f}sec/iter {bytes/(1<<30):0.2f}GiB {bytes/seconds/(1<<30):0.2f}Gbps {100*words/cycles:0.2f}%")


run(do_run(buf, 64))
