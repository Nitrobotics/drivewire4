package com.groupunix.drivewireserver.dwcommands;

import com.fazecast.jSerialComm.SerialPort;

import com.groupunix.drivewireserver.DWDefs;
import com.groupunix.drivewireserver.dwprotocolhandler.DWProtocol;


public class DWCmdServerShowSerial extends DWCommand {



	DWCmdServerShowSerial(DWProtocol dwProto, DWCommand parent)
	{

		setParentCmd(parent);
	}
	
	public String getCommand() 
	{
		return "serial";
	}


	
	public String getShortHelp() 
	{
		return "Show serial device information";
	}


	public String getUsage() 
	{
		return "dw server show serial";
	}

	public DWCommandResponse parse(String cmdline) 
	{
		String text = new String();
		
		text += "Server serial devices:\r\n\r\n";
		
		// jSerialComm - get all comm ports
		SerialPort[] ports = SerialPort.getCommPorts();
	        
		for (SerialPort port : ports)
		{
			try
			{
				text += port.getSystemPortName() + "  ";
				
				// Try to open the port to get info
				if (port.openPort())
				{
					text += port.getBaudRate() + " bps  ";
					text += port.getNumDataBits();
					
					switch(port.getParity())
					{
						case SerialPort.NO_PARITY:
							text += "N";
							break;
							
						case SerialPort.EVEN_PARITY:
							text += "E";
							break;
						
						case SerialPort.MARK_PARITY:
							text += "M";
							break;
						
						case SerialPort.ODD_PARITY:
							text += "O";
							break;
						
						case SerialPort.SPACE_PARITY:
							text += "S";
							break;
							
					}
					
					text += port.getNumStopBits();
					
					int flowMode = port.getFlowControlSettings();
					if (flowMode == SerialPort.FLOW_CONTROL_DISABLED)
						text += "  No flow control  ";
					else
					{
						text += "  ";
						
						if ((flowMode & SerialPort.FLOW_CONTROL_RTS_ENABLED) != 0)
							text += "RTS  ";
						
						if ((flowMode & SerialPort.FLOW_CONTROL_CTS_ENABLED) != 0)
							text += "CTS  ";
						
						if ((flowMode & SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED) != 0)
							text += "In: XOn/XOff  ";
						
						if ((flowMode & SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED) != 0)
							text += "Out: XOn/XOff  ";
					}
					
					
					text += " CD:" + yn(port.getDCD());
					
					text += " CTS:" + yn(port.getCTS());
					
					text += " DSR:" + yn(port.getDSR());
					
					text += " DTR:" + yn(port.getDTR());
					
					text += " RTS:" + yn(port.getRTS());
					
					
					text += "\r\n";
					
					port.closePort();
				}
				else
				{
					text += "In use or unavailable\r\n";
				}
				
			}
			catch (Exception e)
			{
				return(new DWCommandResponse(false, DWDefs.RC_SERVER_IO_EXCEPTION, "While gathering serial port info: " + e.getMessage() ));
			}
		}
		            	
		
		return(new DWCommandResponse(text));
	}
	
	private String yn(boolean cd) {
		if (cd)
			return "Y";
		
		return "n";
	}

	public boolean validate(String cmdline) 
	{
		return(true);
	}
}
