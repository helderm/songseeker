package com.seekermob.songseeker.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.ui.InputDialogFragment.OnTextEnteredListener;
import com.seekermob.songseeker.util.FileCache;
import com.seekermob.songseeker.util.MediaPlayerController;
import com.seekermob.songseeker.util.Util.TabsAdapter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.widget.Toast;

public class MainActivity extends SherlockFragmentActivity implements OnTextEnteredListener{

	TabsAdapter mTabsAdapter;
	ViewPager mViewPager;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	    
		setContentView(R.layout.pager);
		
		//set cache dirs
        FileCache.setCacheDirs(getApplicationContext());
		
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);

		//create tabs
		ActionBar.Tab songsTab = actionBar.newTab().setText(R.string.songs);
		//ActionBar.Tab playlistsTab = actionBar.newTab().setText(R.string.playlists);
		ActionBar.Tab profileTab = actionBar.newTab().setText(R.string.profile);
		
        mViewPager = (ViewPager) findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, actionBar, mViewPager);
        
        //add tabs
        mTabsAdapter.addTab(songsTab, SongsFragment.class);        
        //mTabsAdapter.addTab(playlistsTab, PlaylistsFragment.class);
        mTabsAdapter.addTab(profileTab, ProfileFragment.class);
        
        if(savedInstanceState != null) {
            actionBar.setSelectedNavigationItem(savedInstanceState.getInt("index"));
        }
        
        //checks if we need to clear the cache 
        new AutoClearCacheTask().execute();
	}

	@Override
	protected void onPause() {
		MediaPlayerController.getCon().release();
		super.onPause();
	}
	
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("index", getSupportActionBar().getSelectedNavigationIndex());
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main_menu, menu);		
		return super.onCreateOptionsMenu(menu);
	};
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		
		// Handle item selection
		switch (item.getItemId()) {  
		case R.id.menu_settings:
			i = new Intent(this, PreferencesActivity.class);
			startActivity(i);
			return true;
		case R.id.menu_about:
			i = new Intent(this, AboutActivity.class);
			startActivity(i);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
    
	@Override
	public void onDialogTextEntered(String text, String tag) {
		SongsFragment songsFragment; 
		ProfileFragment profileFragment;
		
		//TODO HARDCODED the tabs index here, NEED TO CHANGE THIS when I modify the number of tabs!
		if(tag.equalsIgnoreCase(SongsFragment.DIALOG_ARTIST_NAME)){
			songsFragment = (SongsFragment) getSupportFragmentManager().findFragmentByTag(
	                "android:switcher:"+R.id.pager+":0"/*0 = tab index*/); //that is the tag the ViewPager sets to the fragment
			songsFragment.getNewPlaylist(text);
		}else if(tag.equalsIgnoreCase(ProfileFragment.DIALOG_ARTIST_NAME)){
			profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentByTag(
	                "android:switcher:"+R.id.pager+":1"); //that is the tag the ViewPager sets to the fragment
			profileFragment.importProfile(text, ProfileFragment.IMPORT_TYPE_USER);
		}else if(tag.equalsIgnoreCase(ProfileFragment.DIALOG_LASTFM_USERNAME)){
			profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentByTag(
	                "android:switcher:"+R.id.pager+":1"); //that is the tag the ViewPager sets to the fragment
			profileFragment.importProfile(text, ProfileFragment.IMPORT_TYPE_LASTFM);
		}
	}
	
	public class AutoClearCacheTask extends AsyncTask<Void, Void, Boolean>{
		
		@Override
		protected Boolean doInBackground(Void... params) {			
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
			String aux = prefs.getString("auto_clear_cache", "10");			
			int maxCache = Integer.parseInt(aux);
			
			if(maxCache == 0){
				return false;
			}
			
			long cacheSize = FileCache.getCache(MainActivity.this).getCacheSize();		
			if(cacheSize > (maxCache * 1048576)){ //bytes in a Mb
				FileCache.getCache(MainActivity.this).clear();
				return true;
			}					
		
			return false;
		}		

		@Override
		protected void onPostExecute(Boolean isCleared) {
			if(isCleared == true)
				Toast.makeText(MainActivity.this, R.string.cache_auto_cleared, Toast.LENGTH_SHORT).show();
		}
	}
}
