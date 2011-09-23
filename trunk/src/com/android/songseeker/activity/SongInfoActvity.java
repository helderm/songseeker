package com.android.songseeker.activity;

import java.util.ArrayList;

import com.android.songseeker.R;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.comm.SevenDigitalComm;
import com.android.songseeker.data.ArtistsParcel;
import com.android.songseeker.data.SongIdsParcel;
import com.android.songseeker.data.SongInfo;
import com.android.songseeker.data.SongNamesParcel;
import com.android.songseeker.util.ImageLoader;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SongInfoActvity extends Activity {

	private static final int SONG_DETAILS_DIAG = 0;
	private ImageLoader imageLoader;
	
	private ArrayList<SongInfo> topTracks;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setContentView(R.layout.song_info);
	    
	    imageLoader = new ImageLoader(getApplicationContext());
	    
	    SongNamesParcel songName = getIntent().getExtras().getParcelable("songName");
	    ArtistsParcel songArtist = getIntent().getExtras().getParcelable("songArtist");
	    
	    TextView tvSongName = (TextView) findViewById(R.id.songinfo_songName);
	    tvSongName.setText(songName.getSongNames().get(0));
	    
	    TextView tvSongArtist = (TextView) findViewById(R.id.songinfo_artistName);
	    tvSongArtist.setText(songArtist.getArtistList().get(0));
	    
	    new GetSongDetails().execute();
	}
	
	@Override
    protected Dialog onCreateDialog(int id) {
		switch(id){
		case SONG_DETAILS_DIAG:
		    ProgressDialog pd = new ProgressDialog(this);
		    pd.setMessage("Fetching song details...");
		    pd.setIndeterminate(true);
		    pd.setCancelable(false);
		    return pd;
		default:
			return null;		    	
		}
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
			
			SongIdsParcel songIdParcel = getIntent().getExtras().getParcelable("songId");			
			
			try{
				song = SevenDigitalComm.getComm().querySongDetails(songIdParcel.getSongIDs().get(0));
				topTracks = SevenDigitalComm.getComm().queryArtistTopTracks(song.artist.id);
			}catch(ServiceCommException e){
				err = e.getMessage();		
				return null;
			}
			
			return song;
		}
		
		@Override
		protected void onPostExecute(SongInfo s) {
			final SongInfo song = s;
			
			removeDialog(SONG_DETAILS_DIAG);
			
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				SongInfoActvity.this.finish();
				return;
			}
			
	        Button buy = (Button)findViewById(R.id.songinfo_buy);
	        buy.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
 		    		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(song.buyUrl));
		    		intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
		    		startActivity(intent);
	            }
	        });
			
		    TextView tvAlbumName = (TextView) findViewById(R.id.songinfo_albumName);
		    tvAlbumName.setText(song.release.name);
		    tvAlbumName.setOnClickListener(new View.OnClickListener() {
		    	public void onClick(View v) {
		    		//Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(song.release.buyUrl));
		    		//intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
		    		//startActivity(intent);
		    	}
		    });
		    		    
		    //set image
		    ImageView coverart = (ImageView) findViewById(R.id.songinfo_coverArt);
		    imageLoader.DisplayImage(song.release.image, SongInfoActvity.this, coverart, R.drawable.blankdisc);	
		    
		    
		    //set toptracks
		    //TextView topTrackName1 = (TextView)((View)findViewById(R.id.songinfo_toptrack1)).findViewById(R.id.recsong_firstLine);
		    //topTrackName1.setText(topTracks.get(0).name);
		    
		}
		
	}
	
	@Override
	protected void onDestroy() {
		imageLoader.stopThread();
		imageLoader.clearCache();
		super.onDestroy();
	}

}
