package com.groupunix.drivewireserver.dwexceptions;

public class DWUnsupportedCommOperationException extends Exception
{
	private static final long serialVersionUID = 1L;
	
	public DWUnsupportedCommOperationException(String msg)
	{
		super(msg);
	}
}
