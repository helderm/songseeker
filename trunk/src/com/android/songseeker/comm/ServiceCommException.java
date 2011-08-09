package com.android.songseeker.comm;

public class ServiceCommException extends Exception {
	private ServiceID serv;
	private ServiceErr err;
	
	ServiceCommException(ServiceID s, ServiceErr e){
		serv = s;  
		err = e;
	}
	
	@Override
	public String getMessage(){
		return serv.getName() +" call failed: " + err.getMsg();
	}
	
	public enum ServiceID {
		ECHONEST ("Echo Nest"), 
		LASTFM ("Last.fm"), 
		GROOVESHARK ("Grooveshark");
		private final String name;
		
		ServiceID(String name) {
	        this.name = name;	        
	    }		
		public String getName(){
			return this.name;
		}
	}
	
	public enum ServiceErr{
		IO ("Communication error");
		private final String msg;
		
		ServiceErr(String msg){
			this.msg = msg;
		}		
		public String getMsg(){
			return this.msg;
		}		
	}
	
	
	
}
