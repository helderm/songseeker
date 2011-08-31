package com.android.songseeker.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.songseeker.R;
import com.android.songseeker.comm.EchoNestComm;
import com.android.songseeker.comm.RdioComm;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.data.SongList;
import com.android.songseeker.util.Util;

import com.echonest.api.v4.Song;
import com.echonest.api.v4.SongParams;

public class CreatePlaylistRdioActivity extends Activity{

	private static final int REQUEST_AUTH_DIAG = 1;
	private static final int FETCH_SONG_IDS_DIAG = 2;
	private static final int CREATE_PLAYLIST_DIAG = 3;
	
	private ProgressDialog fetchSongIdsDiag;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
		if(!RdioComm.getComm(settings).isAuthorized()) {
			new RequestAuthorizeTask().execute(null, null, null);			
		} else {
			new CreatePlaylistTask().execute(null, null, null);
		}	
		
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch(id){
		case REQUEST_AUTH_DIAG:
			ProgressDialog rad = new ProgressDialog(this);
			rad.setMessage("Requesting authorization from Rdio...");
			rad.setIndeterminate(true);
			rad.setCancelable(true);
			return rad;
		case FETCH_SONG_IDS_DIAG:
			fetchSongIdsDiag = new ProgressDialog(this);
			fetchSongIdsDiag.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			fetchSongIdsDiag.setMessage("Fetching song data...");
			fetchSongIdsDiag.setCancelable(false);			
			return fetchSongIdsDiag;
		case CREATE_PLAYLIST_DIAG:
			ProgressDialog cpd = new ProgressDialog(this);
			cpd.setMessage("Creating playlist on Rdio...");
			cpd.setIndeterminate(true);
			cpd.setCancelable(false);
			return cpd;			
		default:
			return null;
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	private class RequestAuthorizeTask extends AsyncTask<Void, Void, Boolean>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			showDialog(REQUEST_AUTH_DIAG);
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {

			try {				
				RdioComm.getComm().requestAuthorize(CreatePlaylistRdioActivity.this);
			} catch (ServiceCommException e) {
				Log.e(Util.APP, "Unable to request access to Rdio!", e);
				err = e.getMessage();
				return false;
			} 

			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if(err != null){
				removeDialog(REQUEST_AUTH_DIAG);
				Toast.makeText(getApplicationContext(), err , Toast.LENGTH_SHORT).show();
				CreatePlaylistRdioActivity.this.finish();
			}
			
		}
		
	}

	private class CreatePlaylistTask extends AsyncTask<Void, Integer, Void>{
		
		private SongList sl = getIntent().getExtras().getParcelable("songList");
		private String err = null;
		
		
		@Override
		protected void onPreExecute() {
			showDialog(FETCH_SONG_IDS_DIAG);
			fetchSongIdsDiag.setMax(sl.getSongIDs().size());
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			
			if(progress[0] >= 0)
				fetchSongIdsDiag.setProgress(progress[0]);
			else{
				removeDialog(FETCH_SONG_IDS_DIAG);
				showDialog(CREATE_PLAYLIST_DIAG);
			}
		}
		
		
		@Override
		protected Void doInBackground(Void... params) {
		
			List<String> songIDs = new ArrayList<String>();
					
			int count = 0;
			for(String id : sl.getSongIDs()){
				
				Log.d(Util.APP, "SongID = [" + id + "]");
				
				Song song = null;
				SongParams sp = new SongParams();
				sp.setID(id);
				sp.addIDSpace(EchoNestComm.RDIO);
				
				try {
					song = EchoNestComm.getComm().getSongs(sp);
					
					String rdioID = song.getString("foreign_ids[0].foreign_id");
					
					String[] split = rdioID.split(":");
					songIDs.add(split[2]);
					Log.d(Util.APP, "RdioID = ["+split[2]+"]");
				}catch (NoSuchMethodError err){
					Log.e(Util.APP, "NoSuchMethodErr from jEN strikes again!", err);
					continue;
				}catch (ServiceCommException e) {
					err = e.getMessage();
					return null;
				} catch (IndexOutOfBoundsException e){					
					if(song != null){
						Log.d(Util.APP, "Song ["+ song.getReleaseName()+" - " +song.getArtistName()+"] not found on EchoNest! Trying Rdio...");
						
						try{
							songIDs.add(RdioComm.getComm().queryTrackID(song.getReleaseName(), song.getArtistName()));							
						}catch(ServiceCommException ex){
							/* This will need a cleanup of tokens inside queryTrackID
							 * if(ex.getErr() == ServiceErr.NOT_AUTH){
								err = e.getMessage();	
								return null;
							}*/
								
							Log.w(Util.APP, "Err while fetching track data from Rdio! Ignoring track...", ex);
						}
					}					
				}
				
				publishProgress(++count);
			}
			
			Log.i(Util.APP, "SongIDs fetched! Creating playlist...");
			
			//show createPlaylist diag
			publishProgress(-1);
			
			try{
				//createPlaylist(songIDs);
				SharedPreferences settings = getPreferences(MODE_PRIVATE);
				RdioComm.getComm().createPlaylist(songIDs, settings);
			}catch(ServiceCommException e){
				err = e.getMessage();				
			}			
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			removeDialog(CREATE_PLAYLIST_DIAG);
			
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();
			}else{
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.pl_created_str), Toast.LENGTH_LONG).show();
			}
			
			CreatePlaylistRdioActivity.this.finish();
		}
		
	}
	
	@Override
    protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Uri uri = intent.getData();

		removeDialog(REQUEST_AUTH_DIAG);
		
		//Verificando se a chamada vem realmente do callback esperado
		if (uri == null || !uri.toString().contains("oauth")) {
			return;
		}

		Log.d(Util.APP, "OAuth callback started!");
		
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		
		try{
			RdioComm.getComm().retrieveAccessTokens(uri, settings);
		}catch (ServiceCommException e){
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
			return;
		}		
		
		new CreatePlaylistTask().execute(null, null, null);		
	}	
}