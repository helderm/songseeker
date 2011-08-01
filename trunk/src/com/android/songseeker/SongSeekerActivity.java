package com.android.songseeker;

//import com.echonest.api.v4.EchoNestAPI;
//import com.echonest.api.v4.EchoNestException;
//import com.echonest.api.v4.Playlist;
//import com.echonest.api.v4.PlaylistParams;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;

public class SongSeekerActivity extends Activity {
   	
	//EchoNestAPI en;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        ///System.setProperty("ECHO_NEST_API_KEY", "OKF60XQ3DSLHDO9CX");
        
        //try {
        //	EchoNestAPI en = new EchoNestAPI();
		//} catch (EchoNestException e1) {
			// TODO Auto-generated catch block
		//	e1.printStackTrace();
		//}
        
        //PlaylistParams p = new PlaylistParams(); 
        //p.addArtist("Weezer");
        //p.setResults(10);   
        //try {
		//	Playlist playlist = en.createStaticPlaylist(p);
		//} catch (EchoNestException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
		//}

          
        
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
}