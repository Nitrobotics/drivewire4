package com.groupunix.drivewireserver.dwprotocolhandler;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

import java.io.IOException;
import java.io.InputStream;
import java.util.TooManyListenersException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.groupunix.drivewireserver.DWDefs;
import com.groupunix.drivewireserver.dwexceptions.DWCommTimeOutException;
import com.groupunix.drivewireserver.dwexceptions.DWPortNotValidException;
import com.groupunix.drivewireserver.dwexceptions.DWPortInUseException;
import com.groupunix.drivewireserver.dwexceptions.DWUnsupportedCommOperationException;

public class DWSerialDevice implements DWProtocolDevice
{
	private static final Logger logger = Logger.getLogger("DWServer.DWSerialDevice");
	
	private SerialPort serialPort = null;

	private boolean bytelog = false;
	private String device;
	private DWProtocol dwProto;
	private boolean DATurboMode = false; 
	private boolean xorinput = false;
	private long WriteByteDelay = 0;
	private long ReadByteWait = 200;
	private byte[] prefix;
	private long readtime;

	private ArrayBlockingQueue<Byte> queue;

	private DWSerialReader evtlistener;
	private boolean portLostLogged = false;

	private boolean ProtocolFlipOutputBits;

	private boolean ProtocolResponsePrefix;

	
	public DWSerialDevice(DWProtocol dwProto) throws DWPortNotValidException, DWPortInUseException, DWUnsupportedCommOperationException, TooManyListenersException, IOException
	{
		
		this.dwProto = dwProto;
		
		this.device = dwProto.getConfig().getString("SerialDevice");
		
		prefix = new byte[1];
		prefix[0] = (byte) 0xC0;
		
		logger.debug("init " + device + " for handler #" + dwProto.getHandlerNo() + " (logging bytes: " + bytelog + "  xorinput: " + xorinput +")");
		
		connect(device);
				
	}
	
	
	public boolean connected()
	{
		if (this.serialPort != null && this.serialPort.isOpen())
		{
			return(true);
		}
		else
		{
			return(false);
		}
	}

	

	public void close()
	{
		
		if (this.serialPort != null)
		{
			logger.debug("closing serial device " +  device + " in handler #" + dwProto.getHandlerNo());
			
			if (this.evtlistener != null)
			{
				this.evtlistener.shutdown();
				serialPort.removeDataListener();
			}
			
			serialPort.closePort();
			serialPort = null;
			
		}
	}

	
	public void shutdown()
	{
		this.close();
				
	}

	
	public void reconnect() throws DWUnsupportedCommOperationException, TooManyListenersException, IOException
	{
		if (this.serialPort != null)
		{
			setSerialParams(serialPort);               
 			
			if (this.evtlistener != null)
			{
				this.serialPort.removeDataListener();
			}
			
			this.queue = new ArrayBlockingQueue<Byte>(512);
			
			this.evtlistener = new DWSerialReader(serialPort.getInputStream(), queue);
			
			if (!serialPort.addDataListener(this.evtlistener))
				throw new TooManyListenersException("Could not add serial port data listener");
		}
	}
	
