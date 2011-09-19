package com.android.songseeker.activity;

import com.android.songseeker.R;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.comm.SevenDigitalComm;
import com.android.songseeker.data.ArtistsParcel;
import com.android.songseeker.data.SongIdsParcel;
import com.android.songseeker.data.SongNamesParcel;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

public class SongInfoActvity extends Activity {

	private static final int SONG_DETAILS_DIAG = 0;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setContentView(R.layout.song_info);
	    
	    
	    SongNamesParcel songName = getIntent().getExtras().getParcelable("songName");
	    ArtistsParcel songArtist = getIntent().getExtras().getParcelable("songArtist");
	    
	    TextView tvSongName = (TextView) findViewById(R.id.songinfo_songName);
	    tvSongName.setText(songName.getSongNames().get(0));
	    
	    TextView tvSongArtist = (TextView) findViewById(R.id.songinfo_artistName);
	    tvSongArtist.setText(songArtist.getArtistList().get(0));
	    
	    new GetSongDetails().execute();
	}
	
	@Override
    protected Dialog onCreateDialog(int id) {
		switch(id){
		case SONG_DETAILS_DIAG:
		    ProgressDialog pd = new ProgressDialog(this);
		    pd.setMessage("Fetching song details...");
		    pd.setIndeterminate(true);
		    pd.setCancelable(false);
		    return pd;
		default:
			return null;		    	
		}
	}
	
	private class GetSongDetails extends AsyncTask<Void, Void, Void>{

		@Override
		protected void onPreExecute() {
			showDialog(SONG_DETAILS_DIAG);
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			SongIdsParcel songIdParcel = getIntent().getExtras().getParcelable("songId");			
			
			try{
				SevenDigitalComm.getComm().queryTrackDetails(songIdParcel.getSongIDs().get(0));
			}catch(ServiceCommException e){
				
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			removeDialog(SONG_DETAILS_DIAG);
		}
		
	}

}
