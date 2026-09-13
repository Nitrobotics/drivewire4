package com.groupunix.drivewireserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.impl.DefaultFileSystemManager;
import org.apache.commons.vfs.provider.local.DefaultLocalFileProvider;
import org.apache.commons.vfs.provider.zip.ZipFileProvider;
import org.apache.commons.vfs.provider.jar.JarFileProvider;
import org.apache.commons.vfs.provider.gzip.GzipFileProvider;

/** This build accepts serial devices and local files only. GUI IPC stays on loopback. */
public final class SerialOnly {
    private SerialOnly() { }
    public static boolean enabled() { return true; }

    public static void initialize() {
        // XML configurations must not fetch external DTDs or schemas.
        System.setProperty("javax.xml.accessExternalDTD", "");
        System.setProperty("javax.xml.accessExternalSchema", "");
        System.setProperty("javax.xml.accessExternalStylesheet", "");
    }

    public static void rejectNetwork() throws IOException {
        throw new IOException("Network connections are disabled in this COM-only build.");
    }

    public static Socket connectUI(String host, int port) throws IOException {
        // Never resolve an arbitrary hostname, even if it might resolve to loopback.
        if (!("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)))
            throw new IOException("The UI server must be on 127.0.0.1 in this COM-only build.");
        return new Socket(InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), port);
    }

    public static String localPath(String path) {
        if (path == null) throw new IllegalArgumentException("Missing local path");
        String p = path.replace('\\', '/');
        // Archive providers recursively resolve their outer file through this same manager.
        if (p.matches("(?i)^(zip|jar|gz):.*"))
            return checkedArchive(path, p.substring(p.indexOf(':') + 1));
        if (p.regionMatches(true, 0, "file:", 0, 5)) {
            p = p.substring(5);
            if (p.startsWith("///")) p = p.substring(2);
        }
        // Decode URI escapes for validation (spaces in file URLs remain supported).
        try { p = java.net.URLDecoder.decode(p.replace("+", "%2B"), "UTF-8").replace('\\', '/'); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid local path: " + path); }
        if (p.startsWith("//") || p.indexOf('\0') >= 0 ||
                (p.contains(":") && !p.matches("^/?[A-Za-z]:/.*")))
            throw new IllegalArgumentException("Only local files are allowed in this COM-only build: " + path);
        return path;
    }

    private static String checkedArchive(String original, String nested) {
        int bang = nested.indexOf("!");
        localPath(bang < 0 ? nested : nested.substring(0, bang));
        return original;
    }

    public static URL localURL(String value) throws MalformedURLException {
        try { localPath(value); }
        catch (IllegalArgumentException e) { throw new MalformedURLException(e.getMessage()); }
        URL url = new URL(value);
        if (!"file".equalsIgnoreCase(url.getProtocol()) && !"jar".equalsIgnoreCase(url.getProtocol()))
            throw new MalformedURLException("Network URLs are disabled in this COM-only build.");
        return url;
    }

    private static FileSystemManager files;
    public static synchronized FileSystemManager files() throws FileSystemException {
        if (files == null) {
            DefaultFileSystemManager m = new DefaultFileSystemManager() {
                @Override public FileObject resolveFile(FileObject base, String uri, FileSystemOptions opts)
                        throws FileSystemException {
                    try { localPath(uri); }
                    catch (IllegalArgumentException e) { throw new FileSystemException(e); }
                    return super.resolveFile(base, uri, opts);
                }
            };
            m.addProvider("file", new DefaultLocalFileProvider());
            m.addProvider("zip", new ZipFileProvider());
            m.addProvider("jar", new JarFileProvider());
            m.addProvider("gz", new GzipFileProvider());
            m.init();
            m.setBaseFile(new java.io.File("."));
            files = m;
        }
        return files;
    }
}
