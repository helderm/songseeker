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

import android.app.Dialog;
import android.app.ListActivity;
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
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SongInfoActivity extends ListActivity {

	private SongInfo song;
	
	private static final int SONG_DETAILS_DIAG = 0;
	private TopTracksAdapter adapter = new TopTracksAdapter();;

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

	private class GetSongDetails extends AsyncTask<Void, Void, Void>{
		String err = null;


		@Override
		protected void onPreExecute() {
			showDialog(SONG_DETAILS_DIAG);
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			ArrayList<SongInfo> topTracks;	

			SongIdsParcel songIdParcel = getIntent().getExtras().getParcelable("songId");			

			try{
				song = SevenDigitalComm.getComm().querySongDetails(songIdParcel.getSongIDs().get(0));
				topTracks = SevenDigitalComm.getComm().queryArtistTopTracks(song.artist.id);
			}catch(ServiceCommException e){
				err = e.getMessage();		
				return null;
			}
			
			adapter.setTopTracks(topTracks);

			return null;
		}

		@Override
		protected void onPostExecute(Void s) {

			removeDialog(SONG_DETAILS_DIAG);

			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				SongInfoActivity.this.finish();
				return;
			}

			//set content for main screen
			setContentView(R.layout.listview);		

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
					playPausePreview(song);
				}
			}); 

			getListView().addHeaderView(header);

			//set adapter for top tracks
			SongInfoActivity.this.setListAdapter(adapter); 	

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
		}		
	}	

	private class TopTracksAdapter extends BaseAdapter {

		private ArrayList<SongInfo> topTracks;    
		private int nowPlayingID;
		public static final int NOT_PLAYING = -1;
		public static final int PLAYING_OWN_SONG = -2;

		public TopTracksAdapter() {    
			topTracks = null;
			nowPlayingID = NOT_PLAYING;
		}

		public int getCount() {
			if(topTracks == null)
				return 0;

			return topTracks.size();
		}

		public SongInfo getItem(int position) {
			return topTracks.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		/*public class ViewHolder{
	    	public TextView username;
	    	public TextView message;
	    	public ImageView image;
	    }*/

		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.rec_song_row, null);
			}

			final SongInfo song = getItem(position);
			if (song != null) {
				TextView tt = (TextView) v.findViewById(R.id.recsong_firstLine);
				TextView bt = (TextView) v.findViewById(R.id.recsong_secondLine);
				ImageView coverart = (ImageView) v.findViewById(R.id.recsong_coverart);
				ImageView playpause = (ImageView) v.findViewById(R.id.recsong_playpause);

				bt.setText(song.release.name);
				tt.setText(song.name);

				if(nowPlayingID == position)
					playpause.setImageResource(R.drawable.pause);
				else
					playpause.setImageResource(R.drawable.play);

				playpause.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						playPausePreview(song);
					}
				}); 			    

				ImageLoader.getLoader(getCacheDir()).DisplayImage(song.release.image, coverart, R.drawable.blankdisc);

			}

			return v;
		}

		public void setTopTracks(ArrayList<SongInfo> tp){
			this.topTracks = tp;
			//notifyDataSetChanged();
		}

		public void setNowPlaying(int position){			
			
			nowPlayingID = position;
			notifyDataSetChanged();
		}
		
		public void setNowPlaying(SongInfo song){
			int i;
			
			for(i=0; i<topTracks.size(); i++){
				if(topTracks.get(i).id.equalsIgnoreCase(song.id))
					break;
			}
			
			nowPlayingID = i;
			notifyDataSetChanged();
		}

		public boolean isPlaying(SongInfo song){
			int i;
			
			for(i=0; i<topTracks.size(); i++){
				if(topTracks.get(i).id.equalsIgnoreCase(song.id))
					break;
			}
			
			if(nowPlayingID == i)
				return true;

			return false;
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		SongInfo si = adapter.getItem(position);

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

	private class StartMediaPlayerTask extends AsyncTask<SongInfo, Void, Void> implements OnCompletionListener{
		private String err = null;

		@Override
		protected Void doInBackground(SongInfo... song) {


			if(isCancelled())
				return null;

			MediaPlayerController.getCon().setOnCompletionListener(this);

			if(song[0].previewUrl == null){
				
				try{
					song[0].previewUrl = SevenDigitalComm.getComm().getPreviewUrl(song[0].id);
				} catch(Exception e){
					err = getString(R.string.err_mediaplayer);
					Log.e(Util.APP, "EchoNest getTrack() exception!", e);
					return null;
				} catch(NoSuchMethodError e){
					err = getString(R.string.err_mediaplayer);
					Log.e(Util.APP, "EchoNest getTrack() error!", e);
					return null;
				}
			}

			if(isCancelled())
				return null;

			try {
				MediaPlayerController.getCon().resetAndStart(song[0].previewUrl);
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
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();

				err = null;
				adapter.setNowPlaying(TopTracksAdapter.NOT_PLAYING);
				return;
			}			
		}

		public void onCompletion(MediaPlayer mp) {
			adapter.setNowPlaying(TopTracksAdapter.NOT_PLAYING);			
		}		
	}	

	private void playPausePreview(SongInfo song){
		//SongInfo song = adapter.getItem(position);

		if(song == null || adapter.isPlaying(song)){
			//pause preview
			mp_task.cancel(true);
			MediaPlayerController.getCon().stop();
			adapter.setNowPlaying(TopTracksAdapter.NOT_PLAYING);
			return;
		}

		//play preview
		MediaPlayerController.getCon().stop();

		mp_task.cancel(true);
		mp_task = new StartMediaPlayerTask();
		adapter.setNowPlaying(song);
		mp_task.execute(song);
	}
}
