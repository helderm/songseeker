package com.android.songseeker;

import java.util.List;

import com.echonest.api.v4.EchoNestAPI;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.PlaylistParams.PlaylistType;
import com.echonest.api.v4.Song;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RecSongsActivity extends Activity implements Runnable{

	private List<Song> recSongs;
	private final int PROGRESS_DIAG = 0;
	Thread progressThread = null;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.rec_songs_list);	
	    
	    //show 'loading' dialog
		showDialog(PROGRESS_DIAG);
	    
		//start thread that will query data from echo nest
		progressThread = new Thread(this);
	    progressThread.start();
	    

	}

	private void populateRecommendedSongs() {
		LinearLayout l = (LinearLayout) findViewById(R.id.rec_songs_list_layout);
		LayoutInflater linflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
		
		for(Song song : recSongs){
			
          View myView = linflater.inflate(R.layout.rec_song, null);                    
          TextView t = (TextView) myView.findViewById(R.id.song_info);          
          t.setText((song.getReleaseName()).toString() + " - " + song.getArtistName());
          l.addView(myView);			
		}		
	}


	@Override
    protected Dialog onCreateDialog(int id) {

		switch(id){
		case PROGRESS_DIAG:
		    ProgressDialog pd = new ProgressDialog(this);
		    pd.setMessage("Retrieving data from Echo Nest...");
		    pd.setIndeterminate(true);
		    pd.setCancelable(true);
		    return pd;
		default:
			return null;
		}
        
    }
	
	@Override
	public void run() {
		Playlist playlist;
		EchoNestAPI en;		
		PlaylistParams plParams;		
		int numArtists;
		
	    numArtists = getIntent().getIntExtra("num_artist", -1);
	    
	    plParams = new PlaylistParams();
	    plParams.setResults(20);   
	    plParams.setType(PlaylistType.ARTIST_RADIO);
	    plParams.setVariety(0.1f);
	    plParams.setMaxEnergy(Settings.pl_energy/100);
	    //TODO add other options
	    
	    
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
			Log.e("SongSeeker", "createStaticPlaylist failed!", e);
			return;
		}
	    
	    recSongs = playlist.getSongs();
	    handler.sendEmptyMessage(0);
		
	}
	
	private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	dismissDialog(PROGRESS_DIAG); 
            populateRecommendedSongs();
        }
	};

}
