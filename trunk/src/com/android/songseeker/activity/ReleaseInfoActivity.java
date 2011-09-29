package com.android.songseeker.activity;

import java.util.ArrayList;

import com.android.songseeker.R;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.comm.SevenDigitalComm;
import com.android.songseeker.data.ArtistsParcel;
import com.android.songseeker.data.IdsParcel;
import com.android.songseeker.data.ReleaseInfo;
import com.android.songseeker.data.SongInfo;
import com.android.songseeker.data.SongNamesParcel;
import com.android.songseeker.util.ImageLoader;
import com.android.songseeker.util.MediaPlayerController;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ReleaseInfoActivity extends ListActivity {

	private ReleaseInfo release;
	
	private static final int RELEASE_DETAILS_DIAG = 0;
	//private ReleaseTracksAdapter adapter = new ReleaseTracksAdapter();;

	//private StartMediaPlayerTask mp_task = new StartMediaPlayerTask();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		new GetReleaseDetails().execute();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case RELEASE_DETAILS_DIAG:
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
	protected void onPause() {
		MediaPlayerController.getCon().release();
		super.onPause();
	}
	
	private class GetReleaseDetails extends AsyncTask<Void, Void, Void>{
		String err = null;		

		@Override
		protected void onPreExecute() {
			showDialog(RELEASE_DETAILS_DIAG);
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			//ArrayList<SongInfo> topTracks;	

			IdsParcel releaseIdParcel = getIntent().getExtras().getParcelable("releaseId");			

			try{
				release = SevenDigitalComm.getComm().queryReleaseDetails(releaseIdParcel.getIds().get(0));
				//topTracks = SevenDigitalComm.getComm().queryArtistTopTracks(song.artist.id);
			}catch(ServiceCommException e){
				err = e.getMessage();		
				return null;
			}
			
			//adapter.setTopTracks(topTracks);

			return null;
		}

		@Override
		protected void onPostExecute(Void s) {

			removeDialog(RELEASE_DETAILS_DIAG);

			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				ReleaseInfoActivity.this.finish();
				return;
			}

			//set content for main screen
			setContentView(R.layout.listview);		

			/*
			//set song info header
			LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			LinearLayout header = (LinearLayout)inflater.inflate(R.layout.song_info, null);

			//set data that we already have
			SongNamesParcel songName = getIntent().getExtras().getParcelable("songName");
			ArtistsParcel songArtist = getIntent().getExtras().getParcelable("songArtist");

			TextView tvSongName = (TextView) header.findViewById(R.id.songinfo_songName);
			tvSongName.setText(songName.getSongNames().get(0));

			TextView tvSongArtist = (TextView) header.findViewById(R.id.songinfo_artistName);
			tvSongArtist.setText(songArtist.getArtistList().get(0));

			if(songName.getSongNames().size() > 1)
				song.previewUrl = songName.getSongNames().get(1); 

			ImageView playpause = (ImageView) header.findViewById(R.id.songinfo_playpause);
			playpause.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mp_task.cancel(true);
					mp_task = new StartMediaPlayerTask();
					mp_task.icon = (ImageView) v;
					mp_task.execute(song);
				}
			}); 

			//set buy button
			Button buy = (Button)header.findViewById(R.id.songinfo_buy);
			buy.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(song.buyUrl));
					intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
			});

			//set album name
			TextView tvAlbumName = (TextView) header.findViewById(R.id.songinfo_albumName);
			tvAlbumName.setText(song.release.name);
			tvAlbumName.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					IdsParcel releaseId = new IdsParcel();
					releaseId.addSongID(song.release.id);					
					Intent i = new Intent(SongInfoActivity.this, ReleaseInfoActivity.class);
					i.putExtra("releaseId", releaseId);
					startActivity(i);
				}
			});

			//set image
			ImageView coverart = (ImageView) header.findViewById(R.id.songinfo_coverArt);
			ImageLoader.getLoader(getCacheDir()).DisplayImage(song.release.image, coverart, R.drawable.blankdisc);
			
			getListView().addHeaderView(header);

			//set adapter for top tracks
			ReleaseInfoActivity.this.setListAdapter(adapter); */	
		}		
	}	

}
