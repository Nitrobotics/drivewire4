package com.groupunix.drivewireserver.dwexceptions;

public class DWPortInUseException extends Exception
{
	private static final long serialVersionUID = 1L;
	
	public DWPortInUseException(String msg)
	{
		super(msg);
	}
}
