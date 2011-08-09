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
			Log.e("SongSeeker", "createStaticPlaylist failed!", e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.IO);			
		}
		
		return pl;
	}
}
