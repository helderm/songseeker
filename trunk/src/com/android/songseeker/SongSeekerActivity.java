package com.android.songseeker;

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
import android.widget.Toast;

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
    	
        //check if the edit text is empty
    	if(textInput.getText().toString().compareTo("") == 0){
    		    		
    		Toast toast = Toast.makeText(SongSeekerActivity.this, 
    						getResources().getText(R.string.invalid_args_str), Toast.LENGTH_SHORT);
    		toast.show();
    		return;
    	}
    	
    	//remove the soft input window from view
    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
    	imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0); 
    	
    	Intent i = new Intent(SongSeekerActivity.this, RecSongsActivity.class);
    	i.putExtra("num_artist", 1);
        i.putExtra("artist0", textInput.getText().toString());
    	
    	startActivity(i);    	
    }

        
}