	private void connect(String portName) throws DWPortNotValidException, DWPortInUseException, DWUnsupportedCommOperationException, TooManyListenersException, IOException
	{
		logger.debug("attempting to open device '" + portName + "'");
		
		// Find the port by name.
		// jSerialComm can throw SerialPortInvalidPortException for descriptors that
		// are not valid on the current platform (e.g. /dev/ttyS0 on macOS). Treat
		// that as an invalid port so the handler can continue running.
		final SerialPort port;
		try
		{
			port = SerialPort.getCommPort(portName);
		}
		catch (SerialPortInvalidPortException e)
		{
			throw new DWPortNotValidException("Invalid port descriptor: " + portName);
		}
		catch (RuntimeException e)
		{
			// Be conservative: any unexpected runtime failure creating the port object
			// is treated as an invalid port.
			throw new DWPortNotValidException("Unable to create port object for: " + portName);
		}
		
		if (port == null)
		{
			throw new DWPortNotValidException("Port not found: " + portName);
		}
		
		// Try to open the port
		if (!port.openPort())
		{
			// Check if it's in use or just failed
			throw new DWPortInUseException("Could not open port: " + portName);
		}
		
		serialPort = port;
		reconnect();
		
		logger.info("opened serial device " + portName);
	}
	
	
	private void setSerialParams(SerialPort sport) throws DWUnsupportedCommOperationException
	{
		int rate;
		int parity = SerialPort.NO_PARITY;
		int stopbits = SerialPort.ONE_STOP_BIT;
		int databits = 8;
		
		// mode vars
		
		this.WriteByteDelay = this.dwProto.getConfig().getLong("WriteByteDelay", 0);
		this.ReadByteWait = this.dwProto.getConfig().getLong("ReadByteWait", 200);
		this.ProtocolFlipOutputBits = this.dwProto.getConfig().getBoolean("ProtocolFlipOutputBits", false);
		this.ProtocolResponsePrefix = dwProto.getConfig().getBoolean("ProtocolResponsePrefix", false);
		this.xorinput = dwProto.getConfig().getBoolean("ProtocolXORInputBits", false);
		this.bytelog = dwProto.getConfig().getBoolean("LogDeviceBytes", false);
		
		// serial params
		
		rate = dwProto.getConfig().getInt("SerialRate", 115200);
		
		
		if (dwProto.getConfig().containsKey("SerialStopbits"))
		{
			String sb =  dwProto.getConfig().getString("SerialStopbits");
			
			if (sb.equals("1"))
				stopbits = SerialPort.ONE_STOP_BIT;
			else if (sb.equals("1.5"))
				stopbits = SerialPort.ONE_POINT_FIVE_STOP_BITS;
			else if (sb.equals("2"))
				stopbits = SerialPort.TWO_STOP_BITS;
			
		}
		
		if (dwProto.getConfig().containsKey("SerialParity"))
		{
			String p = dwProto.getConfig().getString("SerialParity");
			
			if (p.equals("none"))
				parity = SerialPort.NO_PARITY;
			else if (p.equals("even"))
				parity = SerialPort.EVEN_PARITY;
			else if (p.equals("odd"))
				parity = SerialPort.ODD_PARITY;
			else if (p.equals("mark"))
				parity = SerialPort.MARK_PARITY;
			else if (p.equals("space"))
				parity = SerialPort.SPACE_PARITY;
					
		}
		
		int flow = SerialPort.FLOW_CONTROL_DISABLED;
		
		if (dwProto.getConfig().getBoolean("SerialFlowControl_RTSCTS_IN", false))
			flow = flow | SerialPort.FLOW_CONTROL_RTS_ENABLED;
		
		if (dwProto.getConfig().getBoolean("SerialFlowControl_RTSCTS_OUT", false))
			flow = flow | SerialPort.FLOW_CONTROL_CTS_ENABLED;
		
		if (dwProto.getConfig().getBoolean("SerialFlowControl_XONXOFF_IN", false))
			flow = flow | SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED;
		
		if (dwProto.getConfig().getBoolean("SerialFlowControl_XONXOFF_OUT", false))
			flow = flow | SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED;
		
		// jSerialComm returns a boolean for flow-control setup.
		if (!sport.setFlowControl(flow))
			throw new DWUnsupportedCommOperationException("Failed to set flow control");
		
		logger.debug("setting port params to " + rate + " " + databits + ":" + parity + ":" + stopbits );
		if (!sport.setComPortParameters(rate, databits, stopbits, parity))
			throw new DWUnsupportedCommOperationException("Failed to set port parameters");
		
		if (dwProto.getConfig().containsKey("SerialDTR"))
		{
			if (dwProto.getConfig().getBoolean("SerialDTR", false))
				sport.setDTR();
			else
				sport.clearDTR();
			logger.debug("setting port DTR to " + dwProto.getConfig().getBoolean("SerialDTR", false));
		}
		
		if (dwProto.getConfig().containsKey("SerialRTS"))
		{
			if (dwProto.getConfig().getBoolean("SerialRTS", false))
				sport.setRTS();
			else
				sport.clearRTS();
			logger.debug("setting port RTS to " + dwProto.getConfig().getBoolean("SerialRTS", false));
		}
		
		
	}
	
	
	public int getRate()
	{
		if (this.serialPort != null)
			return(this.serialPort.getBaudRate());
		return -1;
	}
	

	
	
