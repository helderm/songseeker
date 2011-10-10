package com.android.songseeker.activity;

import java.util.ArrayList;

import com.android.songseeker.R;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.comm.SevenDigitalComm;
import com.android.songseeker.data.IdsParcel;
import com.android.songseeker.data.SongInfo;
import com.android.songseeker.util.ImageLoader;
import com.android.songseeker.util.MediaPlayerController;
import com.android.songseeker.util.Util;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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
	private TopTracksAdapter adapter = new TopTracksAdapter();

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

	@Override
	protected void onPause() {
		MediaPlayerController.getCon().release();
		super.onPause();
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

			try{
				//if we already have the info, dont query it again
				song = getIntent().getExtras().getParcelable("songParcel");	
				if(song == null){
					IdsParcel songIdParcel = getIntent().getExtras().getParcelable("songId");	
					song = SevenDigitalComm.getComm().querySongDetails(songIdParcel.getIds().get(0));					
				}	

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

			TextView tvSongName = (TextView) header.findViewById(R.id.songinfo_songName);
			tvSongName.setText(song.name);

			TextView tvSongVersion = (TextView) header.findViewById(R.id.songinfo_songVersion);
			if(song.version != null)
				tvSongVersion.setText(song.version);
			else
				tvSongVersion.setVisibility(View.GONE);

			TextView tvSongArtist = (TextView) header.findViewById(R.id.songinfo_artistName);
			tvSongArtist.setText(song.artist.name);
			tvSongArtist.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent(SongInfoActivity.this, ArtistInfoActivity.class);
					i.putExtra("artistParcel", song.artist);
					startActivity(i);
				}
			});

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

			//set share button
			Button share = (Button)header.findViewById(R.id.songinfo_share);
			share.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					final Intent intent = new Intent(Intent.ACTION_SEND);					 
					intent.setType("text/plain");
					intent.putExtra(Intent.EXTRA_SUBJECT, "New song!");
					intent.putExtra(Intent.EXTRA_TEXT, "I discovered the song '"+ song.name + " by "+ song.artist.name +
							"' using the Song Seeker app for Android! Check it out! "+song.buyUrl);
					startActivity(Intent.createChooser(intent, "Share using..."));
				}				
			});

			//set album name
			TextView tvAlbumName = (TextView) header.findViewById(R.id.songinfo_albumName);
			tvAlbumName.setText(song.release.name);
			tvAlbumName.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent(SongInfoActivity.this, ReleaseInfoActivity.class);
					i.putExtra("releaseParcel", song.release);
					startActivity(i);
				}
			});

			//set image
			ImageView coverart = (ImageView) header.findViewById(R.id.songinfo_coverArt);
			ImageLoader.getLoader(getCacheDir()).DisplayImage(song.release.image, coverart, R.drawable.blankdisc);

			getListView().addHeaderView(header);

			//set adapter for top tracks
			SongInfoActivity.this.setListAdapter(adapter); 	
		}		
	}	

	private class TopTracksAdapter extends BaseAdapter {

		private ArrayList<SongInfo> topTracks;    

		public TopTracksAdapter() {    
			topTracks = null;
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
			final int pos = position;
			if (song != null) {
				TextView tt = (TextView) v.findViewById(R.id.recsong_firstLine);
				TextView bt = (TextView) v.findViewById(R.id.recsong_secondLine);
				ImageView coverart = (ImageView) v.findViewById(R.id.recsong_coverart);
				ImageView playpause = (ImageView) v.findViewById(R.id.recsong_playpause);

				bt.setText(song.release.name);
				tt.setText(song.name);

				switch(MediaPlayerController.getCon().getStatus(pos)){				
				case PLAYING:
					playpause.setImageResource(R.drawable.pause);
					break;
				case LOADING:
				case PREPARED:
					playpause.setImageResource(R.drawable.icon);
					break;				
				case STOPPED:
				default:
					playpause.setImageResource(R.drawable.play);
					break;					
				}

				playpause.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						mp_task.cancel(true);
						mp_task = new StartMediaPlayerTask();
						mp_task.position = pos;
						mp_task.execute(song);
					}
				}); 			    

				ImageLoader.getLoader(getCacheDir()).DisplayImage(song.release.image, coverart, R.drawable.blankdisc);

			}

			return v;
		}

		public void setTopTracks(ArrayList<SongInfo> tp){
			this.topTracks = tp;
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		SongInfo si = adapter.getItem(position-1);
		Intent i = new Intent(SongInfoActivity.this, SongInfoActivity.class);
		i.putExtra("songParcel", si);
		startActivity(i);
	}

	private class StartMediaPlayerTask extends AsyncTask<SongInfo, Void, SongInfo>{
		private String err = null;
		public int position = 0;
		public ImageView icon = null;
		
		@Override
		protected SongInfo doInBackground(SongInfo... song) {
			
			if(isCancelled())
				return null;

			if(song[0].previewUrl == null){

				try{
					song[0].previewUrl = SevenDigitalComm.getComm().getPreviewUrl(song[0].id);
				} catch(Exception e){
					err = getString(R.string.err_mediaplayer);
					Log.e(Util.APP, "7digital getPreviewUrl() exception!", e);
					return null;
				} 
			}

			return song[0];
		}

		@Override
		protected void onPostExecute(SongInfo song) {
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();
				err = null;
				return;
			}	

			if(!isCancelled()){
				if(icon != null)
					MediaPlayerController.getCon().startStopMedia(song.previewUrl, icon);
				else
					MediaPlayerController.getCon().startStopMedia(song.previewUrl, position, adapter);
			}
		}
	}	
}
