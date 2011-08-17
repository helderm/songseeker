package com.android.songseeker.comm;

import android.util.Log;

import com.android.songseeker.comm.ServiceCommException.ServiceErr;
import com.android.songseeker.comm.ServiceCommException.ServiceID;
import com.echonest.api.v4.EchoNestAPI;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;

public class EchoNestComm {
	private static final String key = "OKF60XQ3DSLHDO9CX";
	private static EchoNestComm comm = new EchoNestComm();
	private static EchoNestAPI en;
	
	public static final String SEVEN_DIGITAL = "7digital";
	public static final String RDIO = "id:rdio-us-streaming";
	
	private EchoNestComm(){
		en = new EchoNestAPI(key);
	}
	
	public static EchoNestComm getComm(){
		return comm;
	}
	
	public Playlist createStaticPlaylist(PlaylistParams plp) throws ServiceCommException{
		Playlist pl;
		
		try{
			pl = en.createStaticPlaylist(plp);
		}catch(EchoNestException e){
			switch(e.getCode()){
			case 4:
				throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.TRY_LATER);				
			case 5:
				throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.ID_NOT_FOUND);				
			default:	
				Log.e("SongSeeker", "createStaticPlaylist failed!", e);
				throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.IO);				
			}
			//TODO treat exception of unknown identifier 
		}
		
		return pl;
	}
}
