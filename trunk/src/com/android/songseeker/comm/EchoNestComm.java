package com.android.songseeker.comm;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.android.songseeker.comm.ServiceCommException.ServiceErr;
import com.android.songseeker.comm.ServiceCommException.ServiceID;
import com.android.songseeker.util.Util;
import com.echonest.api.v4.Artist;
import com.echonest.api.v4.ArtistParams;
import com.echonest.api.v4.Biography;
import com.echonest.api.v4.EchoNestAPI;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.News;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.Song;
import com.echonest.api.v4.SongParams;

public class EchoNestComm {
	private static final String KEY = "OKF60XQ3DSLHDO9CX";
	private static EchoNestComm comm = new EchoNestComm();
	private static EchoNestAPI en;

	public static final String SEVEN_DIGITAL = "7digital";
	public static final String RDIO = "rdio-us-streaming";

	private static int MAX_WS_RETRIES = 5;

	private EchoNestComm(){
		en = new EchoNestAPI(KEY);
		//en.setTraceRecvs(true);
		//en.setTraceSends(true);
	}

	public static EchoNestComm getComm(){
		return comm;
	}

	public Playlist createStaticPlaylist(PlaylistParams plp) throws ServiceCommException{
		Playlist pl;
		int tries = 1;

		//Log.i(Util.APP, "Creating static playlist on EchoNest...");

		while(true){		
			try{				
				pl = en.createStaticPlaylist(plp);
				break;
			}catch(EchoNestException e){
				switch(e.getCode()){
				case -1:
					tries++;
					if(tries > MAX_WS_RETRIES){
						if(e.getMessage().contains("java.io.IOException"))
							throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.IO);
						else
							throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.REQ_FAILED);
					}

					try {
						Thread.sleep(3000);
					} catch (InterruptedException e1) {	}			

					break;
				case 4:
					tries++;				
					if(tries > MAX_WS_RETRIES){
						throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.TRY_LATER);
					}

					try {
						Thread.sleep(3000);
					} catch (InterruptedException e1) {	}	

					break;
				case 5:
					throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.ARTIST_NOT_FOUND);				
				default:	
					Log.e(Util.APP, "createStaticPlaylist failed!", e);
					throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.IO);				
				}
			} catch(NoSuchMethodError e){
				tries++;
				if(tries > MAX_WS_RETRIES){
					if(e.getMessage().contains("java.io.IOException"))
						throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.IO);
					else
						throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.REQ_FAILED);
				}

				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {	}	
			}
		}

		//Log.i(Util.APP, "Creating static playlist finished!");

		return pl;
	}

	public Song getSongs(SongParams sp) throws ServiceCommException{
		List<Song> ls = null;
		
		try{			
			ls = en.getSongs(sp);
		}catch (EchoNestException e) {
			treatEchoNestException(e, false);
		} catch (Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);
		}

		if(ls.size() <= 0)
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.SONG_NOT_FOUND);
		
		return ls.get(0);
	}

	public Song searchSongs(SongParams sp) throws ServiceCommException{
		List<Song> ls = null;
		
		try{			
			ls = en.searchSongs(sp);
		}catch (EchoNestException e) {
			treatEchoNestException(e, false);
		} catch (Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);
		}

		if(ls.size() <= 0)
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.SONG_NOT_FOUND);
		
		return ls.get(0);
	}
	
	public Artist getArtist(String name) throws ServiceCommException{
		Artist artist = null;
		
		try{			
			artist = en.newArtistByName(name);
		}catch (EchoNestException e) {
			treatEchoNestException(e, true);
		} catch (Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);
		}
		
		return artist;
	}
	
	public Biography getArtistBioFromBucket(String id) throws ServiceCommException {
		Biography bio = null;
		
		try {
			Artist artist = en.newArtistByID("7digital-US:artist:"+id);
			bio = artist.getBiographies(0, 1, "cc-by-sa").get(0);
		} catch (EchoNestException e) {
			treatEchoNestException(e, true);
		} catch (Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);
		}
		
		return bio;
	}
	
	public ArrayList<News> getArtistNewsFromBucket(String id, int count, boolean isHighRelevance) throws ServiceCommException{
		ArrayList<News> news = new ArrayList<News>();
		
		try {
			Artist artist = en.newArtistByID("7digital-US:artist:"+id);
			news = artist.getNews(0, 10, isHighRelevance);
		} catch (EchoNestException e) {
			treatEchoNestException(e, true);
		} catch (Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);
		}
		
		return news;
	}

	public ArrayList<Artist> getSimilarArtistsFromBucket(String id, int count) throws ServiceCommException{
		ArrayList<Artist> similar = new ArrayList<Artist>();
		
		try {
			ArtistParams ap = new ArtistParams();						
			ap.addIDSpace(EchoNestComm.SEVEN_DIGITAL);
		    ap.setID("7digital-US:artist:"+id);
			ap.setLimit(true);
			similar = (ArrayList<Artist>) en.getSimilarArtists(ap);
			
		} catch (EchoNestException e) {
			treatEchoNestException(e, true);
		} catch (Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);
		}
		
		return similar;
	}	

	private void treatEchoNestException(EchoNestException e, boolean isArtist) throws ServiceCommException{

		switch(e.getCode()){
		case -1:
			if(e.getMessage().contains("java.io.IOException"))
				throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.IO);
			else
				throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.REQ_FAILED);
		case 4:
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.TRY_LATER);			
		case 5:
			if(isArtist)
				throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.ARTIST_NOT_FOUND);
			else
				throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.SONG_NOT_FOUND);
		default:	
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);				
		}
	}
}
