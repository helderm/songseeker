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
import com.android.songseeker.util.MediaPlayerController;
import com.android.songseeker.util.Util;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SongInfoActivity extends Activity {

	private static final int SONG_DETAILS_DIAG = 0;
	private ArrayList<SongInfo> topTracks;
	
	private static final int NOT_PLAYING = -2;
	private static final int PLAYING_OWN_SONG = -1;
	int nowPlayingId = NOT_PLAYING;
	ImageView nowPlayingIcon;
	
	private StartMediaPlayerTask mp_task = new StartMediaPlayerTask();
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	     	    
	    new GetSongDetails().execute();
	}
	
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
				SongInfoActivity.this.finish();
				return;
			}
			
			setContentView(R.layout.song_info);		

			//set data that we already have
		    SongNamesParcel songName = getIntent().getExtras().getParcelable("songName");
		    ArtistsParcel songArtist = getIntent().getExtras().getParcelable("songArtist");
		    
		    TextView tvSongName = (TextView) findViewById(R.id.songinfo_songName);
		    tvSongName.setText(songName.getSongNames().get(0));
		    
		    TextView tvSongArtist = (TextView) findViewById(R.id.songinfo_artistName);
		    tvSongArtist.setText(songArtist.getArtistList().get(0));
			
		    //set buy button
	        Button buy = (Button)findViewById(R.id.songinfo_buy);
	        buy.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
 		    		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(song.buyUrl));
		    		intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
		    		startActivity(intent);
	            }
	        });
	
	        //set album name
		    TextView tvAlbumName = (TextView) findViewById(R.id.songinfo_albumName);
		    tvAlbumName.setText(song.release.name);
			    		    
		    //set image
		    ImageView coverart = (ImageView) findViewById(R.id.songinfo_coverArt);
		    ImageLoader.getLoader(getCacheDir()).DisplayImage(song.release.image, coverart, R.drawable.blankdisc);			    
		    
		    //set top tracks
		    LinearLayout parent = (LinearLayout) findViewById(R.id.songinfo_topTracks);		    
		    LayoutInflater  inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    
		    for(final SongInfo si : topTracks){
			    
			    RelativeLayout row = (RelativeLayout) inflater.inflate(R.layout.rec_song_row, null);	    
			    
			    row.setOnClickListener(new View.OnClickListener() {					
					@Override
					public void onClick(View v) {
						SongIdsParcel songIds = new SongIdsParcel();
				    	SongNamesParcel songNames = new SongNamesParcel();
				    	ArtistsParcel songArtists = new ArtistsParcel();    	
						
				    	songIds.addSongID(si.id);
				    	songNames.addName(si.name);
				    	songArtists.addArtist(si.artist.name);
					
						Intent i = new Intent(SongInfoActivity.this, SongInfoActivity.class);
						i.putExtra("songId", songIds);
						i.putExtra("songName", songNames);
						i.putExtra("songArtist", songArtists);  
						startActivity(i);
						
					}
				});
			    
			    TextView topTrackName = (TextView)row.findViewById(R.id.recsong_firstLine);
			    TextView topTrackRelease = (TextView)row.findViewById(R.id.recsong_secondLine);
			    ImageView playpause = (ImageView) row.findViewById(R.id.recsong_playpause);
			    coverart = (ImageView) row.findViewById(R.id.recsong_coverart);		    
			    
			    topTrackName.setText(si.name);
			    topTrackRelease.setText(si.release.name);
			    playpause.setImageResource(R.drawable.play);
			    ImageLoader.getLoader(getCacheDir()).DisplayImage(si.release.image, coverart, R.drawable.blankdisc);			    
			   
			    playpause.setOnClickListener(new View.OnClickListener() {
		            public void onClick(View v) {
		            	playPausePreview(si.id, v);
		            }
		        }); 			    
			    
			    parent.addView(row);
		    }
		}		
	}
	
	
	private class StartMediaPlayerTask extends AsyncTask<String, Void, Void> implements OnCompletionListener{
		private String err = null;
		
		@Override
		protected Void doInBackground(String... trackId) {
			
			String previewURL = null;
			
			if(isCancelled())
				return null;
				
			MediaPlayerController.getCon().setOnCompletionListener(this);
			
			try{				
				previewURL = SevenDigitalComm.getComm().getPreviewUrl(trackId[0]);
			} catch(Exception e){
				err = getString(R.string.err_mediaplayer);
				Log.e(Util.APP, "EchoNest getTrack() exception!", e);
	    		return null;
			} catch(NoSuchMethodError e){
				err = getString(R.string.err_mediaplayer);
				Log.e(Util.APP, "EchoNest getTrack() error!", e);
				return null;
			}

			if(isCancelled())
				return null;
			
			try {
				MediaPlayerController.getCon().resetAndStart(previewURL);
			} catch (Exception e) {
				err = getString(R.string.err_mediaplayer);
				Log.e(Util.APP, "media player exception!", e);
				return null;
			} 
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
	    		err = null;
	    		
	    		nowPlayingId = NOT_PLAYING;
	    		if(nowPlayingIcon != null)
					nowPlayingIcon.setImageResource(R.drawable.play);
	    		
	    		nowPlayingIcon = null;
	    		return;
    		}			
		}

		public void onCompletion(MediaPlayer mp) {
			nowPlayingId = NOT_PLAYING;
			if(nowPlayingIcon != null)
				nowPlayingIcon.setImageResource(R.drawable.play);			
			
			nowPlayingIcon = null;
		}		
	}
	
	
	private void playPausePreview(String id, View v){
		int i;
		
		ImageView playpause = (ImageView) v.findViewById(R.id.recsong_playpause);
		
		for(i=0; i<topTracks.size(); i++){
			if(topTracks.get(i).id.equalsIgnoreCase(id))
				break;
		}
		
		if(nowPlayingId == i){
    		//pause preview
    		mp_task.cancel(true);
    		MediaPlayerController.getCon().stop();
    		nowPlayingId = NOT_PLAYING;
    		nowPlayingIcon = null;
    		playpause.setImageResource(R.drawable.play);
    		return;
		}			
		
		if(nowPlayingIcon != null)
			nowPlayingIcon.setImageResource(R.drawable.play);
		
    	//play preview
		playpause.setImageResource(R.drawable.pause);
		nowPlayingIcon = playpause;
		
		MediaPlayerController.getCon().stop();

    	mp_task.cancel(true);
    	mp_task = new StartMediaPlayerTask();
    	nowPlayingId = i;
    	mp_task.execute(id);
	}
}
