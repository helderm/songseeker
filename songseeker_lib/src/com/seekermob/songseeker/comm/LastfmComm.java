package com.seekermob.songseeker.comm;

import java.util.ArrayList;
import java.util.Collection;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceID;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.PlaylistInfo;
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
	
	private static String key;
	private static String secret;
	private static final String PREF_SESSIONKEY = "prefs.lastfm.sessionkey";
	private static final String PREF_USERNAME = "pref.lastfm.username";
	
	private LastfmComm(){}
	
	public static void initialize(Context c){
		key = c.getString(R.string.lastfm_key);
		secret = c.getString(R.string.lastfm_secret);		
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		sessionKey = settings.getString(PREF_SESSIONKEY, null);
		username = settings.getString(PREF_USERNAME, null);	
	}
	
	public static LastfmComm getComm(){
		Caller.getInstance().setCache(null);
		return comm;
	}
	
	public void requestAuthorize(String user, String pwd, Context c) throws ServiceCommException{
		
		try{
			session = Authenticator.getMobileSession(user, pwd, key, secret);			
		}catch(RuntimeException e){
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);			
		}
		
		if(session == null)
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.NOT_AUTH);
		
		Editor editor = PreferenceManager.getDefaultSharedPreferences(c).edit();
		editor.putString(PREF_SESSIONKEY, session.getKey());
		editor.putString(PREF_USERNAME, session.getUsername());
		editor.commit();		
	}
	
	public ArrayList<PlaylistInfo> getUserPlaylists() throws ServiceCommException{
		if(session == null)
			return null;
		
		ArrayList<PlaylistInfo> playlists = new ArrayList<PlaylistInfo>();
		Collection<Playlist> pls;
		
		try{
			pls = User.getPlaylists(session.getUsername(), key);
		}catch(RuntimeException e){
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);			
		}
		
		for(Playlist pl : pls){
			PlaylistInfo playlist = new PlaylistInfo();
			playlist.id = Integer.toString(pl.getId());
			playlist.name = pl.getTitle();
			playlist.numSongs = Integer.toString(pl.getSize());
			playlists.add(playlist);
		}
		
		return playlists;
	}
	
	public String createPlaylist(String title, Context c) throws ServiceCommException{
		if(session == null)
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.NOT_AUTH);
		
		Playlist playlist = null;
		
		try{
			playlist = Playlist.create(title, c.getString(R.string.playlist_created_by), session);
		}catch(NullPointerException e){
			//jar error, trying here to recover from it
			ArrayList<PlaylistInfo> pls = getUserPlaylists();
			
			//will return the first playlist with the same name, but since lastf allows playlists with the same name,
			//it may not be the same
			for(PlaylistInfo pl : pls){
				if(!pl.name.equalsIgnoreCase(title))
					continue;
				
				return pl.id;
			}
			
		}catch(RuntimeException e){
			Log.i(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);			
		}	
		
		if(playlist == null){
			Log.w(Util.APP, "Error while trying to create playlist on Last.fm!");
			cleanAuth(c);
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.REQ_FAILED);
		}
		
		return Integer.toString(playlist.getId());	
	}
	
	public void addToPlaylist(String playlistId, String artist, String track, Context c) throws ServiceCommException{
		if(session == null)
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.NOT_AUTH);
		
		Result res = null;
		try{
			res = Playlist.addTrack(Integer.parseInt(playlistId), artist, track, session);
		}catch(RuntimeException e){
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);			
		}			

		if(!res.isSuccessful()){
			switch (res.getErrorCode()) {
			case 4:
			case 9:
			case 14:
				cleanAuth(c);
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
	
	public ArrayList<ArtistInfo> getTopArtists(String user) throws ServiceCommException{
		try{
			Collection<Artist> artists = User.getTopArtists(user, key);
			
			if(artists.isEmpty())
				throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.USER_NOT_FOUND);
			
			ArrayList<ArtistInfo> topArtists = new ArrayList<ArtistInfo>();
			for(Artist a : artists){
				ArtistInfo artist = new ArtistInfo();
				artist.name = a.getName();
				topArtists.add(artist);
			}
			
			return topArtists;
		}catch(RuntimeException e){
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);			
		}	
	}
	
	public boolean isAuthorized(){
		if(session == null){
			if(sessionKey == null || username == null)
				return false;
			session = Session.createSession(key, secret, sessionKey, username, false);
		}
		
		return true;	
	}
	
	public boolean hasUsername(){
		if(session == null && username == null)
			return false;
		
		return true;
	}
	
	public void cleanAuth(Context c){
		Editor editor = PreferenceManager.getDefaultSharedPreferences(c).edit();
		editor.putString(PREF_SESSIONKEY, null);
		editor.putString(PREF_USERNAME, null);
		editor.commit();
		
		session = null;
		sessionKey = null;
		username = null;
	}
}
