package org.area515.resinprinter.services;

public class MachineResponse {
	
	
	public MachineResponse(){command="";response=false;message="";}
	public MachineResponse(String command, boolean response, String message){
		this.command = command;
		this.response = response;
		this.message = message;
	}
	String command; // Who initiated the request
	public void setCommand(String command){this.command=command;}
	public String getCommand(){return command;}
	
	boolean response; // Successful = true, problems with request = false
	public void setResponse(boolean response){this.response = response;}
	public boolean getResponse(){return response;}
	
	String message; // if there was something to say (like an exception) say it here
	public void setMessage(String message){this.message = message;}
	public String getMessage(){return message;}
	
	
}