package com.android.songseeker.data;

import java.util.ArrayList;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.android.songseeker.activity.RecSongsActivity.RecSongsAdapter;
import com.android.songseeker.comm.EchoNestComm;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.comm.ServiceCommException.ServiceErr;
import com.android.songseeker.util.Util;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.Song;
import com.echonest.api.v4.SongParams;

public class RecSongsPlaylist {
	private static RecSongsPlaylist obj = new RecSongsPlaylist();
	
	private static ArrayList<Song> songs = null;	
	private static RecSongsAdapter adapter = null;
	
	private RecSongsPlaylist() {}
	
	public static RecSongsPlaylist getInstance(RecSongsAdapter ad){
		adapter = ad;
		return obj;
	}
	
	public static RecSongsPlaylist getInstance(){
		return obj;
	}
	
	/** Get a new playlist methods*/
	public void getPlaylist(PlaylistParams plp, Activity a, int progressDiag){
		songs = new ArrayList<Song>();
		new GetPlaylistTask(plp, a, progressDiag).execute();		
	}
	
	private class GetPlaylistTask extends AsyncTask<Void, Void, Void>{
		private String err = null;
		private PlaylistParams playlistParams;
		private Activity activity;
		private int progressDiag;		
		
		public GetPlaylistTask(PlaylistParams plp, Activity a, int pd) {
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
			
			syncAddSongsToPlaylist((ArrayList<Song>)pl.getSongs());
			
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
			
			adapter.setPlaylist(songs);
			adapter.notifyDataSetChanged();				
		}		
	}	
	
	/** Add songs to the playlist methods*/
	@SuppressWarnings("unchecked")
	public void addSongsToPlaylist(ArrayList<SongInfo> songsInfo, Activity a){		
		new GetSongInfoTask(a).execute(songsInfo);
	}
	
	private class GetSongInfoTask extends AsyncTask<ArrayList<SongInfo>, Void, Void>{

		private String msg = null;
		private String err = null;
		private Activity activity = null;
		
		public GetSongInfoTask(Activity a) {
			activity = a;
		}
		
		@Override
		protected void onPreExecute() {
			Toast.makeText(activity.getApplicationContext(), "Adding song(s) to playlist, please wait...", 
								Toast.LENGTH_LONG).show();
		}
		
		@Override
		protected Void doInBackground(ArrayList<SongInfo>... params) {
			
			ArrayList<Song> songList = new ArrayList<Song>();
			Song song = null;

			for(SongInfo songInfo : params[0]){
				SongParams sp = new SongParams();
				sp.add("track_id", "7digital:track:"+songInfo.id);
				sp.addIDSpace(EchoNestComm.SEVEN_DIGITAL);
				sp.includeTracks();
				sp.setLimit(true);

				try{
					song = EchoNestComm.getComm().getSongs(sp);
				}catch (ServiceCommException e) {
					if(e.getErr() == ServiceErr.ID_NOT_FOUND){
						Log.i(Util.APP, "7digital's id not found in EchoNest, trying to search for the song...");
						
						//try with echo nest 'search'
						sp = new SongParams();
						sp.setArtist(songInfo.artist.name);
						sp.setTitle(songInfo.name);
						sp.addIDSpace(EchoNestComm.SEVEN_DIGITAL);
						sp.includeTracks();
						sp.setLimit(true);

						try {
							song = EchoNestComm.getComm().searchSongs(sp);
						} catch (ServiceCommException e1) {
							
							if(e1.getErr() == ServiceErr.ID_NOT_FOUND){
								Log.w(Util.APP, "Song ["+ songInfo.name + " - "+ songInfo.artist.name +"] not found in EchoNest, skiping...");
							}else{
								Log.w(Util.APP, e1.getMessage(), e1);
							}
							continue;
						}
					}else{
						Log.w(Util.APP, e.getMessage(), e);
						continue;
					}
				}

				songList.add(song);
			}				

			if(songList.size() == 0){
				err = "Failed to add song(s) to the playlist!";
				return null;
			}else if(songList.size() < params[0].size()){
				msg = "Some songs were successfully added to the playlist!";
			}else
				msg = "Song(s) successfully added to the playlist!";				
			
			syncAddSongsToPlaylist(songList);
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(err != null){
				Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
				return;
			}
			
			adapter.setPlaylist(songs);
			adapter.notifyDataSetChanged();		
			
			Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_LONG).show();
		}		
	}	
	
	private synchronized void syncAddSongsToPlaylist(ArrayList<Song> songList){
		if(songs == null)
			songs = new ArrayList<Song>();
		
		songs.addAll(songList);
	}
	
	public void removeSongFromPlaylist(int position){
		songs.remove(position);
		adapter.notifyDataSetChanged();
	}
}