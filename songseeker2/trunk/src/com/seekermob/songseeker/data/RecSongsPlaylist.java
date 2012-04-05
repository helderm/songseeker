package com.seekermob.songseeker.data;

import java.util.ArrayList;

import com.seekermob.songseeker.R;
import android.app.ListActivity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.Song;
import com.seekermob.songseeker.comm.EchoNestComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.util.Util;

public class RecSongsPlaylist {
	private static RecSongsPlaylist obj = new RecSongsPlaylist();

	private static ArrayList<SongInfo> songs = null;
	private static ArrayList<PlaylistListener> listeners = null;

	private static final int MAX_QUERY_RETRIES = 10;
	
	private RecSongsPlaylist() {}

	public static RecSongsPlaylist getInstance(){
		return obj;
	}

	public void setSongs(ArrayList<SongInfo> s){
		songs = s;
	}

	public ArrayList<SongInfo> getPlaylist(){
		return songs;
	}

	public void clearPlaylist(){
		if(songs != null)
			songs.clear();
		songs = null;
	}

	public boolean isEmpty(){
		if(songs == null || songs.size() == 0)
			return true;

		return false;
	}

	/** Get a new playlist methods*/
	public void getPlaylist(PlaylistParams plp, Context c, int progressDiag){
		new GetPlaylistTask(plp, c, progressDiag).execute();		
	}

	private class GetPlaylistTask extends AsyncTask<Void, Void, Void>{
		private String err = null;
		private PlaylistParams playlistParams;
		private Context context;
		//private int progressDiag;		

		public GetPlaylistTask(PlaylistParams plp, Context c, int pd) {
			playlistParams = plp;
			context = c;
			//progressDiag = pd;
		}

		@Override
		protected void onPreExecute() {
			//activity.showDialog(progressDiag);
		}

		@Override
		protected Void doInBackground(Void... params) {
			Playlist pl = null;
			int retries = MAX_QUERY_RETRIES;

			try {
				pl = EchoNestComm.getComm().createStaticPlaylist(playlistParams);
			} catch (ServiceCommException e) {
				err = e.getMessage();
				return null;
			}

			//convert the songs from EN to the parcel format
			//release and artist info will be almost empty until we query 7digital
			ArrayList<SongInfo> songsInfo = new ArrayList<SongInfo>();			
			for(Song song : pl.getSongs()){
				
				SongInfo songInfo = new SongInfo();
				
				try {
					
					songInfo.name = song.getReleaseName();
					songInfo.artist.name = song.getArtistName();
					
					try{
						songInfo.id = song.getString("tracks[0].foreign_id").split(":")[2];
						songInfo.previewUrl = song.getString("tracks[0].preview_url");	
						songInfo.release.image = song.getString("tracks[0].release_image");	
					}catch (IndexOutOfBoundsException e) { /*ignore and try to recover later*/}					
					
					//if id differs from null but we need some data, try fetching from 7digital
					if(songInfo.id != null && (songInfo.name == null || songInfo.artist.name == null ||
						(songInfo.release.image == null && retries > 0))){
							
						songInfo = SevenDigitalComm.getComm().querySongDetails(songInfo.id, songInfo.name, songInfo.artist.name, context);	
						retries--;
					}						
					
					//if the id is null, try using 7digital search with the name of songs and artist
					if(((songInfo.id == null && retries > 0) || (songInfo.release.image == null && retries > 0)) && 
						(songInfo.name != null && songInfo.artist.name != null)){
						
						songInfo = SevenDigitalComm.getComm().querySongSearch(songInfo.name, songInfo.artist.name);	
						retries--;
					}
					
					//final assert
					if(songInfo.id == null || songInfo.name == null || songInfo.artist.name == null){						
						throw new Exception("Failed to fetch a song from EN");						
					}					
					
					songsInfo.add(songInfo);					
				}catch (Exception e){
					Log.i(Util.APP, "Failed to fetch a song from EN, ignoring it...");
					continue;
				}
			}
			
			syncAddSongsToPlaylist(songsInfo);

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

			//activity.removeDialog(progressDiag);

			if(err != null){
				Toast.makeText(context, err, Toast.LENGTH_LONG).show();
				//activity.finish();
				return;
			}

			if(songs.size() == 0){
				Toast.makeText(context, R.string.playlist_empty, 
						Toast.LENGTH_LONG).show();
				//activity.finish();
				return;	    		
			}

			//notify listeners that the data changed
			notifyListeners();

			context = null; //maybe this prevents memory leakage
		}		
	}	

	/** Add songs to the playlist methods*/
	@SuppressWarnings("unchecked")
	public void addSongsToPlaylist(ArrayList<SongInfo> songsInfo, ListActivity a){		
		new GetSongInfoTask(a).execute(songsInfo);
	}

	private class GetSongInfoTask extends AsyncTask<ArrayList<SongInfo>, Void, Void>{

		private String msg = null;
		private String err = null;
		private Context context = null;

		public GetSongInfoTask(Context c) {
			context = c;
		}

		@Override
		protected void onPreExecute() {
			Toast.makeText(context, R.string.adding_songs_playlist, 
					Toast.LENGTH_SHORT).show();
		}

		@Override
		protected Void doInBackground(ArrayList<SongInfo>... params) {

			ArrayList<SongInfo> songs = new ArrayList<SongInfo>();
			
			for(SongInfo song : params[0]){
								
				//check for null data
				if(song.id == null || song.name == null || song.artist.name == null || song.release.image == null){
					continue;
				}
				
				if(song.previewUrl == null){
					try{
						song.previewUrl = SevenDigitalComm.getComm().getPreviewUrl(song.id);
					} catch(Exception e){
						continue;
					} 
				}
				
				songs.add(song);
			}				

			if(songs.size() == 0){
				err = context.getString(R.string.err_add_songs_playlist);
				return null;
			}else if(songs.size() < params[0].size()){
				msg = context.getString(R.string.some_songs_added_playlist);
			}else
				msg = context.getString(R.string.success_add_songs_playlist);				

			syncAddSongsToPlaylist(songs);

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if(err != null){
				Toast.makeText(context, err, Toast.LENGTH_SHORT).show();
				return;
			}

			Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();			
			
			//notify listeners that the data changed
			notifyListeners();
			
			context = null; //maybe this prevents memory leakage
		}		
	}	

	private synchronized void syncAddSongsToPlaylist(ArrayList<SongInfo> songList){
		if(songs == null)
			songs = new ArrayList<SongInfo>();

		songs.addAll(songList);
	}

	public void removeSongFromPlaylist(int position){
		songs.remove(position);
		notifyListeners();
	}

	//listener methods/interface
	public void registerListener(PlaylistListener listener) {
		if(listeners == null)
			listeners = new ArrayList<RecSongsPlaylist.PlaylistListener>();

		listeners.add(listener);
	}

	public void unregisterListener(PlaylistListener listener) {
		listeners.remove(listener);

		if(listeners.isEmpty()){
			listeners = null;
		}
	}

	public static interface PlaylistListener{
		public void onDataChanged(ArrayList<SongInfo> playlist);
	}
	
	private void notifyListeners(){
		
		if(listeners == null)
			return;
		
		//notify listeners that the data changed
		for(PlaylistListener listener : listeners){
			listener.onDataChanged(songs);
		}
	}
}
