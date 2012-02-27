package com.seekermob.songseeker.data;

import java.util.ArrayList;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.Song;
import com.seekermob.songseeker.activity.RecSongsActivity.RecSongsAdapter;
import com.seekermob.songseeker.comm.EchoNestComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.util.Util;

public class RecSongsPlaylist {
	private static RecSongsPlaylist obj = new RecSongsPlaylist();

	private static ArrayList<SongInfo> songs = null;
	private static ArrayList<PlaylistListener> listeners = null;

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
	public void getPlaylist(PlaylistParams plp, ListActivity a, int progressDiag){
		new GetPlaylistTask(plp, a, progressDiag).execute();		
	}

	private class GetPlaylistTask extends AsyncTask<Void, Void, Void>{
		private String err = null;
		private PlaylistParams playlistParams;
		private ListActivity activity;
		private int progressDiag;		

		public GetPlaylistTask(PlaylistParams plp, ListActivity a, int pd) {
			playlistParams = plp;
			activity = a;
			progressDiag = pd;
		}

		@Override
		protected void onPreExecute() {
			activity.showDialog(progressDiag);
		}

		@Override
		protected Void doInBackground(Void... params) {
			Playlist pl = null;

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
				try {
					SongInfo songInfo = new SongInfo();
					
					songInfo.id = song.getString("tracks[0].foreign_id").split(":")[2];
					songInfo.name = song.getReleaseName();
					songInfo.artist.name = song.getArtistName();
					songInfo.previewUrl = song.getString("tracks[0].preview_url");	
					songInfo.release.image = song.getString("tracks[0].release_image");
					
					//check for null data
					if(songInfo.id == null || songInfo.name == null || songInfo.artist.name == null ||
						songInfo.previewUrl == null || songInfo.release.image == null){
						continue;
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

			activity.removeDialog(progressDiag);

			if(err != null){
				Toast.makeText(activity.getApplicationContext(), err, Toast.LENGTH_LONG).show();
				activity.finish();
				return;
			}

			if(songs.size() == 0){
				Toast.makeText(activity.getApplicationContext(), "Your playlist is empty! Try changing your playlist options.", 
						Toast.LENGTH_LONG).show();
				activity.finish();
				return;	    		
			}

			//notify listeners that the data changed
			notifyListeners();

			activity = null; //maybe this prevents memory leakage
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
		private ListActivity activity = null;

		public GetSongInfoTask(ListActivity a) {
			activity = a;
		}

		@Override
		protected void onPreExecute() {
			Toast.makeText(activity.getApplicationContext(), "Adding song(s) to playlist, please wait...", 
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
				err = "Failed to add song(s) to the playlist!";
				return null;
			}else if(songs.size() < params[0].size()){
				msg = "Some songs were successfully added to the playlist!";
			}else
				msg = "Song(s) successfully added to the playlist!";				

			syncAddSongsToPlaylist(songs);

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if(err != null){
				Toast.makeText(activity.getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				return;
			}

			Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();			
			
			//notify listeners that the data changed
			notifyListeners();
			
			activity = null; //maybe this prevents memory leakage
		}		
	}	

	private synchronized void syncAddSongsToPlaylist(ArrayList<SongInfo> songList){
		if(songs == null)
			songs = new ArrayList<SongInfo>();

		songs.addAll(songList);
	}

	public void removeSongFromPlaylist(int position, ListActivity activity){
		songs.remove(position);
		if(songs.isEmpty()){
			activity.finish();
			return;
		}			

		((RecSongsAdapter)activity.getListAdapter()).notifyDataSetChanged();
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
