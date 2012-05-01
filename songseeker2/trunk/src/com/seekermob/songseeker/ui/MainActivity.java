package com.seekermob.songseeker.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.ui.InputDialogFragment.OnTextEnteredListener;
import com.seekermob.songseeker.util.FileCache;
import com.seekermob.songseeker.util.MediaPlayerController;
import com.seekermob.songseeker.util.Util.TabsAdapter;

import android.os.Bundle;
import android.support.v4.view.ViewPager;

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
        
        if (savedInstanceState != null) {
            actionBar.setSelectedNavigationItem(savedInstanceState.getInt("index"));
        }
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
}
