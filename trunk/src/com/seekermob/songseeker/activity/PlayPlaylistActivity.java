package com.seekermob.songseeker.activity;

import java.util.ArrayList;
import java.util.List;

import com.google.android.apps.analytics.easytracking.TrackedActivity;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.GroovesharkComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.data.ArtistsParcel;
import com.seekermob.songseeker.data.SongNamesParcel;
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
	
	    task = new GetSongIdsTask();
	    task.execute();
	}
	
	private class GetSongIdsTask extends AsyncTask<Void, Integer, ArrayList<String>>{
		
		private String err = null;
		
		private List<String> songNames = new ArrayList<String>();
		private List<String> artistNames = new ArrayList<String>(); 
		
		@Override
		protected void onPreExecute() {
			SongNamesParcel sn = getIntent().getExtras().getParcelable("songNames");
			ArtistsParcel ar = getIntent().getExtras().getParcelable("songArtists");
			
			showDialog(FETCH_SONG_IDS_DIAG);
			
			//access is limited today to 32 ws calls/ip/minute, so i'll need to truncate the playlist 
			if(sn.getSongNames().size() > 30){
				
				songNames =  sn.getSongNames().subList(0, 30);
				artistNames = ar.getArtistList().subList(0, 30);
				Toast.makeText(getApplicationContext(), "Truncating playlist to 30 songs, due to technical reasons...", Toast.LENGTH_LONG).show();
			}else{
				songNames = sn.getSongNames();
				artistNames = ar.getArtistList();
			}
			
			fetchSongIdsDiag.setMax(songNames.size());
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			
			fetchSongIdsDiag.setProgress(progress[0]);
		}		
		
		@Override
		protected ArrayList<String> doInBackground(Void... params) {
		
			ArrayList<String> songIDs = new ArrayList<String>();
			
			SharedPreferences settings = getApplicationContext().getSharedPreferences(Util.APP, Context.MODE_PRIVATE);
			
			int count = 0;
			for(int i=0; i<songNames.size(); i++){
				
				//check if the task was cancelled by the user
				if(Thread.interrupted()){
					return null;
				}
				
				try {					
					String gsID = GroovesharkComm.getComm().getSongID(songNames.get(i), artistNames.get(i), settings);
					songIDs.add(gsID);					
				}catch (ServiceCommException e) {
					if(e.getErr() == ServiceErr.SONG_NOT_FOUND){
						publishProgress(++count);
						Log.i(Util.APP, "Song ["+songNames.get(i)+" - "+artistNames.get(i)+"] not found in Grooveshark, ignoring...");
						continue;
					}
					
					if(e.getErr() == ServiceErr.TRY_LATER){
						err = "Some songs were not fetched due to technical reasons, try later!";
						return songIDs;
					}
					
					err = e.getMessage();
					return songIDs;
				} 
				
				publishProgress(++count);
			}
			
			//check if the task was cancelled by the user
			if(Thread.interrupted()){
				return null;
			}
			
			return songIDs;
		}
		
		@Override
		protected void onPostExecute(ArrayList<String> songIds) {
			removeDialog(FETCH_SONG_IDS_DIAG);
			
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
			}
			
			if(songIds == null || songIds.isEmpty()){
				Toast.makeText(getApplicationContext(), "No song found!", Toast.LENGTH_SHORT).show();
				PlayPlaylistActivity.this.finish();
				return;
			}
			
			//build url with fetched song ids
			StringBuilder sb = new StringBuilder();
			
			sb.append(GroovesharkComm.WIDGET_URL);			
			for(String id : songIds){
				sb.append(id+",");
			}
			sb.deleteCharAt(sb.length()-1);	

			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sb.toString()));
			startActivity(intent);
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
