package com.groupunix.drivewireserver.dwprotocolhandler;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortIOException;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

/**
 * Serial port listener: moves received bytes into the queue DWSerialDevice.comRead1() polls.
 *
 * wb 2026-09-07 changes:
 *  - queue.add() threw IllegalStateException("Queue full") inside the jSerialComm callback whenever
 *    the 512-byte queue overflowed (line noise while the machine powers up, a burst while the handler
 *    is busy); an exception thrown out of the callback silently stopped the reader.  offer() drops the
 *    byte and counts it instead, with a warning.
 *  - read failures went to stderr with printStackTrace() (the "exception error" seen when the remote
 *    machine is power-cycled); they are logged through log4j now, rate-limited.
 *  - the listener also subscribes to LISTENING_EVENT_PORT_DISCONNECTED and flags the loss, so that
 *    DWSerialDevice.comRead1() can leave its wait loop and the handler can reopen the port.
 */
public class DWSerialReader implements SerialPortDataListener
{
	private static final Logger logger = Logger.getLogger("DWServer.DWSerialReader");

	private ArrayBlockingQueue<Byte> queue;
	private InputStream in;
	private boolean wanttodie = false;
	private volatile boolean disconnected = false;
	private long dropped = 0;
	private long lastErrLog = 0;

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

		int data;

		try
		{
			while (!wanttodie && ((data = in.read()) > -1))
			{
				if (!queue.offer((byte) data))
				{
					dropped++;

					if ((dropped == 1) || ((dropped % 4096) == 0))
						logger.warn("serial input queue full, " + dropped + " byte(s) dropped so far (line noise, or the server is not keeping up)");
				}
			}
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
