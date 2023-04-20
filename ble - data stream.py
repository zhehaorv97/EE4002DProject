import asyncio
import platform
import struct
from bleak import BleakClient
import sys
import binascii
import csv
import time

start = time.time()
dataList2 = list(range(30))
count = 0
length = 3
payload = []
correct = 0.0
total = 0.0

def writedata_movement(data):
    try:
        f = open("data1.csv", 'a', newline="") 
    except IOError:
        f = open("data2.csv", 'a', newline="") 
    writer = csv.writer(f)
    writer.writerow(data)
    f.close()


class Service:
    
    def __init__(self):
        self.data_uuid = None
        self.ctrl_uuid = None
        self.period_uuid = None

    async def read(self, client):
        raise NotImplementedError()
    

class Sensor(Service):

    def callback(self, sender: int, data: bytearray):
        raise NotImplementedError()

    async def start_listener(self, client, *args):
        await client.start_notify(self.data_uuid, self.callback)

    async def read(self, client):
        val = await client.read_gatt_char(self.data_uuid)
        return self.callback(1, val)

class Nrf52832ECGdataSubService:

    def __init__(self):
        self.bits = 0

    def enable_bits(self):
        return self.bits

    def cb_sensor(self, data):
        raise NotImplementedError
    

class Nrf52832ECGdata(Sensor):

    def __init__(self):
        super().__init__()
        self.data_uuid = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"
        self.ctrl_uuid = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"
        self.ctrlBits = 0
        self.sub_callbacks = []

    def register(self, cls_obj: Nrf52832ECGdataSubService):
        self.ctrlBits |= cls_obj.enable_bits()
        self.sub_callbacks.append(cls_obj.cb_sensor)

    async def start_listener(self, client, *args):
        await client.start_notify(self.data_uuid, self.callback)

    def callback(self, sender: int, data: bytearray):

        print(str(len(data))+ " bytes of data is received")
    
        global payload
        global count
        global total
        global correct

        length = 40
        if total > 100000:
            total = 0
            correct = 0
        total += length
        for i in range (length):
            payload.append(time.time()-start)
            num = 255-(0x82+data[i*3]+data[i*3+1])
            if ((num & 0b11111111)==data[i*3+2]):
                num = data[i*3] << 8 | data[i*3+1]
                if (num>32767):
                    payload.append(~(65535-num))
                    payload.extend(data[length*3+i*3:length*3+3+i*3])
                    writedata_movement(payload)
                    correct += 1
                    
                else:
                    payload.append(num)
                    payload.extend(data[length*3+i*3:length*3+3+i*3])
                    writedata_movement(payload)
                    correct += 1
                payload = []


class ECGSensorNrf52832ECGdata(Nrf52832ECGdataSubService):
    def __init__(self):
        super().__init__()
        self.bits = 2
        self.scale_acc = 4.0/32768.0
        self.scale_gyro = 500.0/65536.0
        

async def run(ble_address: str):

    async with BleakClient(address) as client:
        x = await client.is_connected()
        print("Connected: {0}".format(x))
        movement_sensor = Nrf52832ECGdata()
        await movement_sensor.start_listener(client)
        while True:
            await asyncio.sleep(1.0)



if __name__ == "__main__":
    import os

    os.environ["PYTHONASYNCIODEBUG"] = str(1)
    address = (
        "F4:2D:67:9F:B1:CB"
        if platform.system() != "Darwin"
        else "6FFBA6AE-0802-4D92-B1CD-041BE4B4FEB9"
    )
    
    loop = asyncio.get_event_loop()
    loop.run_until_complete(run(address))

