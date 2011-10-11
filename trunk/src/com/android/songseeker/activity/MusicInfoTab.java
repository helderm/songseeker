package com.android.songseeker.activity;

import com.android.songseeker.R;
import com.android.songseeker.data.ArtistInfo;
import com.android.songseeker.data.IdsParcel;
import com.android.songseeker.data.ReleaseInfo;
import com.android.songseeker.data.SongInfo;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class MusicInfoTab extends TabActivity {

	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    IdsParcel songId = null;
	    SongInfo song = null;
	    ReleaseInfo release = null;
	    ArtistInfo artist = null;
	    
	    setContentView(R.layout.music_info_tab);

	    Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    
	    
	    //prepare the song info tab, if available
	    song = getIntent().getExtras().getParcelable("songParcel");
	    songId = getIntent().getExtras().getParcelable("songId");
	    if(song != null || songId != null){
	    	intent = new Intent().setClass(this, SongInfoActivity.class);
	    	
	    	if(song != null)
	    		intent.putExtra("songParcel", song);
	    	else 
	    		intent.putExtra("songId", songId);
	    	
		    spec = tabHost.newTabSpec("songs").setIndicator("Songs",
		                      res.getDrawable(R.drawable.tab_artists))
		                  .setContent(intent);
		    tabHost.addTab(spec);	    	
	    }

	    //prepare the release info tab, if available
	    release = getIntent().getExtras().getParcelable("releaseParcel");
	    if(release != null || song != null || songId != null){
	    	intent = new Intent().setClass(this, ReleaseInfoActivity.class);
	    	
	    	if(release != null)
	    		intent.putExtra("releaseParcel", release);
	    	
		    intent = new Intent().setClass(this, ReleaseInfoActivity.class);
		    spec = tabHost.newTabSpec("albums").setIndicator("Albums",
		                      res.getDrawable(R.drawable.tab_artists))
		                  .setContent(intent);
		    tabHost.addTab(spec);
	    }
	    
   
	    //prepare the artist info tab
	    intent = new Intent().setClass(this, ArtistInfoActivity.class);
	    artist = getIntent().getExtras().getParcelable("artistParcel");
	    if(artist != null)
	    	intent.putExtra("artistParcel", artist);
	    
	    spec = tabHost.newTabSpec("artists").setIndicator("Artists",
	                      res.getDrawable(R.drawable.tab_artists))
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    tabHost.setCurrentTab(0);
	}

}
