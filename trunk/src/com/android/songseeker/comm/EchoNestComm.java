package com.android.songseeker.comm;

import java.util.List;

import android.util.Log;

import com.android.songseeker.comm.ServiceCommException.ServiceErr;
import com.android.songseeker.comm.ServiceCommException.ServiceID;
import com.android.songseeker.util.Util;
import com.echonest.api.v4.EchoNestAPI;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.Song;
import com.echonest.api.v4.SongParams;

public class EchoNestComm {
	private static final String key = "OKF60XQ3DSLHDO9CX";
	private static EchoNestComm comm = new EchoNestComm();
	private static EchoNestAPI en;
	
	public static final String SEVEN_DIGITAL = "7digital";
	public static final String RDIO = "rdio-us-streaming";
	
	private EchoNestComm(){
		en = new EchoNestAPI(key);
		en.setTraceRecvs(true);
		en.setTraceSends(true);
	}
	
	public static EchoNestComm getComm(){
		return comm;
	}
	
	public Playlist createStaticPlaylist(PlaylistParams plp) throws ServiceCommException{
		Playlist pl;
		
		Log.i(Util.APP, "Creating static playlist on EchoNest...");
				
		try{
			pl = en.createStaticPlaylist(plp);
		}catch(EchoNestException e){
			switch(e.getCode()){
			case 4:
				throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.TRY_LATER);				
			case 5:
				throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.ID_NOT_FOUND);				
			default:	
				Log.e(Util.APP, "createStaticPlaylist failed!", e);
				throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.IO);				
			}
			//TODO treat exception of unknown identifier 
		}
		
		Log.i(Util.APP, "Creating static playlist finished!");
		
		return pl;
	}
	
	public Song getSongs(SongParams sp) throws ServiceCommException{
		List<Song> ls;
		
		try{			
			ls = en.getSongs(sp);
		}catch(Exception e){
			Log.e(Util.APP, "identifySong err!", e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.IO);	
		}
		
		return ls.get(0);
	}
}
