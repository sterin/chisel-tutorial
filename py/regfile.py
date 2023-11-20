from components import *

overlay = pynq.Overlay('regfile.bit')

regfile_0 = overlay.RegFile_0
regfile_1 = overlay.RegFile_0

print("LOADED. Waiting ... ")
# input()

async def trigger(delay, tag, regfile):
    await asyncio.sleep(delay)
    for i in range(10):
        await asyncio.sleep(1)
        print(f'[{tag}] INTR! {regfile.get_intr()} -> {regfile.set_intr(1)}')
        

async def handler(tag, regfile):
    while True:
        await regfile.wait()
        print(f"[{tag}] ACK!")


run(trigger(0, "XXX", regfile_0), trigger(0.5, "YYY", regfile_1), handler("XXX", regfile_0), handler("YYY", regfile_1))
