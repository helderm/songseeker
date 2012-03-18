package com.seekermob.songseeker.activity;

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.ReleaseInfo;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.util.TrackedTabActivity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TabHost;

import android.widget.Toast;

@SuppressWarnings("deprecation")
public class MusicInfoTab extends TrackedTabActivity {
	
	private SongInfo song = null;
	private ReleaseInfo release = null;
	private ArtistInfo artist = null;
	
	private GetSongDetails task = null;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		boolean isFromRecSongs = true;

		Resources res = getResources(); // Resource object to get Drawables
		TabHost tabHost = getTabHost();  // The activity TabHost
		TabHost.TabSpec spec;  // Resusable TabSpec for each tab
		Intent intent;  // Reusable Intent for each tab
        
		//check for orientation change
		SongInfo savedSong = (SongInfo)getLastNonConfigurationInstance();
		if(savedSong != null){
						
			if(savedSong.id != null){
				song = savedSong;
				isFromRecSongs = false;
			}
			
			if(savedSong.release != null)
				release = savedSong.release;
			
			if(savedSong.artist != null)
				artist = savedSong.artist;
		}
		
		if(song == null){
			//prepare the song info tab, if available
			song = getIntent().getExtras().getParcelable("songId");
			
			if(song == null){
				isFromRecSongs = false;
				song = getIntent().getExtras().getParcelable("songParcel");
			}
		}

		if(song != null){
			//if we got here from RecSongs
			if(isFromRecSongs){
				
				//check if we have all the info needed for the tabs
				//call 7digital ws if we dont
				if(song.buyUrl == null || song.artist.buyUrl == null || song.artist.id == null ||
					song.release.artist == null || song.release.buyUrl == null || song.release.id == null || 
					song.release.image == null || song.release.name == null){

					//fetch parcel based on songId
					task = new GetSongDetails();
					task.execute(song);
					return;
					
				}
			}

			intent = new Intent().setClass(this, SongInfoActivity.class);
			intent.putExtra("songParcel", song);

			//pass the top songs to the tab if we have it
			if(getIntent().getExtras().getParcelableArrayList("artistTopSongs") != null){
				intent.putParcelableArrayListExtra("artistTopSongs", getIntent().getExtras().getParcelableArrayList("artistTopSongs"));
			}
			
			spec = tabHost.newTabSpec("songs").setIndicator("Song",
					res.getDrawable(R.drawable.ic_tab_songs))
					.setContent(intent);
			tabHost.addTab(spec);
		}

		//prepare the release info tab, if available
		if(release == null){
			if(song != null && song.release != null)
				release = song.release;
			else
				release = getIntent().getExtras().getParcelable("releaseParcel");
		}

		if(release != null){
			intent = new Intent().setClass(this, ReleaseInfoActivity.class);
			intent.putExtra("releaseParcel", release);

			if(getIntent().getExtras().getParcelableArrayList("releaseSongList") != null){
				intent.putParcelableArrayListExtra("releaseSongList", getIntent().getExtras().getParcelableArrayList("releaseSongList"));
			}
			
			spec = tabHost.newTabSpec("albums").setIndicator("Album",
					res.getDrawable(R.drawable.ic_tab_albums))
					.setContent(intent);
			tabHost.addTab(spec);
		}

		//prepare the artist info tab
		if(artist == null){
			if(song != null && song.artist != null)
				artist = song.artist;
			else if(release != null)
				artist = release.artist;
			else
				artist = getIntent().getExtras().getParcelable("artistParcel");
		}

		if(artist != null){
			intent = new Intent().setClass(this, ArtistInfoActivity.class);
			intent.putExtra("artistParcel", artist);

			if(getIntent().getExtras().getParcelableArrayList("artistReleases") != null){
				intent.putParcelableArrayListExtra("artistReleases", getIntent().getExtras().getParcelableArrayList("artistReleases"));
			}
			
			spec = tabHost.newTabSpec("artists").setIndicator("Artist",
					res.getDrawable(R.drawable.ic_tab_artists))
					.setContent(intent);
			tabHost.addTab(spec);
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		removeDialog(SONG_DETAILS_DIAG);
		
		if(task != null)
			task.cancel(true);		
		
	}

