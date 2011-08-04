package com.android.songseeker;

import java.util.List;

import com.echonest.api.v4.EchoNestAPI;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.Song;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class RecSongsActivity extends Activity {

	private List<Song> recSongs;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		
		super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.rec_songs_list);
	
	    getRecommendedSongs();
	    
	    printRecommendedSongs();
		
	    //get the input from the

	}

	private void printRecommendedSongs() {
		LinearLayout l = (LinearLayout) findViewById(R.id.rec_songs_list_layout);
		LayoutInflater linflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		
		for(Song song : recSongs){
			
          View myView = linflater.inflate(R.layout.rec_song, null);
                    
          TextView t = (TextView) myView.findViewById(R.id.song_info);

          //Change name dynamically
          t.setText(("Artist: " + song.getArtistName() + " - Song: "+ song.getReleaseName()).toString());
          l.addView(myView);			
		}
		
	}

	private void getRecommendedSongs() {
		PlaylistParams plParams;
		Playlist playlist;
		EchoNestAPI en;
		int numArtists;
		
	    numArtists = getIntent().getIntExtra("num_artist", -1);
	    
	    plParams = new PlaylistParams();
	    plParams.setResults(20);   

	    for(int i=0; i<numArtists; i++){
	    	String str = getIntent().getStringExtra("artist"+i);
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
			// TODO Add an error popup screen to the user and go back to the previous screen
			Log.e("RecSongsActivity", "createStaticPlaylist failed!", e);
			return;
		}
	    
		recSongs = playlist.getSongs();
	}

}
