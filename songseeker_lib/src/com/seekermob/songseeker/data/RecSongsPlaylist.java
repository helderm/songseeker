package com.seekermob.songseeker.data;

import java.util.ArrayList;

import com.seekermob.songseeker.R;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.ListFragment;
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

	private ArrayList<SongInfo> songs;
	private ArrayList<PlaylistListener> listeners;
	private GetPlaylistTask task;

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
	public void getNewPlaylist(PlaylistParams plp, ListFragment lf){
		cancelTask();
		
		task = (GetPlaylistTask) new GetPlaylistTask(plp, lf).execute();		
	}

	private class GetPlaylistTask extends AsyncTask<Void, Void, Void>{
		private String err = null;
		private PlaylistParams playlistParams;
		private ListFragment listFragment;	

		public GetPlaylistTask(PlaylistParams plp, ListFragment lf) {
			playlistParams = plp;
			listFragment = lf;
		}

		@Override
		protected void onPreExecute() {
			Util.setListShown(listFragment, false);
		}

		@Override
		protected Void doInBackground(Void... params) {
			Playlist pl = null;
			int retries = MAX_QUERY_RETRIES;

			try {
				pl = EchoNestComm.getComm().createStaticPlaylist(playlistParams);
			} catch (ServiceCommException e) {
				err = e.getMessage(listFragment.getActivity());
				return null;
			}

			if(isCancelled())
				return null;
			
			//convert the songs from EN to the parcel format
			//release and artist info will be almost empty until we query 7digital
			ArrayList<SongInfo> songsInfo = new ArrayList<SongInfo>();			
			for(Song song : pl.getSongs()){
				
				if(isCancelled())
					return null;
				
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
							
						songInfo = SevenDigitalComm.getComm().querySongDetails(songInfo.id, songInfo.name, songInfo.artist.name);	
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
			
			if(isCancelled())
				return null;
			
			syncAddSongsToPlaylist(songsInfo);

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			Util.setListShown(listFragment, true);
			
			//notify listeners that the data changed
			notifyListeners();
			
			if(err != null){
				Toast.makeText(listFragment.getActivity().getApplicationContext(), err, Toast.LENGTH_LONG).show();
				listFragment = null;
				return;
			}

			if(songs.size() == 0){
				Toast.makeText(listFragment.getActivity().getApplicationContext(), R.string.playlist_empty, Toast.LENGTH_LONG).show();
				listFragment = null;
				return;	    		
			}			

			listFragment = null; //maybe this prevents memory leakage
		}		
	}	

	public boolean isTaskRunning(){
		if(task != null && task.getStatus() != AsyncTask.Status.FINISHED){
			return true;			
		}
		
		return false;
	}
	
	public void cancelTask(){
		if(!isTaskRunning())
			return;
		
		task.cancel(true);
		task = null;
	}
	
	/** Add songs to the playlist methods*/
	@SuppressWarnings("unchecked")
	public void addSongsToPlaylist(ArrayList<SongInfo> songsInfo, Context c){		
		if(songsInfo == null){
			Toast.makeText(c, R.string.no_song_found, Toast.LENGTH_SHORT).show();			
			return;
		}
		
		new AddToPlaylistTask(c).execute(songsInfo);
	}

	private class AddToPlaylistTask extends AsyncTask<ArrayList<SongInfo>, Void, Void>{

		private String msg;
		private String err;
		private Context context;

		public AddToPlaylistTask(Context c) {
			context = c;
		}

		@Override
		protected void onPreExecute() {
			//Toast.makeText(context, R.string.adding_songs_playlist, 
			//		Toast.LENGTH_SHORT).show();
		}

		@Override
		protected Void doInBackground(ArrayList<SongInfo>... params) {

			ArrayList<SongInfo> songs = new ArrayList<SongInfo>();
			
			for(SongInfo song : params[0]){
								
				//check for null data
				if(song.id == null || song.name == null || song.artist.name == null || song.release.image == null){
					continue;
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
