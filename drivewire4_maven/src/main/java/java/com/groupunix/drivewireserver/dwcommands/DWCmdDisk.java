package com.groupunix.drivewireserver.dwcommands;


import com.groupunix.drivewireserver.DWDefs;
import com.groupunix.drivewireserver.dwprotocolhandler.DWProtocolHandler;

public class DWCmdDisk extends DWCommand {

	static final String command = "disk";
	private DWCommandList commands;
	private DWProtocolHandler dwProto;	
	
	public DWCmdDisk(DWProtocolHandler dwProto,DWCommand parent)
	{
		setParentCmd(parent);
		this.dwProto = dwProto;
		
		commands = new DWCommandList(this.dwProto, this.dwProto.getCMDCols());
		
		commands.addcommand(new DWCmdDiskShow(dwProto,this));
		commands.addcommand(new DWCmdDiskEject(dwProto,this));
		commands.addcommand(new DWCmdDiskInsert(dwProto,this));
		commands.addcommand(new DWCmdDiskReload(dwProto,this));
		commands.addcommand(new DWCmdDiskWrite(dwProto,this));
		commands.addcommand(new DWCmdDiskCreate(dwProto,this));
		commands.addcommand(new DWCmdDiskSet(dwProto,this));
		commands.addcommand(new DWCmdDiskDos(dwProto,this));
		// testing only, little point
		//commands.addcommand(new DWCmdDiskDump(dwProto,this));
	}

	
	public String getCommand() 
	{
		return command;
	}

	public DWCommandResponse parse(String cmdline)
	{
		// wb 2026-09-08: no disk set until the instance has started; refuse cleanly instead of a NullPointerException
		if (dwProto.getDiskDrives() == null)
			return(new DWCommandResponse(false, DWDefs.RC_INSTANCE_NOT_READY, "The server instance is not running, so it has no disk drives yet. Start it in the Instance Manager (or enable its AutoStart) before mounting disks."));
		if (cmdline.length() == 0)
		{
			return(new DWCommandResponse(this.commands.getShortHelp()));
		}
		return(commands.parse(cmdline));
	}

	public DWCommandList getCommandList()
	{
		return(this.commands);
	}



	public String getShortHelp() 
	{
		return "Manage disks and disksets";
	}


	public String getUsage() 
	{
		return "dw disk [command]";
	}
	
	public boolean validate(String cmdline) 
	{
		return(commands.validate(cmdline));
	}
	
}
