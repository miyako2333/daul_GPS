import socket
import threading
import time
import random
import json


from sbp.client.drivers.pyserial_driver import PySerialDriver
from sbp.client import Handler, Framer
from sbp.navigation import SBP_MSG_BASELINE_NED
from sbp.navigation import SBP_MSG_POS_LLH

import argparse

lst = [];

def main():
    parser = argparse.ArgumentParser(
        description="Swift Navigation SBP Example.")
    parser.add_argument(
        "-p",
        "--port",
        default=['/dev/ttyUSB0'],
        nargs=1,
        help="specify the serial port to use.")
    args = parser.parse_args()

    # Open a connection to Piksi using the default baud rate (1Mbaud)
    with PySerialDriver(args.port[0], baud=115200) as driver:
        with Handler(Framer(driver.read, None, verbose=True)) as source:
            try:
                #for msg, metadata in source:
                    #print(msg)
                    #return msg
                for msg, metadata in source.filter(SBP_MSG_POS_LLH):
                #for msg, metadata in source.filter(SBP_MSG_BASELINE_NED):
                    #print("%.4f,%.4f,%.4f" % (msg.n * 1e-3, msg.e * 1e-3,msg.d * 1e-3))
                    #time.sleep(0.5)
                    
                    print("Lat:%.4f, Lon:%.4f, height:%.4f" % (msg.lon, msg.lat, msg.height ))
                    locodata = {'name':'None', 'lat':msg.lat, 'lng':msg.lon}
                    #sock, addr = ser.accept()
                    #tcplink(sock, addr, ldata)
                    #lst.append(locodata)
                    return(locodata)
                    
            except KeyboardInterrupt:
                pass



def tcplink(connect, addr):
    print('Accept new connection from %s:%s...' % addr)
    # connect.send(b'Hello,s\n')
    while True:
        #loca = location()
        #s = json.dumps(loca)+"\n"
        ldata = main()
        dd = json.dumps(ldata)+"\n"
        data = bytes(dd.encode('utf-8'))
        #time.sleep(1)
        connect.send(data)
        print(ldata)
        if (ser.fileno() == -1):
            break



ser = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
ser.bind(('192.168.5.1', 65432))
ser.listen(1)               # 监听连接 如果有超过5个连接请求，从第6个开始不会被accept
print('Server is running...')       # 打印运行提示
try:
    while True:
        #main()
        
        sock, addr = ser.accept()
        #pthread = threading.Thread(target=main)   #多线程处理socket连接
        #pthread.start()
        #tcplink(sock, addr)
        pthread = threading.Thread(target=tcplink, args=(sock, addr))   #多线程处理socket连接
        pthread.start()
except KeyboardInterrupt:
    
    ser.close()

        
        

