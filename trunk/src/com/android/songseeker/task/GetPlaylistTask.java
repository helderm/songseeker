package com.android.songseeker.task;

import android.os.Handler;

import com.android.songseeker.comm.EchoNestComm;
import com.android.songseeker.comm.ServiceCommException;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;

public class GetPlaylistTask extends Task {

	private PlaylistParams plp;
	private Handler handler;
	
	public GetPlaylistTask(PlaylistParams plp, Handler handler){
		this.plp = plp;
		this.handler = handler;
	}
	
	@Override
	public Playlist call(){
		
		Playlist playlist = null;
		
		
		try {
			playlist = EchoNestComm.getComm().createStaticPlaylist(plp);
		} catch (ServiceCommException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return playlist;	
	}
	
	public void setPlaylistParam(PlaylistParams plp){
		this.plp = plp;
	}	
	
	
}
