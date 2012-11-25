package com.seekermob.songseeker.ui;

import java.util.ArrayList;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.ReleaseInfo;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.util.Util.TabsAdapter;

import android.content.Intent;
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

		//prepare action bar
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
		
		//prepare viewpager
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, actionBar, mViewPager);
        
        //fetch data from intent
        SongInfo song = getIntent().getExtras().getParcelable(SongInfoFragment.BUNDLE_SONG);        
        ReleaseInfo release = getIntent().getExtras().getParcelable(ReleaseInfoFragment.BUNDLE_RELEASE);
        ArtistInfo artist = getIntent().getExtras().getParcelable(ArtistInfoFragment.BUNDLE_ARTIST);        
        
        if(release == null && song != null && song.release != null){
        	release = song.release;
        }        
        
        if(artist == null){
        	if(song != null && song.artist != null)
        		artist = song.artist;
        	else if(release != null && release.artist != null)
        		artist = release.artist;
        }
		        
        //add tabs			
		if(song != null){
			Bundle songInfoArgs = new Bundle();
			ArrayList<SongInfo> topTracks = getIntent().getExtras().
												getParcelableArrayList(SongInfoFragment.BUNDLE_TOP_SONGS);
			
			ActionBar.Tab songTab = actionBar.newTab().setText(R.string.song);
			songInfoArgs.putParcelable(SongInfoFragment.BUNDLE_SONG, song);
			
			if(topTracks != null){
				songInfoArgs.putParcelableArrayList(SongInfoFragment.BUNDLE_TOP_SONGS, topTracks);
			}
			
			mTabsAdapter.addTab(songTab, SongInfoFragment.class, songInfoArgs);
		}
		
		if(release != null){
			Bundle releaseInfoArgs = new Bundle();			
			ArrayList<SongInfo> releaseSongs = getIntent().getExtras().
												getParcelableArrayList(ReleaseInfoFragment.BUNDLE_RELEASE_SONGS);
			
			ActionBar.Tab releaseTab = actionBar.newTab().setText(R.string.album);			
			releaseInfoArgs.putParcelable(ReleaseInfoFragment.BUNDLE_RELEASE, release);
			
			if(releaseSongs != null){
				releaseInfoArgs.putParcelableArrayList(ReleaseInfoFragment.BUNDLE_RELEASE_SONGS, releaseSongs);
			}
			
			mTabsAdapter.addTab(releaseTab, ReleaseInfoFragment.class, releaseInfoArgs);
		}
		
		if(artist != null){
			Bundle artistInfoArgs = new Bundle();
			ArrayList<ReleaseInfo> artistReleases = getIntent().getExtras().
					getParcelableArrayList(ArtistInfoFragment.BUNDLE_ARTIST_RELEASES);
			
			ActionBar.Tab artistTab = actionBar.newTab().setText(R.string.artist);
			
			if(artistReleases != null){
				artistInfoArgs.putParcelableArrayList(ArtistInfoFragment.BUNDLE_ARTIST_RELEASES, artistReleases);
			}
			
			artistInfoArgs.putParcelable(ArtistInfoFragment.BUNDLE_ARTIST, artist);
			mTabsAdapter.addTab(artistTab, ArtistInfoFragment.class, artistInfoArgs);
		}
        
		//fetch index again if we are recovering the state
        if(savedInstanceState != null) {
        	actionBar.setSelectedNavigationItem(savedInstanceState.getInt("index"));
        	return;
        }    
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("index", getSupportActionBar().getSelectedNavigationIndex());        
    }	

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
		case android.R.id.home:
            final Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);			
		}
    }
    
}
