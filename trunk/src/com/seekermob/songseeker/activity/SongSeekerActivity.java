package com.seekermob.songseeker.activity;

import java.io.File;
import java.util.ArrayList;

import com.google.android.apps.analytics.easytracking.TrackedActivity;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.UserProfile;
import com.seekermob.songseeker.util.AppRater;
import com.seekermob.songseeker.util.ImageLoader;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class SongSeekerActivity extends TrackedActivity {
   	
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
                
        //check if we need to auto clear the cache
        new AutoClearCacheTask().execute();
        
        //check if we want to display the request to rate the app
        AppRater.app_launched(this);
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
        int itemId = item.getItemId();
    	
        if(itemId == R.id.settings){
        	i = new Intent(SongSeekerActivity.this, SettingsActivity.class);
        	startActivity(i);
            return true;
        }
        
        if(itemId == R.id.pl_options){
        	i = new Intent(SongSeekerActivity.this, PlaylistOptionsActivity.class);
        	startActivity(i);
            return true;
        }
        	
        if(itemId == R.id.about){
        	i = new Intent(SongSeekerActivity.this, AboutActivity.class);
        	startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    private void searchNewSongs(String artistName){    	

    	ArrayList<ArtistInfo> artist = new ArrayList<ArtistInfo>();
    	artist.add(new ArtistInfo());    	
    	artist.get(0).name = artistName;    	
    	
    	Intent i = new Intent(SongSeekerActivity.this, RecSongsActivity.class);
    	i.putExtra("searchSeed", artist);

    	startActivity(i);    	
    }
    
    protected void getSongsFromProfile() {
		if(UserProfile.getInstance(getCacheDir()).isEmpty()){
			Toast.makeText(getApplicationContext(), "Please add at least one artist to your profile.", Toast.LENGTH_LONG).show();
			
			Intent i = new Intent(SongSeekerActivity.this, ProfileActivity.class);
			startActivity(i);	
			return;
		}
		
    	Intent i = new Intent(SongSeekerActivity.this, RecSongsActivity.class);
    	i.putExtra("searchSeed", UserProfile.getInstance(getCacheDir()).getRandomArtists(5));

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
    
	public class AutoClearCacheTask extends AsyncTask<Void, Void, Boolean>{

		@Override
		protected Boolean doInBackground(Void... params) {			
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SongSeekerActivity.this);
			String aux = prefs.getString("auto_clear_cache", "0");			
			int maxCache = Integer.parseInt(aux);
			
			if(maxCache == 0){
				return false;
			}

			File cacheDir = getCacheDir();			
			long cacheSize = ImageLoader.getLoader(cacheDir).getFileCacheSize(cacheDir);        	
			
			if(cacheSize > (maxCache * 1048576)){ //bytes in a Mb
				 ImageLoader.getLoader(cacheDir).clearCache(cacheDir);
				 return true;
			}					
		
			return false;
		}		

		@Override
		protected void onPostExecute(Boolean isCleared) {
			if(isCleared == true)
				Toast.makeText(getApplicationContext(), "Cache auto cleared!", Toast.LENGTH_SHORT).show();
		}
	}	    
    
    public void shareApp(View v){
		final Intent intent = new Intent(Intent.ACTION_SEND);					 
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_SUBJECT, "Song Seeker app for Android");
		intent.putExtra(Intent.EXTRA_TEXT, "Song Seeker is a new way of discovering music for Android! Based in your taste, it automatically " +
				"fetches songs that may suit you. If you are a music lover, give it a try! https://market.android.com/details?id=com.seekermob.songseekerfree");
		startActivity(Intent.createChooser(intent, "Share using..."));
    }
    
    public void viewProfile(View v){
    	Intent i = new Intent(SongSeekerActivity.this, ProfileActivity.class);
    	startActivity(i);
    }

    //used by the free package in the main.xml layout after clicking the 'donate' button
    public void donate(View v) {
    	Toast.makeText(getApplicationContext(), "The Donate version will soon be released, stay tuned!", Toast.LENGTH_LONG).show();
    	
    	/*Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://market.android.com/details?id=com.seekermob.songseekerfull"));
		intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);*/
    }   
    
    
}