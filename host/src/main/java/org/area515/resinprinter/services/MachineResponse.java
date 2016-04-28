package org.area515.resinprinter.services;


public class MachineResponse {
	private String command; // Who initiated the request
	private boolean response; // Successful = true, problems with request = false
	private String message;
	
	public MachineResponse(){command="";response=false;message="";}
	public MachineResponse(String command, boolean response, String message){
		this.command = command;
		this.response = response;
		this.message = message;
	}
	
	public String getCommand(){return command;}
	public void setCommand(String command){this.command=command;}
	
	public boolean getResponse(){return response;}
	public void setResponse(boolean response){this.response = response;}
	
	public String getMessage(){return message;}
	public void setMessage(String message){this.message = message;}
}