	public void comWrite(byte[] data, int len, boolean pfix)
	{	
		try 
		{
			if ((this.serialPort == null) || !this.serialPort.isOpen())
			{
				logger.debug("write of " + len + " byte(s) dropped: serial device not open");
				return;
			}
			if (this.ProtocolFlipOutputBits || this.DATurboMode) 
				data = DWUtils.reverseByteArray(data);
				
			
			if (this.WriteByteDelay > 0)
			{
				for (int i = 0;i< len;i++)
				{
					comWrite1(data[i],pfix);
				}
			}
			else
			{
				if (pfix && (this.ProtocolResponsePrefix || this.DATurboMode))
				{
					byte[] out = new byte[this.prefix.length + len];
					System.arraycopy(this.prefix, 0, out, 0, this.prefix.length);
					System.arraycopy(data, 0, out, this.prefix.length, len);
					
					serialPort.getOutputStream().write(out);
				}
				else
				{
					serialPort.getOutputStream().write(data, 0, len);
				}
				
				
				// extreme cases only
				
				if (bytelog)
				{
					String tmps = new String();
					
					for (int i = 0;i< len;i++)
					{
						tmps += " " + (int)(data[i] & 0xFF);
					}
					
					logger.debug("WRITE " + len + ":" + tmps);
					
				}
			}
		} 
		catch (IOException e) 
		{
			// problem with comm port, bail out
			logger.error(e.getMessage());
			
		}
	}	
	
	


	public void comWrite1(int data, boolean pfix)
	{
		
		
		try 
		{
			if ((this.serialPort == null) || !this.serialPort.isOpen())
			{
				logger.debug("write dropped: serial device not open");
				return;
			}
			if (this.ProtocolFlipOutputBits || this.DATurboMode) 
				data = DWUtils.reverseByte(data);
				
			if (this.WriteByteDelay > 0)
			{
				try
				{
					Thread.sleep(this.WriteByteDelay);
				} 
				catch (InterruptedException e)
				{
					logger.warn("interrupted during writebytedelay");
				}
			}
			
			if (pfix && (this.ProtocolResponsePrefix || this.DATurboMode))
			{
				byte[] out = new byte[this.prefix.length + 1];
				out[out.length - 1] = (byte)data;
				System.arraycopy(this.prefix, 0, out, 0, this.prefix.length);
				
				serialPort.getOutputStream().write(out);
			}
			else
			{
				serialPort.getOutputStream().write((byte) data);
			}
			
			if (bytelog)
				logger.debug("WRITE1: " + (0xFF & data));
			
		} 
		catch (IOException e) 
		{
			// problem with comm port, bail out
			logger.error(e.getMessage());
			
		}
	}
	
	
	
