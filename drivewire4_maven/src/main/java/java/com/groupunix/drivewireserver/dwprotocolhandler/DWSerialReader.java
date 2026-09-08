package com.groupunix.drivewireserver.dwprotocolhandler;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortIOException;
import com.fazecast.jSerialComm.SerialPortTimeoutException;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

/**
 * Serial port listener: moves received bytes into the queue DWSerialDevice.comRead1() polls.
 *
 * wb 2026-09-07 changes:
 *  - the original loop read one byte at a time until jSerialComm's InputStream threw
 *    SerialPortTimeoutException ("The read operation timed out before any data was returned"):
 *    that exception was the normal end of every batch and went to stderr with printStackTrace(),
 *    once per byte the DriveWire driver's virtual-serial poll sends.  The loop now reads only what
 *    available() reports, so the timeout never fires; if it ever does it is silently the end of data.
 *  - queue.add() threw IllegalStateException("Queue full") inside the jSerialComm callback whenever
 *    the 512-byte queue overflowed; offer() drops the byte and counts it instead, with a warning.
 *  - real read failures are logged through log4j, rate-limited, instead of stderr.
 *  - the listener also subscribes to LISTENING_EVENT_PORT_DISCONNECTED and flags the loss, so that
 *    DWSerialDevice.comRead1() can leave its wait loop and the handler can reopen the port.
 */
public class DWSerialReader implements SerialPortDataListener
{
	private static final Logger logger = Logger.getLogger("DWServer.DWSerialReader");
	private static final int CHUNK = 4096;

	private ArrayBlockingQueue<Byte> queue;
	private InputStream in;
	private boolean wanttodie = false;
	private volatile boolean disconnected = false;
	private long dropped = 0;
	private long lastErrLog = 0;
	private byte[] buf = new byte[CHUNK];

	public DWSerialReader(InputStream in, ArrayBlockingQueue<Byte> q)
	{
		this.queue = q;
		this.in = in;
	}

	@Override
	public int getListeningEvents()
	{
		return SerialPort.LISTENING_EVENT_DATA_AVAILABLE | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
	}

	@Override
	public void serialEvent(SerialPortEvent event)
	{
		if (event.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED)
		{
			this.disconnected = true;
			logger.warn("serial port reports it was disconnected");
			return;
		}

		if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
			return;

		try
		{
			int avail;

			while (!wanttodie && ((avail = in.available()) > 0))
			{
				int got = in.read(buf, 0, Math.min(avail, CHUNK));

				if (got <= 0)
					break;

				for (int i = 0; i < got; i++)
				{
					if (!queue.offer(buf[i]))
					{
						dropped++;

						if ((dropped == 1) || ((dropped % 4096) == 0))
							logger.warn("serial input queue full, " + dropped + " byte(s) dropped so far (line noise, or the server is not keeping up)");
					}
				}
			}
		}
		catch (SerialPortTimeoutException e)
		{
			// nothing more to read right now: the normal end of data in jSerialComm's non-blocking mode
		}
		catch (SerialPortIOException e)
		{
			// port closed or unplugged under us
			this.disconnected = true;
			logOnce("serial read failed, port lost: " + e.getMessage());
		}
		catch (IOException e)
		{
			logOnce("serial read failed: " + e.getMessage());
		}
		catch (RuntimeException e)
		{
			logOnce("serial reader: " + e);
		}
	}

	private void logOnce(String msg)
	{
		long now = System.currentTimeMillis();

		if ((now - lastErrLog) > 5000)
		{
			lastErrLog = now;
			logger.warn(msg);
		}
	}

	public boolean isDisconnected()
	{
		return this.disconnected;
	}

	public long getDropped()
	{
		return this.dropped;
	}

	public void shutdown()
	{
		this.wanttodie = true;
	}
}
