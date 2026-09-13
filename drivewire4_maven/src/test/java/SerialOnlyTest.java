import com.groupunix.drivewireserver.*;
import com.groupunix.drivewireserver.dwprotocolhandler.DWTCPDevice;
import java.net.*;
import java.nio.file.*;
import java.util.zip.*;
import org.apache.commons.configuration.XMLConfiguration;

/** Standalone regression test; never contacts an external host or opens a COM port. */
public class SerialOnlyTest {
    interface Action { void run() throws Exception; }
    static int checks;
    static void denied(Action action) throws Exception {
        try { action.run(); } catch (java.io.IOException | IllegalArgumentException e) { checks++; return; }
        throw new AssertionError("Network operation was permitted");
    }
    static void check(boolean value) { if (!value) throw new AssertionError(); checks++; }
    public static void main(String[] args) throws Exception {
        SerialOnly.initialize();
        for (String path : new String[] {"http://example.invalid/disk.dsk", "https://example.invalid/",
                "ftp://example.invalid/disk", "sftp://example.invalid/disk", "smb://example.invalid/share",
                "\\\\example.invalid\\share\\disk", "file://example.invalid/share/disk",
                "file:////example.invalid/share", "file:%2f%2fexample.invalid/share",
                "zip:https://example.invalid/disks.zip!/disk.dsk"}) {
            denied(() -> SerialOnly.files().resolveFile(path));
        }
        denied(() -> SerialOnly.localURL("https://example.invalid/"));
        denied(() -> SerialOnly.connectUI("example.invalid", 6800));
        denied(() -> SerialOnly.connectUI("192.0.2.1", 6800));
        denied(() -> new DWTCPDevice(0, 0));
        Path temp = Files.createTempDirectory("dw4-local-test-");
        try {
            Path disk = temp.resolve("local disk.dsk");
            Files.write(disk, new byte[] {1, 2, 3, 4});
            check(SerialOnly.files().resolveFile(disk.toString()).getContent().getSize() == 4);
            check(SerialOnly.files().resolveFile(disk.toUri().toString()).getContent().getSize() == 4);
            check(SerialOnly.localURL(disk.toUri().toString()).getProtocol().equals("file"));
            Path zip = temp.resolve("disk.zip");
            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
                out.putNextEntry(new ZipEntry("disk.dsk")); out.write(new byte[] {1, 2, 3, 4}); out.closeEntry();
            }
            check(SerialOnly.files().resolveFile("zip:" + zip.toUri() + "!/disk.dsk").getContent().getSize() == 4);
            DriveWireServer.serverconfig = new XMLConfiguration();
            DWUIThread ui = new DWUIThread(0);
            Thread thread = new Thread(ui); thread.start(); thread.join(2000);
            java.lang.reflect.Field field = DWUIThread.class.getDeclaredField("srvr"); field.setAccessible(true);
            check(field.get(ui) == null && !thread.isAlive());
            field = DriveWireServer.class.getDeclaredField("uiObj"); field.setAccessible(true); field.set(null, ui);
            field = DriveWireServer.class.getDeclaredField("ready"); field.setAccessible(true); field.setBoolean(null, true);
            com.groupunix.drivewireui.UITransport.setNetwork(false);
            try (Socket client = com.groupunix.drivewireui.UITransport.connect("unused.invalid", 1, 1000)) {
                check(client instanceof LocalUISocket);
                client.setSoTimeout(2000);
                client.getOutputStream().write("-1\0ui server show instances\n".getBytes());
                byte[] response = client.getInputStream().readAllBytes();
                check(response.length >= 3 && response[0] == 0 && response[1] == 0 && response[2] == 0);
            }
            try (ServerSocket local = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
                com.groupunix.drivewireui.UITransport.setNetwork(true);
                try (Socket remote = com.groupunix.drivewireui.UITransport.connect("127.0.0.1", local.getLocalPort(), 1000);
                     Socket accepted = local.accept()) {
                    check(!(remote instanceof LocalUISocket));
                    com.groupunix.drivewireui.UITransport.setNetwork(false);
                    accepted.setSoTimeout(1000);
                    check(remote.isClosed() && accepted.getInputStream().read() == -1);
                }
            }
            try (Socket client = com.groupunix.drivewireui.UITransport.connect("unused.invalid", 1, 1000)) {
                check(client instanceof LocalUISocket);
                client.setSoTimeout(20);
                try { client.getInputStream().read(); throw new AssertionError("Missing timeout"); }
                catch (SocketTimeoutException expected) { checks++; }
            }
            ui.die();

        } finally {
            ((org.apache.commons.vfs.impl.DefaultFileSystemManager) SerialOnly.files()).close();
            try (java.util.stream.Stream<Path> paths = Files.walk(temp)) {
                for (Path p : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
        System.out.println("PASS: " + checks + " COM-only checks (network rejection, local files/ZIP, socket-free local UI, remote opt-out)");
    }
}
