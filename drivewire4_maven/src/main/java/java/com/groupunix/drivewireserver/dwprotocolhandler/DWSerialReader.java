package com.groupunix.drivewireserver.dwprotocolhandler;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortIOException;
import com.fazecast.jSerialComm.SerialPortTimeoutException;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

/**
 * Serial port listener: moves received bytes into the queue DWSerialDevice.comRead1() polls.
 *
 * Reads only available bytes. A lost port, broken reader or overflow invalidates
 * the current transaction and asks the handler to close/reopen the native port.
 * The callback never closes its own port or spins on repeated failures.
 */
public class DWSerialReader implements SerialPortDataListener
{
	private static final Logger logger = Logger.getLogger("DWServer.DWSerialReader");
	private static final int CHUNK = 4096;

	private ArrayBlockingQueue<Byte> queue;
	private InputStream in;
	private volatile boolean wanttodie = false;
	private final AtomicBoolean disconnected = new AtomicBoolean();
	private long dropped = 0;
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
		if (wanttodie || disconnected.get())
			return;
		if ((event.getEventType() & SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) != 0)
		{
			disconnect("serial port reports it was disconnected");
			return;
		}

		if ((event.getEventType() & SerialPort.LISTENING_EVENT_DATA_AVAILABLE) == 0)
			return;

		try
		{
			int avail;

			while (!wanttodie && !disconnected.get())
			{
				avail = in.available();
				if (avail < 0)
				{
					disconnect("serial input is no longer available");
					return;
				}
				if (avail == 0)
					break;
				int got = in.read(buf, 0, Math.min(avail, CHUNK));

				if (got < 0)
				{
					disconnect("serial input reached end of stream");
					return;
				}
				if (got == 0)
					break;

				for (int i = 0; i < got; i++)
				{
					if (wanttodie || disconnected.get())
						return;
					if (!queue.offer(buf[i]))
					{
						dropped++;
						// A partial packet cannot safely become the next opcode.
						disconnect("serial input queue overflow; discarding partial transaction and reopening port");
						return;
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
			disconnect("serial read failed, port lost: " + e.getMessage());
		}
		catch (IOException e)
		{
			disconnect("serial read failed: " + e.getMessage());
		}
		catch (RuntimeException e)
		{
			disconnect("serial reader failed: " + e);
		}
	}

	void disconnect(String msg)
	{
		if (disconnected.compareAndSet(false, true))
		{
			queue.clear();
			logger.warn(msg);
		}
	}

	public boolean isDisconnected()
	{
		return this.disconnected.get();
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
