package com.android.songseeker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SongSeekerActivity extends Activity {
   	
	Button search;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        search = (Button)findViewById(R.id.search_but);
        search.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	createRecSongsActivity();
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
    
    private void createRecSongsActivity(){    	
    	EditText textInput = (EditText) findViewById(R.id.find_input);    	
    	Intent i = new Intent(SongSeekerActivity.this, RecSongsActivity.class);
        
    	i.putExtra("num_artist", 1);
        i.putExtra("artist0", textInput.getText().toString());
    	
    	startActivity(i);    	
    }
        
}