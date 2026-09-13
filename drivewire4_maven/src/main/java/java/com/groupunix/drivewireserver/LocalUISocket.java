package com.groupunix.drivewireserver;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

/** Socket-shaped adapter for the existing UI protocol; never creates an OS socket. */
public final class LocalUISocket extends Socket {
    private final Pipe input;
    private final Pipe output;
    private volatile boolean closed;
    private Thread serverThread;

    private LocalUISocket(Pipe input, Pipe output) { this.input = input; this.output = output; }
    public static LocalUISocket[] pair() {
        Pipe a = new Pipe(), b = new Pipe();
        return new LocalUISocket[] { new LocalUISocket(a, b), new LocalUISocket(b, a) };
    }
    public void setServerThread(Thread thread) { serverThread = thread; }
    @Override public InputStream getInputStream() { return input; }
    @Override public OutputStream getOutputStream() {
        return new OutputStream() {
            @Override public void write(int value) throws IOException { output.put((byte) value); }
            @Override public void write(byte[] bytes, int off, int len) throws IOException {
                for (int i = 0; i < len; i++) output.put(bytes[off + i]);
            }
            @Override public void close() { output.close(); }
        };
    }
    @Override public boolean isClosed() { return closed; }
    @Override public boolean isConnected() { return !closed; }
    @Override public boolean isInputShutdown() { return closed; }
    @Override public synchronized void setSoTimeout(int timeout) {
        if (timeout < 0) throw new IllegalArgumentException("Negative timeout");
        input.timeout = timeout;
    }
    @Override public synchronized int getSoTimeout() { return input.timeout; }
    @Override public void close() {
        closed = true;
        input.close(); output.close();
        if (serverThread != null && serverThread != Thread.currentThread()) serverThread.interrupt();
    }

    private static final class Pipe extends InputStream {
        private final byte[] data = new byte[65536];
        private int head, count;
        private boolean eof;
        private volatile int timeout;
        synchronized void put(byte value) throws IOException {
            while (count == data.length && !eof) pause(0);
            if (eof) throw new IOException("Local UI stream closed");
            data[(head + count) % data.length] = value; count++; notifyAll();
        }
        private void pause(long millis) throws IOException {
            try { wait(millis); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new InterruptedIOException(); }
        }
        @Override public synchronized int read() throws IOException {
            byte[] one = new byte[1];
            return read(one, 0, 1) < 0 ? -1 : one[0] & 255;
        }
        @Override public synchronized int read(byte[] bytes, int off, int len) throws IOException {
            java.util.Objects.checkFromIndexSize(off, len, bytes.length);
            if (len == 0) return 0;
            long deadline = System.nanoTime() + timeout * 1000000L;
            while (count == 0 && !eof) {
                if (timeout == 0) pause(0);
                else {
                    long left = deadline - System.nanoTime();
                    if (left <= 0) throw new SocketTimeoutException("Local UI response timed out");
                    pause(Math.max(1, left / 1000000));
                }
            }
            if (count == 0) return -1;
            int n = Math.min(len, count);
            for (int i = 0; i < n; i++) { bytes[off + i] = data[head]; head = (head + 1) % data.length; }
            count -= n; notifyAll(); return n;
        }
        @Override public synchronized int available() { return count; }
        @Override public synchronized void close() { eof = true; notifyAll(); }
    }
}
