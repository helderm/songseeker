package com.seekermob.songseeker.activity;

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.ReleaseInfo;
import com.seekermob.songseeker.data.SongInfo;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.widget.TabHost;

import android.widget.Toast;

@SuppressWarnings("deprecation")
public class MusicInfoTab extends TabActivity {
	
	private SongInfo song = null;
	private ReleaseInfo release = null;
	private ArtistInfo artist = null;
	
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
		
		//if we dont have froyo, then we cant call the async task 
		if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.ECLAIR_MR1){
			if(song != null){
				
				intent = new Intent().setClass(this, SongInfoActivity.class);
				if(!isFromRecSongs){
					intent.putExtra("songParcel", song);
				}else{
					intent.putExtra("songId", song);
				}			

				spec = tabHost.newTabSpec("songs").setIndicator("Song",
						res.getDrawable(R.drawable.ic_tab_songs))
						.setContent(intent);
				tabHost.addTab(spec);
			}

			//prepare the release info tab, if available
			if(song != null && song.release != null)
				release = song.release;
			else
				release = getIntent().getExtras().getParcelable("releaseParcel");
			
			if(release != null){
				intent = new Intent().setClass(this, ReleaseInfoActivity.class);
				
				if(!isFromRecSongs)
					intent.putExtra("releaseParcel", release);
				else
					intent.putExtra("songId", song);

				spec = tabHost.newTabSpec("albums").setIndicator("Album",
						res.getDrawable(R.drawable.ic_tab_albums))
						.setContent(intent);
				tabHost.addTab(spec);
			}


			//prepare the artist info tab
			if(song != null && song.artist != null)
				artist = song.artist;
			else if(release != null)
				artist = release.artist;
			else
				artist = getIntent().getExtras().getParcelable("artistParcel");
			
			if(artist != null){
				intent = new Intent().setClass(this, ArtistInfoActivity.class);
								
				if(!isFromRecSongs)
					intent.putExtra("artistParcel", artist);
				else
					intent.putExtra("songId", song);
				
				spec = tabHost.newTabSpec("artists").setIndicator("Artist",
						res.getDrawable(R.drawable.ic_tab_artists))
						.setContent(intent);
				tabHost.addTab(spec);
			}
		}else{
			if(song != null){
				//if we got here from RecSongs
				if(isFromRecSongs){
					//fetch parcel based on songId
					new GetSongDetails().execute(song);
					return;
				}

				intent = new Intent().setClass(this, SongInfoActivity.class);
				intent.putExtra("songParcel", song);

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

				spec = tabHost.newTabSpec("artists").setIndicator("Artist",
						res.getDrawable(R.drawable.ic_tab_songs))
						.setContent(intent);
				tabHost.addTab(spec);
			}
		}
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
				song = SevenDigitalComm.getComm().querySongDetails(song.id, song.name, song.artist.name);				
			}catch(ServiceCommException e){
				err = e.getMessage();		
				return null;
			}

			return song;
		}

		@Override
		protected void onPostExecute(SongInfo s) {

			removeDialog(SONG_DETAILS_DIAG);

			song = s;
			release = s.release;
			artist = s.artist;
			
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				MusicInfoTab.this.finish();
				return;
			}

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
		}else
			savedSong.release = null;
		
		if(artist != null){
			savedSong.artist.buyUrl = artist.buyUrl;
			savedSong.artist.id = artist.id;
			savedSong.artist.image = artist.image;
			savedSong.artist.name = artist.name;
		}else
			savedSong.artist = null;			
		
		return savedSong;
	}

}
