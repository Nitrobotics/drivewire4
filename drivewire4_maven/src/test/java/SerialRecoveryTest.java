import com.fazecast.jSerialComm.*;
import com.groupunix.drivewireserver.dwprotocolhandler.*;
import com.groupunix.drivewireui.SyncThread;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.Socket;
import java.util.concurrent.*;

public class SerialRecoveryTest {
    static int checks;
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); checks++; }
    static HierarchicalConfiguration config() {
        HierarchicalConfiguration c=new HierarchicalConfiguration();
        c.setProperty("SerialDevice","TEST"); c.setProperty("SerialRate",230400);
        c.setProperty("DeviceType","serial"); c.setProperty("DeviceFailRetryTime",0);
        c.setProperty("ReadByteWait",0); return c;
    }
    static DWProtocol protocol(HierarchicalConfiguration c) {
        return (DWProtocol)Proxy.newProxyInstance(DWProtocol.class.getClassLoader(),new Class<?>[]{DWProtocol.class},(p,m,a)-> {
            if(m.getName().equals("getConfig")) return c;
            if(m.getReturnType()==int.class) return 0;
            if(m.getReturnType()==boolean.class) return false;
            return null;
        });
    }
    static DWSerialDevice device(SerialPort p) throws Exception {
        SerialPort.pending.add(p); return new DWSerialDevice(protocol(config()));
    }
    static InputStream faulty(int mode) {
        return new InputStream() {
            public int available() throws IOException {
                if(mode==0) throw new IOException("read failed");
                if(mode==1) throw new IllegalStateException("driver failed");
                return mode==2 ? -1 : 1;
            }
            public int read() { return -1; }
        };
    }
    static void await(java.util.function.BooleanSupplier condition) throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        while(!condition.getAsBoolean() && System.nanoTime()<end) Thread.sleep(5);
        check(condition.getAsBoolean(),"timed out waiting for state");
    }
    public static void main(String[] args) throws Exception {
        Logger.getRootLogger().setLevel(Level.OFF);
        for(int failure=0;failure<3;failure++) {
            SerialPort p=new SerialPort(); p.paramsOK=failure!=0; p.listenerOK=failure!=1; p.failRemove=failure==2;
            if(failure==2) p.paramsOK=false;
            try { device(p); throw new AssertionError("setup should fail"); } catch(Exception expected) { checks++; }
            check(!p.open && p.closes==1,"failed setup leaked COM handle: " + failure + ", open=" + p.open + ", closes=" + p.closes);
        }
        for(int failure=0;failure<4;failure++) {
            SerialPort p=new SerialPort(); p.input=faulty(failure); DWSerialDevice d=device(p);
            p.event(SerialPort.LISTENING_EVENT_DATA_AVAILABLE);
            check(!d.connected() && d.comRead1(false)==-1,"read failure did not enter reconnect");
            d.close(); check(p.closes==1,"read failure cleanup");
        }
        SerialPort p=new SerialPort(); p.input=new ByteArrayInputStream(new byte[]{0,(byte)128,(byte)255});
        DWSerialDevice d=device(p); p.event(SerialPort.LISTENING_EVENT_DATA_AVAILABLE);
        InputStream stream=d.getInputStream();
        check(stream.read()==0 && stream.read()==128 && stream.read()==255,"unsigned stream data/FF");
        d.comWrite(new byte[]{3,4,5},3,false);
        check(java.util.Arrays.equals(p.written.toByteArray(),new byte[]{3,4,5}),"normal write");
        p.failWrite=true; d.comWrite1(8,false);
        check(!d.connected() && d.comRead1(false)==-1,"write failure not recovered"); d.close();

        p=new SerialPort(); p.input=new ByteArrayInputStream(new byte[1024]); d=device(p);
        p.event(SerialPort.LISTENING_EVENT_DATA_AVAILABLE);
        check(!d.connected() && d.comRead1(false)==-1,"overflow reused damaged transaction"); d.close();

        p=new SerialPort(); d=device(p); final DWSerialDevice waiting=d;
        FutureTask<Integer> read=new FutureTask<>(()->waiting.comRead1(false)); Thread t=new Thread(read); t.start();
        Thread.sleep(30);
        SerialPortDataListener old=p.listener;
        p.event(SerialPort.LISTENING_EVENT_DATA_AVAILABLE|SerialPort.LISTENING_EVENT_PORT_DISCONNECTED);
        check(read.get(1,TimeUnit.SECONDS)==-1,"disconnect did not wake reader");
        try { d.comRead(2); throw new AssertionError("partial transaction accepted"); } catch(IOException expected) { checks++; }
        d.close(); old.serialEvent(new SerialPortEvent(p,SerialPort.LISTENING_EVENT_DATA_AVAILABLE));
        check(!d.connected(),"old callback revived closed connection");

        // Exercise actual handler setup/retry helpers with repeated remove/open cycles.
        DWProtocolHandler handler=new DWProtocolHandler(0,config());
        Method setup=DWProtocolHandler.class.getDeclaredMethod("setupProtocolDevice"); setup.setAccessible(true);
        Method retry=DWProtocolHandler.class.getDeclaredMethod("waitForDeviceRetry"); retry.setAccessible(true);
        for(int i=0;i<20;i++) {
            p=new SerialPort(); SerialPort.pending.add(p); setup.invoke(handler);
            check(handler.isConnected(),"reopen failed");
            p.event(SerialPort.LISTENING_EVENT_PORT_DISCONNECTED);
            check(!handler.isConnected(),"handler still connected after removal");
            setup.invoke(handler); check(!p.open && p.closes==1,"old handle retained on failed reopen");
        }
        long start=System.nanoTime(); check((Boolean)retry.invoke(handler),"retry aborted");
        check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)>=900,"zero config spins retry");
        FutureTask<Boolean> wait=new FutureTask<>(()->(Boolean)retry.invoke(handler)); t=new Thread(wait); t.start();
        Thread.sleep(30); handler.shutdown();
        check(!wait.get(1,TimeUnit.SECONDS),"shutdown did not cancel retry");
        int opens=SerialPort.attempts; setup.invoke(handler);
        check(SerialPort.attempts==opens,"shutdown reopened port");

        // Reproduce the UI EOF-vs-die race without starting a UI or touching ports.
        Field sock=SyncThread.class.getDeclaredField("sock"); sock.setAccessible(true);
        Method close=SyncThread.class.getDeclaredMethod("closeSocket"); close.setAccessible(true);
        for(int i=0;i<100;i++) {
            SyncThread sync=new SyncThread(); Socket socket=new Socket(); sock.set(sync,socket);
            FutureTask<Void> stopping=new FutureTask<>(()->{sync.die();return null;});
            Thread stop=new Thread(stopping); stop.start(); close.invoke(sync); stopping.get(1,TimeUnit.SECONDS);
            check(socket.isClosed() && sock.get(sync)==null,"UI close race");
        }
        System.out.println("PASS "+checks+" serial recovery checks: failed-open cleanup, read/write loss, EOF, overflow, unsigned FF, blocked-read wake, stale callback, repeated reopen, retry pacing, stop, UI-close race.");
    }
}
