package com.seekermob.songseeker.activity;

import java.util.ArrayList;
import java.util.List;

import com.google.android.apps.analytics.easytracking.TrackedActivity;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.GroovesharkComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.util.Util;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class PlayPlaylistActivity extends TrackedActivity implements OnCancelListener{

	private GetSongIdsTask task;
	
	private static final int FETCH_SONG_IDS_DIAG = 0;
	private ProgressDialog fetchSongIdsDiag = null;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
		//check if DMS is installed on the device
		if(!Util.isAppInstalled("com.mysticdeath.md_gs_app", PlayPlaylistActivity.this)){
			Toast.makeText(getApplicationContext(), "Install Dood's Music Streamer to enable this feature!", Toast.LENGTH_LONG).show();
			
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.mysticdeath.md_gs_app"));
			intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			
			PlayPlaylistActivity.this.finish();
			return;
		}
	    
	    task = new GetSongIdsTask();
	    task.execute();
	}
	
	private class GetSongIdsTask extends AsyncTask<Void, Integer, int[]>{
		
		private String err = null;
		private List<SongInfo> songs = getIntent().getParcelableArrayListExtra("songsInfo");
		
		@Override
		protected void onPreExecute() {
			
			showDialog(FETCH_SONG_IDS_DIAG);			
			Toast.makeText(getApplicationContext(), "Be sure that 'Settings > Share Requests' is turned on at Dood's Music!", Toast.LENGTH_LONG).show();
			
			//access is limited today to some ws calls/ip/minute, so i'll need to truncate the playlist 
			if(songs.size() > GroovesharkComm.RATE_LIMIT){
				
				songs = songs.subList(0, GroovesharkComm.RATE_LIMIT);
				Toast.makeText(getApplicationContext(), "Truncating playlist to " + GroovesharkComm.RATE_LIMIT + " songs, due to technical reasons...", Toast.LENGTH_SHORT).show();
			}
			
			fetchSongIdsDiag.setMax(songs.size());
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			
			fetchSongIdsDiag.setProgress(progress[0]);
		}		
		
		@Override
		protected int[] doInBackground(Void... params) {
			SharedPreferences settings = getApplicationContext().getSharedPreferences(Util.APP, Context.MODE_PRIVATE);
			ArrayList<String> ids = new ArrayList<String>();
			int songIds[] = null;
			int i = 0;			
			int count = 0;
			
			//fetch the song ids from grooveshark
			for(SongInfo song : songs){

				//check if the task was cancelled by the user
				if(Thread.interrupted()){
					return null;
				}
				
				try {					
					String gsID = GroovesharkComm.getComm().getSongID(song.name, song.artist.name, settings);					
					ids.add(gsID);
				}catch (ServiceCommException e) {
					if(e.getErr() == ServiceErr.SONG_NOT_FOUND){
						publishProgress(++count);
						Log.i(Util.APP, "Song ["+song.name+" - "+song.artist.name+"] not found in Grooveshark, ignoring...");
						continue;
					}
					
					songIds = new int[ids.size()];
					for(String id : ids){
						songIds[i++] = Integer.parseInt(id);
					}
					
					if(e.getErr() == ServiceErr.TRY_LATER){
						err = "Some songs were not fetched due to technical reasons, try later!";
						return songIds;
					}
					
					err = e.getMessage();
					return songIds;
				} 
				
				publishProgress(++count);
			}
			
			//check if the task was cancelled by the user
			if(Thread.interrupted()){
				return null;
			}
			
			//convert from string list to static array
			songIds = new int[ids.size()];
			for(String id : ids){
				songIds[i++] = Integer.parseInt(id);
			}
			
			return songIds;
		}
		
		@Override
		protected void onPostExecute(int songIds[]) {
			removeDialog(FETCH_SONG_IDS_DIAG);
			
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
			}
			
			if(songIds == null || songIds.length == 0){
				Toast.makeText(getApplicationContext(), "No song found!", Toast.LENGTH_SHORT).show();
				PlayPlaylistActivity.this.finish();
				return;
			}
			
			Intent intent = new Intent();
			intent.setAction("com.mysticdeath.md_gs_app.remotelist");
			intent.putExtra("name", Util.APP);
			intent.putExtra("songIDs", songIds);
			sendBroadcast(intent);
			PlayPlaylistActivity.this.finish();
		}		
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {

		switch(id){
		case FETCH_SONG_IDS_DIAG:
			fetchSongIdsDiag = new ProgressDialog(this);
			fetchSongIdsDiag.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			fetchSongIdsDiag.setMessage("Fetching song data...");
			fetchSongIdsDiag.setCancelable(true);
			fetchSongIdsDiag.setOnCancelListener(this);
			return fetchSongIdsDiag;			
		default:
			return null;
		}
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		if(task != null)
			task.cancel(true);
		
		Toast.makeText(getApplicationContext(), getString(R.string.op_cancel_str), Toast.LENGTH_SHORT).show();
		finish();
	}
}
