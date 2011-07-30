package com.android.songseeker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;

public class SongSeekerActivity extends Activity {
    Settings settings;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        settings = new Settings();
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
    	//Intent i = new Intent(SongSeekerActivity.this, screen2.class);
        //startActivity(i);	
    }
}