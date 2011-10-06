package com.android.songseeker.activity;

import com.android.songseeker.comm.EchoNestComm;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.data.ArtistInfo;
import com.echonest.api.v4.Biography;

import com.android.songseeker.R;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class ArtistBioActivity extends Activity {

	private static final int ARTIST_BIO_DIAG = 0;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		new GetArtistBio().execute();
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
			Biography bio;

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
}
