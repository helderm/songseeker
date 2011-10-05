package com.android.songseeker.activity;

import com.android.songseeker.comm.EchoNestComm;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.data.ArtistInfo;
import com.echonest.api.v4.Biography;

import com.android.songseeker.R;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class ArtistBioActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		new GetArtistBio().execute();
	}


	private class GetArtistBio extends AsyncTask<Void, Void, Biography>{
		private String err = null;

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

			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT);
				ArtistBioActivity.this.finish();
				return;
			}
			
			setContentView(R.layout.link_text);
			TextView text = (TextView)findViewById(R.id.text);
			text.setText(bio.getText());
		}
	}
}
