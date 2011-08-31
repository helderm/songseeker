package com.android.songseeker.comm;

public class ServiceCommException extends Exception {

	private static final long serialVersionUID = 1L;
	private ServiceID serv;
	private ServiceErr err;
	
	ServiceCommException(ServiceID s, ServiceErr e){
		serv = s;  
		err = e;
	}
	
	@Override
	public String getMessage(){
		return serv.getName() +": " + err.getMsg();
	}
	
	public ServiceErr getErr() {
		return err;
	}
	
	public enum ServiceID {
		ECHONEST ("Echo Nest"), 
		LASTFM ("Last.fm"), 
		GROOVESHARK ("Grooveshark"),
		RDIO ("Rdio");
		private final String name;
		
		ServiceID(String name) {
	        this.name = name;	        
	    }		
		public String getName(){
			return this.name;
		}
	}
	
	public enum ServiceErr{
		IO ("Unable to reach the service provider. Check your internet connection!"),
		ID_NOT_FOUND ("Artist not found!"),
		TRY_LATER ("Service unavailable. Try later!"),
		NOT_AUTH ("Not authorized!"),
		REQ_FAILED ("The service informed that the requested operation failed. Try later!"),
		UNKNOWN ("Unknown error!");
		
		private final String msg;
		
		ServiceErr(String msg){
			this.msg = msg;
		}	
		
		public String getMsg(){
			return this.msg;
		}		
	}
	
	
	
}
