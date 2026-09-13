package com.groupunix.drivewireui;

import java.io.IOException;
import java.net.*;
import java.util.HashSet;
import java.util.Set;
import com.groupunix.drivewireserver.DriveWireServer;

/** Owns every remote UI socket so opting out closes commands and status feeds immediately. */
public final class UITransport {
    private static boolean network;
    private static final Set<Socket> remotes = new HashSet<>();
    private UITransport() { }
    public static synchronized boolean isNetwork() { return network; }
    public static synchronized void setNetwork(boolean enabled) {
        network = enabled;
        if (!enabled) {
            for (Socket socket : remotes.toArray(new Socket[0])) {
                try { socket.close(); } catch (IOException ignored) { }
            }
            remotes.clear();
        }
    }
    public static Socket connect(String host, int port, int timeout) throws IOException {
        Socket remote;
        synchronized (UITransport.class) {
            if (!network) return DriveWireServer.connectLocalUI();
            remote = new Socket() {
                @Override public void close() throws IOException {
                    try { super.close(); }
                    finally { synchronized (UITransport.class) { remotes.remove(this); } }
                }
            };
            remotes.add(remote); // Includes connections still being established.
        }
        try {
            remote.connect(new InetSocketAddress(host, port), timeout);
            synchronized (UITransport.class) {
                if (!network || remote.isClosed()) throw new IOException("Network instance disabled");
            }
            return remote;
        } catch (IOException | RuntimeException e) { remote.close(); throw e; }
    }
}
