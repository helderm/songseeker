package com.seekermob.songseeker.comm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.util.Log;

import com.echonest.api.v4.Artist;
import com.echonest.api.v4.ArtistParams;
import com.echonest.api.v4.Biography;
import com.echonest.api.v4.EchoNestAPI;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.News;
import com.echonest.api.v4.Params;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.Song;
import com.echonest.api.v4.SongParams;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceID;
import com.seekermob.songseeker.util.Util;

public class EchoNestComm {
	private static EchoNestComm comm = new EchoNestComm();
	private static EchoNestAPI en;

	public static final String SEVEN_DIGITAL = "7digital-US";
	public static final String RDIO = "rdio-us-streaming";

	private static int MAX_WS_RETRIES = 5;
	private static String KEY;

	private EchoNestComm(){		
	}
	
	public static void initialize(Context c){
		KEY = c.getString(R.string.echonest_key);
	}

	public static EchoNestComm getComm(){
		if(en == null){			
			en = new EchoNestAPI(KEY);
		}
		
		return comm;
	}

	public Playlist createStaticPlaylist(PlaylistParams plp) throws ServiceCommException{
		Playlist pl;
		int tries = 1;

		while(true){		
			try{				
				
				//lame solution to avoid returning the same playlist in the cache after refreshing
				Random rand = new Random();				
				plp.setArtistStartYearAfter(rand.nextInt(1900));
				
				pl = en.createStaticPlaylist(plp);
				
				if(pl == null)
					throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.REQ_FAILED);
				
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
					Log.e(Util.APP, "EN createStaticPlaylist failed!", e);
					throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.IO);				
				}
			} catch(Exception e){
				throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);
			}catch(NoSuchMethodError e){
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

		return pl;
	}

	public Song getSongs(SongParams sp) throws ServiceCommException{
		List<Song> ls = null;
		
		try{			
			ls = en.getSongs(sp);
		}catch (EchoNestException e) {
			treatEchoNestException(e, false);
		} catch (Exception e){
			Log.w(Util.APP, "Failed to fetch songs from EN", e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);
		}catch(NoSuchMethodError e){
			Log.e(Util.APP, "EchoNest's noSuchMethod error strikes again!", e);
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
			Log.w(Util.APP, "Failed to search for songs in EN", e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);
		}catch(NoSuchMethodError e){
			Log.e(Util.APP, "EchoNest's noSuchMethod error strikes again!", e);
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
			Log.w(Util.APP, "Failed to find artist on EN", e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);
		} catch(NoSuchMethodError e){
			Log.e(Util.APP, "EchoNest's noSuchMethod error strikes again!", e);
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
			Log.w(Util.APP, "Failed to fetch artist bio in EN", e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);
		} catch(NoSuchMethodError e){
			Log.e(Util.APP, "EchoNest's noSuchMethod error strikes again!", e);
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
			Log.w(Util.APP, "Failed to fecth artist news in EN", e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);
		} catch(NoSuchMethodError e){
			Log.e(Util.APP, "EchoNest's noSuchMethod error strikes again!", e);
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
			Log.w(Util.APP, "Failed to fecth similar artists in EN", e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);
		} catch(NoSuchMethodError e){
			Log.e(Util.APP, "EchoNest's noSuchMethod error strikes again!", e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);	
		}
		
		return similar;
	}	

	public ArrayList<Artist> getSuggestedArtists(String partialName, int numResults) throws ServiceCommException{
		ArrayList<Artist> suggestions = new ArrayList<Artist>();
		
		try{
			Params p = new Params();
			p.set("name", partialName);
			p.set("results", numResults);			
			suggestions = (ArrayList<Artist>) en.suggestArtists(p);
		} catch (EchoNestException e){
			treatEchoNestException(e, true);
		} catch(NoSuchMethodError e){
			Log.e(Util.APP, "EchoNest's noSuchMethod error strikes again!", e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);	
		}
		
		return suggestions;
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
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.ECHONEST, ServiceErr.UNKNOWN);				
		}
	}
}
