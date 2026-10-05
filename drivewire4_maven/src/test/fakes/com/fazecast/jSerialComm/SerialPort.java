package com.fazecast.jSerialComm;

import java.io.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Test-only serial hardware substitute. Never included in the server jar. */
public final class SerialPort {
    public static final int NO_PARITY=0, EVEN_PARITY=2, ODD_PARITY=1, MARK_PARITY=3, SPACE_PARITY=4;
    public static final int ONE_STOP_BIT=1, ONE_POINT_FIVE_STOP_BITS=2, TWO_STOP_BITS=3;
    public static final int FLOW_CONTROL_DISABLED=0, FLOW_CONTROL_RTS_ENABLED=1, FLOW_CONTROL_CTS_ENABLED=16;
    public static final int FLOW_CONTROL_XONXOFF_IN_ENABLED=65536, FLOW_CONTROL_XONXOFF_OUT_ENABLED=1048576;
    public static final int TIMEOUT_NONBLOCKING=0, LISTENING_EVENT_DATA_AVAILABLE=1, LISTENING_EVENT_PORT_DISCONNECTED=0x10000000;
    public static final ConcurrentLinkedQueue<SerialPort> pending = new ConcurrentLinkedQueue<>();
    public static volatile int attempts;
    public volatile boolean open, openOK=true, paramsOK=true, listenerOK=true, failWrite, failRemove;
    public volatile int closes, flushes, rate;
    public volatile SerialPortDataListener listener;
    public InputStream input = new ByteArrayInputStream(new byte[0]);
    public final ByteArrayOutputStream written = new ByteArrayOutputStream();
    public static SerialPort[] getCommPorts() { return new SerialPort[0]; }
    public static SerialPort getCommPort(String name) {
        attempts++;
        SerialPort p=pending.poll();
        if(p==null) { p=new SerialPort(); p.openOK=false; }
        return p;
    }
    public boolean openPort() { return open=openOK; }
    public boolean isOpen() { return open; }
    public boolean closePort() { closes++; open=false; return true; }
    public boolean setFlowControl(int flags) { return paramsOK; }
    public boolean setComPortParameters(int r,int bits,int stop,int parity) { rate=r; return paramsOK; }
    public boolean setComPortTimeouts(int flags,int r,int w) { return true; }
    public boolean flushIOBuffers() { flushes++; return true; }
    public boolean setDTR() { return true; }
    public boolean clearDTR() { return true; }
    public boolean setRTS() { return true; }
    public boolean clearRTS() { return true; }
    public int getBaudRate() { return rate; }
    public String getSystemPortName() { return "TEST"; }
    public InputStream getInputStream() { return input; }
    public OutputStream getOutputStream() { return new OutputStream() {
        public void write(int value) throws IOException {
            if(failWrite || !open) throw new IOException("simulated removal during write");
            written.write(value);
        }
    }; }
    public boolean addDataListener(SerialPortDataListener l) { listener=l; return listenerOK; }
    public void removeDataListener() {
        listener=null;
        if(failRemove) throw new IllegalStateException("simulated listener cleanup failure");
    }
    public void event(int mask) { SerialPortDataListener l=listener; if(l!=null) l.serialEvent(new SerialPortEvent(this,mask)); }
}
