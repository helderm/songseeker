package com.seekermob.songseeker.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.util.Util.TabsAdapter;

import android.os.Bundle;
import android.support.v4.view.ViewPager;

public class MusicInfoActivity extends SherlockFragmentActivity {
	TabsAdapter mTabsAdapter;
	ViewPager mViewPager;
		
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	    
		setContentView(R.layout.pager);
		
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);
		
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, actionBar, mViewPager);
        SongInfo song = getIntent().getExtras().getParcelable("song");        
        
		//create tabs
		ActionBar.Tab songTab = actionBar.newTab().setText(R.string.song);
		ActionBar.Tab releaseTab = actionBar.newTab().setText(R.string.album);
		ActionBar.Tab artistTab = actionBar.newTab().setText(R.string.artist);
		        
        //add tabs
		Bundle songInfoArgs = new Bundle();
		Bundle releaseInfoArgs = new Bundle();
		Bundle artistInfoArgs = new Bundle();
		
		songInfoArgs.putParcelable("song", song);
		releaseInfoArgs.putParcelable("release", song.release);
		artistInfoArgs.putParcelable("artist", song.artist);
		
		mTabsAdapter.addTab(songTab, SongInfoFragment.class, songInfoArgs);   
		mTabsAdapter.addTab(releaseTab, ReleaseInfoFragment.class, releaseInfoArgs);
        mTabsAdapter.addTab(artistTab, ArtistInfoFragment.class, artistInfoArgs);	
        
        if (savedInstanceState != null) {
        	actionBar.setSelectedNavigationItem(savedInstanceState.getInt("index"));
        	return;
        }    
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("index", getSupportActionBar().getSelectedNavigationIndex());        
    }	

}
