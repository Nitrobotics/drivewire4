# Local COM management and optional remote UI

Hardware connections use serial COM ports. TCP DriveWire devices, virtual modem/TCP
listeners and connections, NineServer, web browsing, update downloads, bug-report
uploads, and cloud requests are disabled in source, irrespective of saved settings.

Local UI commands and status feeds use bounded in-memory streams. No TCP listener,
localhost connection, or firewall allowance is needed for the local Instance Manager.
The Java runtime is still required to execute the application.

The `Network instance` checkbox opts into connecting the GUI to a remote DW4 server.
It defaults off and is saved in the UI configuration. Unchecking it closes all remote
UI sockets, including pending connections, and switches management back to the local
server. It does not enable Internet disk providers, web features, or inbound listeners.

Disk access uses a separate VFS manager with only local file, ZIP, JAR, and gzip
providers. Network schemes and UNC paths are rejected, including network-backed
archive URLs. Local Windows paths and escaped spaces in file URLs are supported.
External XML DTD/schema/stylesheet fetching is disabled at startup.

The regression test is `src/test/java/SerialOnlyTest.java`. It checks rejected
network paths and devices, local files and ZIP members, actual local Instance Manager
commands without sockets, and immediate remote disconnection on opt-out. Tests use a
temporary localhost peer for the opt-in case, never an external host or a COM port.

Build with `E:\cygwin64\home\taylo\wildbits-buildkit\dw4\build_dw4.bat`.
After closing the running DW4, install with the adjacent `update_dw4_jar.bat`.
That installer backs up the previous JAR and preserves configuration and disks.
Source and JAR changes do not alter an already running Java process.
