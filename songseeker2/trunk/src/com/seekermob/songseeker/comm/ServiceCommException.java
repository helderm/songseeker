package com.seekermob.songseeker.comm;

import com.seekermob.songseeker.R;

import android.content.Context;

public class ServiceCommException extends Exception {

	private static final long serialVersionUID = 1L;
	private ServiceID serv;
	private ServiceErr err;
	
	public ServiceCommException(ServiceID s, ServiceErr e){
		serv = s;  
		err = e;
	}
	
	public String getMessage(Context c){
		return serv.getName() +": " + err.getMsg(c);
	}
	
	public ServiceErr getErr() {
		return err;
	}
	
	public enum ServiceID {
		ECHONEST ("Echo Nest"), 
		LASTFM ("Last.fm"), 
		GROOVESHARK ("Grooveshark"),
		RDIO ("Rdio"),
		YOUTUBE ("YouTube"),
		SEVENDIGITAL ("7Digital"),
		BANDSINTOWN ("BandsInTown");
		
		private final String name;
		
		ServiceID(String name) {
	        this.name = name;	        
	    }		
		public String getName(){
			return this.name;
		}
	}
	
	public enum ServiceErr{
		IO (R.string.exception_io),
		ARTIST_NOT_FOUND (R.string.exception_artist_not_found),
		SONG_NOT_FOUND (R.string.exception_song_not_found),
		USER_NOT_FOUND (R.string.exception_user_not_found),
		TRY_LATER (R.string.exception_try_later),
		NOT_AUTH (R.string.exception_not_auth),
		REQ_FAILED (R.string.exception_req_failed),
		UNKNOWN (R.string.exception_unknown);
		
		private final int msg;
		
		ServiceErr(int msg){
			this.msg = msg;
		}	
		
		public String getMsg(Context c){
			return c.getString(this.msg);
		}		
	}
	
	
	
}
