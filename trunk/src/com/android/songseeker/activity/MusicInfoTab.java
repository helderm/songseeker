package com.android.songseeker.activity;

import com.android.songseeker.R;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.comm.SevenDigitalComm;
import com.android.songseeker.data.ArtistInfo;
import com.android.songseeker.data.IdsParcel;
import com.android.songseeker.data.ReleaseInfo;
import com.android.songseeker.data.SongInfo;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TabHost;

import android.widget.Toast;

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
			if(song == null){
				//fetch parcel based on songId
				new GetSongDetails().execute();
				return;
			}

			intent = new Intent().setClass(this, SongInfoActivity.class);
			intent.putExtra("songParcel", song);

			spec = tabHost.newTabSpec("songs").setIndicator("Song",
					res.getDrawable(R.drawable.tab_artists))
					.setContent(intent);
			tabHost.addTab(spec);
		}

		//prepare the release info tab, if available
		if(song != null)
			release = song.release;
		else
			release = getIntent().getExtras().getParcelable("releaseParcel");
		
		if(release != null){
			intent = new Intent().setClass(this, ReleaseInfoActivity.class);
			intent.putExtra("releaseParcel", release);

			spec = tabHost.newTabSpec("albums").setIndicator("Album",
					res.getDrawable(R.drawable.tab_artists))
					.setContent(intent);
			tabHost.addTab(spec);
		}


		//prepare the artist info tab
		if(song != null)
			artist = song.artist;
		else if(release != null)
			artist = release.artist;
		else
			artist = getIntent().getExtras().getParcelable("artistParcel");
		
		if(artist != null){
			intent = new Intent().setClass(this, ArtistInfoActivity.class);
			intent.putExtra("artistParcel", artist);

			spec = tabHost.newTabSpec("artists").setIndicator("Artist",
					res.getDrawable(R.drawable.tab_artists))
					.setContent(intent);
			tabHost.addTab(spec);
		}

		tabHost.setCurrentTab(0);
	}

	private class GetSongDetails extends AsyncTask<Void, Void, SongInfo>{
		String err = null;

		@Override
		protected void onPreExecute() {
			showDialog(SONG_DETAILS_DIAG);
		}

		@Override
		protected SongInfo doInBackground(Void... arg0) {
			SongInfo song;				

			try{
				IdsParcel songIdParcel = getIntent().getExtras().getParcelable("songId");	
				song = SevenDigitalComm.getComm().querySongDetails(songIdParcel.getIds().get(0));				
			}catch(ServiceCommException e){
				err = e.getMessage();		
				return null;
			}

			return song;
		}

		@Override
		protected void onPostExecute(SongInfo song) {

			removeDialog(SONG_DETAILS_DIAG);

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
					res.getDrawable(R.drawable.tab_artists))
					.setContent(intent);
			tabHost.addTab(spec);

			//prepare the release info tab
			intent = new Intent().setClass(MusicInfoTab.this, ReleaseInfoActivity.class);
			intent.putExtra("releaseParcel", song.release);

			spec = tabHost.newTabSpec("albums").setIndicator("Album",
					res.getDrawable(R.drawable.tab_artists))
					.setContent(intent);
			tabHost.addTab(spec);

			//prepare the artist info tab			
			intent = new Intent().setClass(MusicInfoTab.this, ArtistInfoActivity.class);
			intent.putExtra("artistParcel", song.artist);

			spec = tabHost.newTabSpec("artists").setIndicator("Artist",
					res.getDrawable(R.drawable.tab_artists))
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

}