	private class GetSongDetails extends AsyncTask<SongInfo, Void, SongInfo>{
		String err = null;

		@Override
		protected void onPreExecute() {
			showDialog(SONG_DETAILS_DIAG);
		}

		@Override
		protected SongInfo doInBackground(SongInfo... args) {
			SongInfo song = args[0];				

			try{
				song = SevenDigitalComm.getComm().querySongDetails(song.id, song.name, song.artist.name, getApplicationContext());				
			}catch(ServiceCommException e){
				err = e.getMessage();		
				return null;
			}

			return song;
		}

		@Override
		protected void onPostExecute(SongInfo s) {

			removeDialog(SONG_DETAILS_DIAG);
			
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				MusicInfoTab.this.finish();
				return;
			}			

			song = s;
			release = s.release;
			artist = s.artist;

			Resources res = getResources(); // Resource object to get Drawables
			TabHost tabHost = getTabHost();  // The activity TabHost
			TabHost.TabSpec spec;  // Resusable TabSpec for each tab
			Intent intent;  // Reusable Intent for each tab

			//prepare the song info tab
			intent = new Intent().setClass(MusicInfoTab.this, SongInfoActivity.class);
			intent.putExtra("songParcel", song);

			spec = tabHost.newTabSpec("songs").setIndicator("Song",
					res.getDrawable(R.drawable.ic_tab_songs))
					.setContent(intent);
			tabHost.addTab(spec);

			//prepare the release info tab
			intent = new Intent().setClass(MusicInfoTab.this, ReleaseInfoActivity.class);
			intent.putExtra("releaseParcel", song.release);

			spec = tabHost.newTabSpec("albums").setIndicator("Album",
					res.getDrawable(R.drawable.ic_tab_albums))
					.setContent(intent);
			tabHost.addTab(spec);

			//prepare the artist info tab			
			intent = new Intent().setClass(MusicInfoTab.this, ArtistInfoActivity.class);
			intent.putExtra("artistParcel", song.artist);

			spec = tabHost.newTabSpec("artists").setIndicator("Artist",
					res.getDrawable(R.drawable.ic_tab_artists))
					.setContent(intent);
			tabHost.addTab(spec);
			

			tabHost.setCurrentTab(0);
		}		
	}

	private static final int SONG_DETAILS_DIAG = 0;
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case SONG_DETAILS_DIAG:
			ProgressDialog pd = new ProgressDialog(this);
			pd.setMessage("Fetching song details...");
			pd.setIndeterminate(true);
			pd.setCancelable(true);
			return pd;
		default:
			return null;		    	
		}
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		SongInfo savedSong = new SongInfo();
		
		removeDialog(SONG_DETAILS_DIAG);
		
		if(song != null){
			savedSong.buyUrl = song.buyUrl;
			savedSong.duration = song.duration;
			savedSong.id = song.id;
			savedSong.name = song.name;
			savedSong.previewUrl = song.previewUrl;
			savedSong.trackNum = song.trackNum;
			savedSong.version = song.version;		
		}else{
			savedSong.id = null;
		}
		
		if(release != null){
			savedSong.release.artist = release.artist;
			savedSong.release.buyUrl = release.buyUrl;
			savedSong.release.id = release.id;
			savedSong.release.image = release.image;
			savedSong.release.name = release.name;					
		}else if(song != null && song.release != null){			
			savedSong.release.artist = song.release.artist;
			savedSong.release.buyUrl = song.release.buyUrl;
			savedSong.release.id = song.release.id;
			savedSong.release.image = song.release.image;
			savedSong.release.name = song.release.name;	
		} else{
			savedSong.release = null;
		}
		
		if(artist != null){
			savedSong.artist.buyUrl = artist.buyUrl;
			savedSong.artist.id = artist.id;
			savedSong.artist.image = artist.image;
			savedSong.artist.name = artist.name;
		} else if(song != null && song.artist != null){
			savedSong.artist.buyUrl = song.artist.buyUrl;
			savedSong.artist.id = song.artist.id;
			savedSong.artist.image = song.artist.image;
			savedSong.artist.name = song.artist.name;
		}else
			savedSong.artist = null;			
		
		return savedSong;
	}

}
