package com.android.songseeker.activity;

import java.util.ArrayList;

import com.android.songseeker.R;
import com.android.songseeker.data.ArtistsParcel;
import com.android.songseeker.data.UserProfile;

import android.app.Activity;
import android.app.Dialog;
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
   	
	private static final int SEARCH_DIAG = 0; 
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        ImageButton search = (ImageButton)findViewById(R.id.search_but);
        search.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	showDialog(SEARCH_DIAG);
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
    
    private void searchNewSongs(String artistName){    	

    	ArtistsParcel ss = new ArtistsParcel();

    	ss.addArtist(artistName);
    	
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
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	switch (id) {
		case SEARCH_DIAG:
			Dialog dialog = new Dialog(this);

			dialog.setContentView(R.layout.new_playlist_diag);
			dialog.setTitle("Search:");
			
			EditText input = (EditText)dialog.findViewById(R.id.pl_name_input);
			input.setHint(R.string.artist_name_str);
						
			Button create_but = (Button)dialog.findViewById(R.id.create_pl_but);
			
			create_but.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
	               	View p = (View)v.getParent();	            	
	            	View parent = (View)p.getParent();	            	
	            	EditText textInput = (EditText) parent.findViewById(R.id.pl_name_input); 
	            	
	                //check if the edit text is empty
	            	if(textInput.getText().toString().compareTo("") == 0){
	            		    		
	            		Toast toast = Toast.makeText(SongSeekerActivity.this, 
	            						getResources().getText(R.string.invalid_args_str), Toast.LENGTH_SHORT);
	            		toast.show();
	            		removeDialog(SEARCH_DIAG);
	            		return;
	            	}
	            	
	            	//remove the soft input window from view
	            	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
	            	imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0); 
	            		            	
	            	removeDialog(SEARCH_DIAG);	            	
	            		            	
	            	searchNewSongs(textInput.getText().toString());
	            }
	        }); 
			
			return dialog;	
		default:
			return null;
		}
    }
}