	public byte[] comRead(int len) throws IOException, DWCommTimeOutException 
	{
		byte[] buf = new byte[len];
		
		for (int i = 0;i<len;i++)
		{
			buf[i] = (byte) comRead1(true, false);
		}
		
		if (this.bytelog)
		{
			String tmps = new String();
			
			for (int i = 0;i< buf.length;i++)
			{
				tmps += " " + (int)(buf[i] & 0xFF);
			}
		
			logger.debug("READ " + len + ": " + tmps);
		}
		
		return(buf);
	}
	
	
	public int comRead1(boolean timeout) throws IOException, DWCommTimeOutException 
	{
		return comRead1(timeout, true);
	}
	
	
	public int comRead1(boolean timeout, boolean blog) throws IOException, DWCommTimeOutException 
	{
		
		int res = -1;
		
		try
		{
			while ((res == -1) && (this.serialPort != null)) 
			{
				// wb 2026-09-07: a port that closed under us (USB pod power-cycled with the machine, cable pulled)
				// kept this loop polling an empty queue forever, so the handler never reopened the device and
				// DriveWire stayed dead until the server was restarted.  Leave instead: the handler's "device
				// unavailable" path retries the open every DeviceFailRetryTime ms; an op in flight gets a timeout.
				if (!this.serialPort.isOpen() || ((this.evtlistener != null) && this.evtlistener.isDisconnected()))
				{
					if (!portLostLogged)
					{
						logger.warn("serial device " + device + " is no longer open (unplugged or power-cycled?), will retry the open");
						portLostLogged = true;
					}
					if (timeout)
						throw (new DWCommTimeOutException("serial device " + device + " lost"));
					return -1;
				}
				long starttime = System.currentTimeMillis();
				Byte read = queue.poll(this.ReadByteWait, TimeUnit.MILLISECONDS);
				this.readtime += System.currentTimeMillis() - starttime;
				
				if (read != null)
					res = 0xFF & read;
				else if (timeout)
				{
					throw (new DWCommTimeOutException("No data in " + this.ReadByteWait + " ms"));
				}
				
			}
		} 
		catch (InterruptedException e)
		{
			logger.debug("interrupted in serial read");
		}
		
		if (this.xorinput )
			res = res ^ 0xFF;
		
		if (blog && this.bytelog)
			logger.debug("READ1: " + res);
		
		return res;
	}


	@Override
	public String getDeviceName() 
	{
		if (this.serialPort != null)
			return(this.serialPort.getSystemPortName());
		
		return null;
	}


	@Override
	public String getDeviceType() 
	{
		return("serial");
	}


	public void enableDATurbo() throws DWUnsupportedCommOperationException
	{
		// valid port, not already turbo
		if ((this.serialPort != null) && !this.DATurboMode)
		{
			// change to 2x instead of hardcoded
			if ((this.serialPort.getBaudRate() >= DWDefs.COM_MIN_DATURBO_RATE) && ((this.serialPort.getBaudRate() <= DWDefs.COM_MAX_DATURBO_RATE)))
			{
				if (!this.serialPort.setComPortParameters(
					this.serialPort.getBaudRate() * 2, 
					8, 
					SerialPort.TWO_STOP_BITS, 
					SerialPort.NO_PARITY
				))
				{
					throw new DWUnsupportedCommOperationException("Failed to switch to DATurbo baud rate");
				}
				this.DATurboMode = true;
			}
		}
	}
	
	public long getReadtime()
	{
		return this.readtime;
	}
	
	public void resetReadtime()
	{
		this.readtime = 0;
	}


	public SerialPort getSerialPort()
	{
		return this.serialPort;
	}


	@Override
	public String getClient() 
	{
		return null;
	}


	@Override
	public InputStream getInputStream() 
	{
		 return new InputStream() 
		 {
             private boolean endReached = false;

         	@Override
             public int read() throws IOException 
             {
                 try {
                     if (endReached)
                         return -1;
                         
                     Byte value = queue.take();
                     if (value == null) 
                     {
                    	
                         throw new IOException(
                                 "Timeout while reading from the queue-based input stream");
                     }

                     endReached = (value.intValue() == -1);
                     return value;
                 } catch (InterruptedException ie) {
                     throw new IOException(
                             "Interruption occurred while writing in the queue");
                 }
             }
         };
	}
}
