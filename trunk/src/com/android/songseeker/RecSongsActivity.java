package com.android.songseeker;

import com.echonest.api.v4.EchoNestAPI;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class RecSongsActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Intent intent;
		PlaylistParams plParams;
		Playlist playlist;
		EchoNestAPI en;
		int numArtists;
		
		super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.rec_songs_list);
	
	    intent = getIntent();
	    numArtists = intent.getIntExtra("num_artist", -1);
	    
	    plParams = new PlaylistParams();
	    plParams.setResults(20);   

	    for(int i=0; i<numArtists; i++){
	    	String str = intent.getStringExtra("artist"+i);
	    	if(str.equals(null)){
	    		Log.w("RecSongsActivity", "ignoring null artist ["+i+"]");
	    		continue;
	    	}
	    	
	    	plParams.addArtist(str);	    	
	    	
	    }
	    
	    en = new EchoNestAPI("OKF60XQ3DSLHDO9CX");
	    try {
			playlist = en.createStaticPlaylist(plParams);
		} catch (EchoNestException e) {
			// TODO Auto-generated catch block
			Log.e("RecSongsActivity", "createStaticPlaylist failed!", e);
		}
	    
	    //get the input from the
	    // TODO Auto-generated method stub
	}

}
