package com.android.songseeker.activity;

import java.util.ArrayList;

import com.android.songseeker.R;
import com.android.songseeker.data.ArtistsParcel;
import com.android.songseeker.data.UserProfile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class SongSeekerActivity extends Activity {
   	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Button search = (Button)findViewById(R.id.search_but);
        search.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	searchNewSongs();
            }
        }); 
        
        ImageButton seek = (ImageButton)findViewById(R.id.seek_songs_but);
        seek.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	getSongsFromProfile();
            }
        }); 
    }

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent i;
    	
    	// Handle item selection
        switch (item.getItemId()) {
        case R.id.settings:
        	i = new Intent(SongSeekerActivity.this, SettingsActivity.class);
            startActivity(i);	
        	return true;
        case R.id.pl_options:
        	i = new Intent(SongSeekerActivity.this, PlaylistOptionsActivity.class);
            startActivity(i);	
            return true;
        case R.id.about:
            //showAbout();
            return true;  
        case R.id.profile:
        	i = new Intent(SongSeekerActivity.this, ProfileActivity.class);
            startActivity(i);	
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void searchNewSongs(){    	
    	EditText textInput = (EditText) findViewById(R.id.find_input);    	
    	
    	//remove the soft input window from view
    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
    	imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0); 
    	
    	ArtistsParcel ss = new ArtistsParcel();
    	if(!textInput.getText().toString().equalsIgnoreCase(""))
    		ss.addArtist(textInput.getText().toString());
    	
    	Intent i = new Intent(SongSeekerActivity.this, RecSongsActivity.class);
    	i.putExtra("searchSeed", ss);

    	startActivity(i);    	
    }
    
    protected void getSongsFromProfile() {
		if(UserProfile.getInstance(getCacheDir()).isEmpty()){
			Toast.makeText(getApplicationContext(), "Please add at least one artist to your profile.", Toast.LENGTH_LONG).show();
			
			Intent i = new Intent(SongSeekerActivity.this, ProfileActivity.class);
			startActivity(i);	
			return;
		}
		
		ArrayList<String> artists = UserProfile.getInstance(getCacheDir()).getRandomArtists(5);
		
		ArtistsParcel ss = new ArtistsParcel();
		for(String name : artists){
			ss.addArtist(name);
		}
		
    	Intent i = new Intent(SongSeekerActivity.this, RecSongsActivity.class);
    	i.putExtra("searchSeed", ss);

    	startActivity(i);    			
	}
}