package com.android.songseeker.activity;

import java.util.ArrayList;

import com.android.songseeker.comm.EchoNestComm;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.data.ArtistInfo;
import com.echonest.api.v4.Artist;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

public class SimilarArtistsActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

	    new GetSimilarArtists().execute();
	}
	
	private class GetSimilarArtists extends AsyncTask<Void, Void, ArrayList<Artist>>{
		String err = null;
		
		@Override
		protected ArrayList<Artist> doInBackground(Void... arg0) {
			ArrayList<Artist> similar;
			
			try {
				ArtistInfo artist = getIntent().getExtras().getParcelable("artistParcel");
				similar = EchoNestComm.getComm().getSimilarArtistsFromBucket(artist.id, 10);
			} catch (ServiceCommException e) {
				err = e.getMessage();
				return null;
			}
			
			
			return similar;
		}
		
	}
	

}
