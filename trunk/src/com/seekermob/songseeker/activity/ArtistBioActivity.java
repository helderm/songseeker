package com.seekermob.songseeker.activity;

import com.echonest.api.v4.Biography;
import com.google.android.apps.analytics.easytracking.TrackedActivity;

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.EchoNestComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.data.ArtistInfo;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class ArtistBioActivity extends TrackedActivity {

	private static final int ARTIST_BIO_DIAG = 0;
	Biography bio;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//check for orientation change
		bio = (Biography) getLastNonConfigurationInstance();				
		if(bio == null){
			new GetArtistBio().execute();
			return;
		}		
		
		if(bio.getText() == null){
			Toast.makeText(getApplicationContext(), "Biography not found!", Toast.LENGTH_SHORT);
			finish();
			return;
		}
		
		setContentView(R.layout.link_text);
		TextView tvText = (TextView)findViewById(R.id.text);
		tvText.setText(bio.getText() + (bio.getURL()!=null? (" - Source: " + bio.getURL()): ""));
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case ARTIST_BIO_DIAG:
			ProgressDialog pd = new ProgressDialog(this);
			pd.setMessage("Fetching artist biography...");
			pd.setIndeterminate(true);
			pd.setCancelable(true);
			return pd;
		default:
			return null;		    	
		}
	}

	private class GetArtistBio extends AsyncTask<Void, Void, Biography>{
		private String err = null;

		@Override
		protected void onPreExecute() {
			showDialog(ARTIST_BIO_DIAG);
		}
		
		@Override
		protected Biography doInBackground(Void... arg0) {

			ArtistInfo artist = getIntent().getExtras().getParcelable("artistParcel");
			try {	
				bio = EchoNestComm.getComm().getArtistBioFromBucket(artist.id);
			} catch (ServiceCommException e) {
				err = e.getMessage();
				return null;
			}

			return bio;
		}

		@Override
		protected void onPostExecute(Biography bio) {

			removeDialog(ARTIST_BIO_DIAG);
			
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				ArtistBioActivity.this.finish();
				return;
			}
			
			if(bio.getText() == null){
				Toast.makeText(getApplicationContext(), "Biography not found!", Toast.LENGTH_SHORT);
				ArtistBioActivity.this.finish();
				return;
			}
			
			setContentView(R.layout.link_text);
			TextView tvText = (TextView)findViewById(R.id.text);
			tvText.setText(bio.getText() + (bio.getURL()!=null? (" - Source: " + bio.getURL()): ""));
		}
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return bio;
	}
}
