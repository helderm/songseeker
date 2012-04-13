package com.seekermob.songseeker.comm;

import java.util.Collection;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceID;
import com.seekermob.songseeker.util.Util;

import de.umass.lastfm.Artist;
import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Playlist;
import de.umass.lastfm.Result;
import de.umass.lastfm.Session;
import de.umass.lastfm.User;

public class LastfmComm {
	private static LastfmComm comm = new LastfmComm();
	private static Session session = null;
	private static String sessionKey;
	private static String username;
	
	private static final String KEY = "4c2fe8fbeda6545125c50798c20bc9c0";
	private static final String SECRET = "f7d0e539a08eb21e07a9d26fa66926a8";
	private static final String PREF_SESSIONKEY = "prefs.lastfm.sessionkey";
	private static final String PREF_USERNAME = "pref.lastfm.username";
	
	//private static final String ENDPOINT = "http://ws.audioscrobbler.com/2.0/?";
	
	private LastfmComm(){}
	
	public static LastfmComm getComm(SharedPreferences settings){
		
		if(sessionKey != null)
			return comm;
		
		sessionKey = settings.getString(PREF_SESSIONKEY, null);
		username = settings.getString(PREF_USERNAME, null);		
		
		return comm;
	}
	
	public static LastfmComm getComm(){
		Caller.getInstance().setCache(null);
		return comm;
	}
	
	public void requestAuthorize(String user, String pwd, SharedPreferences settings) throws ServiceCommException{
		
		try{
			session = Authenticator.getMobileSession(user, pwd, KEY, SECRET);			
		}catch(RuntimeException e){
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);			
		}
		
		if(session == null)
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.NOT_AUTH);
		
		Editor editor = settings.edit();
		editor.putString(PREF_SESSIONKEY, session.getKey());
		editor.putString(PREF_USERNAME, session.getUsername());
		editor.commit();		
	}
	
	public Collection<Playlist> getUserPlaylists() throws ServiceCommException{
		if(session == null)
			return null;
		
		Collection<Playlist> pls;
		
		try{
			pls = User.getPlaylists(session.getUsername(), KEY);
		}catch(RuntimeException e){
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);			
		}
		
		return pls;
	}
	
	public Playlist createPlaylist(String title, SharedPreferences settings) throws ServiceCommException{
		if(session == null)
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.NOT_AUTH);
		
		Playlist pl = null;
		
		try{
			pl = Playlist.create(title, "Playlist created by the Song Seeker Android app.", session);
		}catch(RuntimeException e){
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);			
		}	
		
		if(pl == null){
			Log.w(Util.APP, "Error while trying to create playlist on Last.fm!");
			cleanAuth(settings);
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.REQ_FAILED);
		}
		
		return pl;	
	}
	
	public void addToPlaylist(int playlistId, String artist, String track, SharedPreferences settings) throws ServiceCommException{
		if(session == null)
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.NOT_AUTH);
		
		Result res = null;
		try{
			res = Playlist.addTrack(playlistId, artist, track, session);
		}catch(RuntimeException e){
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);			
		}			

		if(!res.isSuccessful()){
			switch (res.getErrorCode()) {
			case 4:
			case 9:
			case 14:
				cleanAuth(settings);
				throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.NOT_AUTH);
			case 6:
				Log.w(Util.APP, "Song ["+track+" - "+artist+"] not found on Last.fm!");
				return;
			case 8:
			case 16:
			case 29:
				throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.TRY_LATER);

			default:
				Log.w(Util.APP,"Unknown error on Last.fm Playlist.getTrack()! Err=[("+res.getErrorCode()+") - "+res.getErrorMessage() +"]");
				throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.UNKNOWN);
			}
		}
	}
	
	public Collection<Artist> getTopArtists(String user) throws ServiceCommException{
		try{
			Collection<Artist> topArtists = User.getTopArtists(user, KEY);
			
			if(topArtists.isEmpty())
				throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.USER_NOT_FOUND);
			
			return topArtists;
		}catch(RuntimeException e){
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);			
		}	
	}
	
	public boolean isAuthorized(){
		if(session == null){
			if(sessionKey == null || username == null)
				return false;
			session = Session.createSession(KEY, SECRET, sessionKey, username, false);
		}
		
		return true;	
	}
	
	public boolean hasUsername(){
		if(session == null && username == null)
			return false;
		
		return true;
	}
	
	public void cleanAuth(SharedPreferences settings){
		Editor editor = settings.edit();
		editor.putString(PREF_SESSIONKEY, null);
		editor.putString(PREF_USERNAME, null);
		editor.commit();
		
		session = null;
		sessionKey = null;
		username = null;
	}
}
