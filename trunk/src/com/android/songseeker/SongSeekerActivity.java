package com.android.songseeker;

import java.util.Random;

import com.echonest.api.v4.EchoNestAPI;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.Song;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SongSeekerActivity extends Activity {
   	
	EchoNestAPI en;
	Button search;
	Playlist playlist;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //setContentView(R.layout.search);
        
        //init Echo Nest API object
        en = new EchoNestAPI("OKF60XQ3DSLHDO9CX");		
        
        //search = (Button)findViewById(R.id.search_but);
        //search.setOnClickListener(new View.OnClickListener() {
        //    public void onClick(View v) {
        //    	createSearchActivity();
        //    }
        //}); 
        
//        PlaylistParams p = new PlaylistParams(); 
//        p.addArtist("Eric Clapton");
//        p.setResults(20);   
//        try {
//			playlist = en.createStaticPlaylist(p);
//		} catch (EchoNestException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}          
//			
//		LinearLayout l = (LinearLayout) findViewById(R.id.search_layout);
//        LayoutInflater linflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		for(Song song : playlist.getSongs()){
//			
//            View myView = linflater.inflate(R.layout.song_search, null);
//
//            TextView t = (TextView) myView.findViewById(R.id.song_info);
//
//            //Change name dynamically
//            t.setText(("Artist: " + song.getArtistName() + " - Song: "+ song.getReleaseName()).toString());
//            l.addView(myView);
//			
//		}
		
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.settings:
        	//create settings activity
        	return true;
        case R.id.pl_options:
        	createPlaylistOptionsActivity();
            return true;
        case R.id.about:
            //showAbout();
            return true;            
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void createPlaylistOptionsActivity(){
    	Intent i = new Intent(SongSeekerActivity.this, PlaylistOptionsActivity.class);
        startActivity(i);	
    }
    
    private void createSearchActivity(){
    	Intent i = new Intent(SongSeekerActivity.this, PlaylistOptionsActivity.class);
        startActivity(i);    	
    }
        
}