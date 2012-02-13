package com.seekermob.songseeker.activity;

import java.util.ArrayList;

import com.google.android.apps.analytics.easytracking.TrackedListActivity;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.data.RecSongsPlaylist;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;
import com.seekermob.songseeker.util.MediaPlayerController;
import com.seekermob.songseeker.util.MediaPlayerController.MediaStatus;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SongInfoActivity extends TrackedListActivity {

	private SongInfo song;

	private static final int SONG_DETAILS_DIAG = 0;
	private TopTracksAdapter adapter;
	private GetSongDetails task;

	private StartMediaPlayerTask mp_task = new StartMediaPlayerTask();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		adapter = new TopTracksAdapter();	
		song = getIntent().getExtras().getParcelable("songParcel");
				
		//check orientation change
		@SuppressWarnings("unchecked")
		ArrayList<SongInfo> savedTopTracks = (ArrayList<SongInfo>) getLastNonConfigurationInstance();
		
		if(savedTopTracks == null){		
			task = new GetSongDetails();
			task.execute();
		}else{
			setListHeader();
			adapter.setTopTracks(savedTopTracks);
		}
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
			
			try{
				showDialog(SONG_DETAILS_DIAG);
			}catch(RuntimeException e){
				//this is thrown when the user press back quickly
				task.cancel(true);				
			}
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			ArrayList<SongInfo> topTracks;						

			if(isCancelled())
				return null;
			
			try{
				//if we already have the info, dont query it again
				//song = getIntent().getExtras().getParcelable("songParcel");	
				if(song == null){
					//this will happen only on Eclair, see MusicInfoTab
					song = getIntent().getExtras().getParcelable("songId");	
					song = SevenDigitalComm.getComm().querySongDetails(song.id, song.name, song.artist.name);		
				}	

				if(isCancelled())
					return null;
				
				topTracks = SevenDigitalComm.getComm().queryArtistTopTracks(song.artist.id);
				
			}catch(ServiceCommException e){
				err = e.getMessage();		
				return null;
			}

			if(isCancelled())
				return null;
			
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

			setListHeader();
		}		
	}	

	private class TopTracksAdapter extends BaseAdapter {

		private ArrayList<SongInfo> topTracks;   
		private LayoutInflater inflater;

		public TopTracksAdapter() {    
			topTracks = null;
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			
			if (convertView == null) {
			   	convertView = inflater.inflate(R.layout.list_row, null);
				
				holder = new ViewHolder();
				holder.topText = (TextView) convertView.findViewById(R.id.firstLine);
			    holder.botText = (TextView) convertView.findViewById(R.id.secondLine);
			    holder.coverArt = (ImageView) convertView.findViewById(R.id.coverart);
			    holder.mediaBtns = (FrameLayout) convertView.findViewById(R.id.media_btns);
			    holder.playPause = (ImageView) convertView.findViewById(R.id.playpause);
			    holder.loading = (ProgressBar) convertView.findViewById(R.id.loading);
			    
			    convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}

			final SongInfo song = getItem(position);
			final int pos = position;
			
			if(song == null){
				return convertView;
			}

			holder.botText.setText(song.release.name);
			holder.topText.setText(song.name);
			holder.mediaBtns.setVisibility(View.VISIBLE);
			
			MediaStatus mediaStatus = MediaPlayerController.getCon().getStatus(pos);
			
			//control visibility of the media icon
			switch(mediaStatus){				
			case PLAYING:
				holder.loading.setVisibility(View.GONE);
				holder.playPause.setVisibility(View.VISIBLE);
				holder.playPause.setImageResource(R.drawable.ic_image_pause);
				break;
			case LOADING:
			case PREPARED:
				holder.playPause.setVisibility(View.GONE);
				holder.loading.setVisibility(View.VISIBLE);					
				break;				
			case STOPPED:
			default:
				holder.loading.setVisibility(View.GONE);
				holder.playPause.setVisibility(View.VISIBLE);
				holder.playPause.setImageResource(R.drawable.ic_image_play);
				break;					
			}
			
			//control onClickListeners
			switch(mediaStatus){
			case LOADING:
			case PREPARED:
				holder.loading.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						mp_task.cancel(true);
						mp_task = new StartMediaPlayerTask();
						mp_task.position = pos;
						mp_task.execute(song);
					}
				}); 
				break;
			case PLAYING:
			case STOPPED:
			default:
				holder.playPause.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						mp_task.cancel(true);
						mp_task = new StartMediaPlayerTask();
						mp_task.position = pos;
						mp_task.execute(song);
					}
				}); 
				break;
			}			    

			ImageLoader.getLoader(getCacheDir()).DisplayImage(song.release.image, holder.coverArt, R.drawable.ic_disc_stub, ImageSize.SMALL);			

			return convertView;
		}

		public void setTopTracks(ArrayList<SongInfo> tp){
			this.topTracks = tp;
		}
		
	    private class ViewHolder{
	    	public TextView topText;
	    	public TextView botText;
	    	public ImageView coverArt;
	    	public ImageView playPause;
	    	public ProgressBar loading;
	    	public FrameLayout mediaBtns;
	    }
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		SongInfo si = adapter.getItem(position-1);
		Intent i = new Intent(SongInfoActivity.this, MusicInfoTab.class);
		i.putExtra("songParcel", si);
		startActivity(i);
	}

	private class StartMediaPlayerTask extends AsyncTask<SongInfo, Void, SongInfo>{
		private String err = null;
		public int position = 0;
		
		public ImageView icon = null;
		public ProgressBar loading = null;
		
		@Override
		protected SongInfo doInBackground(SongInfo... song) {
			
			if(isCancelled())
				return null;

			if(song[0].previewUrl == null){

				try{
					song[0].previewUrl = SevenDigitalComm.getComm().getPreviewUrl(song[0].id);
				} catch(Exception e){
					err = getString(R.string.err_mediaplayer);
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
					MediaPlayerController.getCon().startStopMedia(song.previewUrl, icon, loading);
				else
					MediaPlayerController.getCon().startStopMedia(song.previewUrl, position, adapter);
			}
		}
	}	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if(task != null)
			task.cancel(true);		
		
		if(mp_task != null)
			mp_task.cancel(true);
	}
	
	private void setListHeader(){
		//set content for main screen
		setContentView(R.layout.listview);		
		
		//set transparent background to show album image
		getListView().setBackgroundColor(0);

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
		
		TextView tvAlbumName = (TextView) header.findViewById(R.id.songinfo_albumName);
		tvAlbumName.setText(song.release.name);

		//set media buttons onclicks
		ImageView playpause = (ImageView) header.findViewById(R.id.songinfo_playpause);
		playpause.setImageResource(R.drawable.ic_image_play);
		
		playpause.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mp_task.cancel(true);
				mp_task = new StartMediaPlayerTask();
				
				FrameLayout header = (FrameLayout) v.getParent();					
				mp_task.icon = (ImageView) v;
				mp_task.loading = (ProgressBar) header.findViewById(R.id.songinfo_loading);
				mp_task.execute(song);
			}
		}); 

		ProgressBar loading = (ProgressBar) header.findViewById(R.id.songinfo_loading);
		loading.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mp_task.cancel(true);
				mp_task = new StartMediaPlayerTask();
				
				FrameLayout header = (FrameLayout) v.getParent();					
				mp_task.icon = (ImageView) header.findViewById(R.id.songinfo_playpause);
				mp_task.loading = (ProgressBar) v;
				mp_task.execute(song);
			}
		});
		
		//set buy button
		ImageButton buy = (ImageButton)header.findViewById(R.id.songinfo_buy);
		buy.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(song.buyUrl));
				intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		});
		
		//set add button
		ImageButton add = (ImageButton)header.findViewById(R.id.songinfo_add);
		add.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ArrayList<SongInfo> songs = new ArrayList<SongInfo>();
				songs.add(song);
				RecSongsPlaylist.getInstance().addSongsToPlaylist(songs, SongInfoActivity.this);
			}
		});
		
		//set share button
		ImageButton share = (ImageButton)header.findViewById(R.id.songinfo_share);
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
		
		//set watch button
		ImageButton watch = (ImageButton)header.findViewById(R.id.songinfo_watch);
		watch.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(SongInfoActivity.this, WatchSongVideoActivity.class);					
				intent.putExtra("songParcel", song);					
				startActivity(intent);
			}
		});

		//set image
		ImageView coverart = (ImageView) header.findViewById(R.id.songinfo_coverArt);
		ImageLoader.getLoader(getCacheDir()).DisplayImage(song.release.image, coverart, R.drawable.ic_disc_stub_large, ImageSize.MEDIUM);
		
		ImageView bkg = (ImageView) findViewById(R.id.listview_bkg);
		ImageLoader.getLoader(getCacheDir()).DisplayImage(song.release.image, getListView(), bkg, ImageSize.LARGE);
		
		getListView().addHeaderView(header);

		//set adapter for top tracks
		SongInfoActivity.this.setListAdapter(adapter); 
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return adapter.topTracks;
	}
}
