package com.seekermob.songseeker.activity;

import java.util.ArrayList;

import com.google.android.apps.analytics.easytracking.TrackedListActivity;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.data.RecSongsPlaylist;
import com.seekermob.songseeker.data.ReleaseInfo;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;
import com.seekermob.songseeker.util.MediaPlayerController;
import com.seekermob.songseeker.util.Util;
import com.seekermob.songseeker.util.MediaPlayerController.MediaStatus;

import android.app.Dialog;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ReleaseInfoActivity extends TrackedListActivity {

	private ReleaseInfo release;
	
	private static final int RELEASE_DETAILS_DIAG = 0;
	private SongListAdapter adapter;

	private StartMediaPlayerTask mp_task = new StartMediaPlayerTask();
	private GetReleaseDetails task = new GetReleaseDetails();
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		release = getIntent().getExtras().getParcelable("releaseParcel");
		adapter = new SongListAdapter();
		
		//check orientation change
		@SuppressWarnings("unchecked")
		ArrayList<SongInfo> savedSongList = (ArrayList<SongInfo>) getLastNonConfigurationInstance();
		
		if(savedSongList == null){		
			task = new GetReleaseDetails();
			task.execute();
		}else{
			setListHeader();
			adapter.setSongList(savedSongList);
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
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case RELEASE_DETAILS_DIAG:
			ProgressDialog pd = new ProgressDialog(this);
			pd.setMessage("Fetching album details...");
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
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		task.cancel(true);
	}
	
	private class GetReleaseDetails extends AsyncTask<Void, Void, Void>{
		String err = null;		

		@Override
		protected void onPreExecute() {
			showDialog(RELEASE_DETAILS_DIAG);
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			ArrayList<SongInfo> songList;

			try{
				if(release == null){
					//this will happen only on Eclair, see MusicInfoTab
					SongInfo song = getIntent().getExtras().getParcelable("songId");	
					song = SevenDigitalComm.getComm().querySongDetails(song.id, song.name, song.artist.name);	
					release = song.release;
				}				
				
				songList = SevenDigitalComm.getComm().queryReleaseSongList(release.id);
			}catch(ServiceCommException e){
				err = e.getMessage();		
				return null;
			}			
			
			adapter.setSongList(songList);

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

			setListHeader();
		}		
	}	
	
	private class SongListAdapter extends BaseAdapter {

		private ArrayList<SongInfo> songList;
		private LayoutInflater inflater;    

		public SongListAdapter() {    
			songList = null;
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public int getCount() {
			if(songList == null)
				return 0;

			return songList.size();
		}

		public SongInfo getItem(int position) {
			return songList.get(position);
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
			
			holder.topText.setText(song.name);
			holder.botText.setText(song.artist.name);
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

	    private class ViewHolder{
	    	public TextView topText;
	    	public TextView botText;
	    	public ImageView coverArt;
	    	public ImageView playPause;
	    	public ProgressBar loading;
	    	public FrameLayout mediaBtns;
	    }
		
		public void setSongList(ArrayList<SongInfo> tp){
			this.songList = tp;
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		SongInfo si = adapter.getItem(position-1);
		Intent i = new Intent(ReleaseInfoActivity.this, MusicInfoTab.class);
		i.putExtra("songParcel", si);
		startActivity(i);
	}
	
	private class StartMediaPlayerTask extends AsyncTask<SongInfo, Void, SongInfo>{
		private String err = null;
		public int position = -1;

		@Override
		protected SongInfo doInBackground(SongInfo... song) {


			if(isCancelled())
				return null;

			if(song[0].previewUrl == null){
				
				try{
					song[0].previewUrl = SevenDigitalComm.getComm().getPreviewUrl(song[0].id);
				} catch(Exception e){
					err = getString(R.string.err_mediaplayer);
					Log.i(Util.APP, "Unable to fetch the preview url from 7digital!", e);
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
			
			if(!isCancelled())
				MediaPlayerController.getCon().startStopMedia(song.previewUrl, position, adapter);
		}
	}	
	
	private void setListHeader(){
		//set content for main screen
		setContentView(R.layout.listview);	
		
		//set transparent background to show album image
		getListView().setBackgroundColor(0);
		
		//set album info header
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout header = (LinearLayout)inflater.inflate(R.layout.release_info, null);
		
		TextView releaseName = (TextView) header.findViewById(R.id.releaseinfo_releaseName);
		releaseName.setText(release.name);

		TextView releaseArtist = (TextView) header.findViewById(R.id.releaseinfo_artistName);
		releaseArtist.setText(release.artist.name);

		//set buy button
		ImageButton buy = (ImageButton)header.findViewById(R.id.releaseinfo_buy);
		buy.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(release.buyUrl));
				intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		});

		//set add button
		ImageButton add = (ImageButton)header.findViewById(R.id.releaseinfo_add);
		add.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				RecSongsPlaylist.getInstance().addSongsToPlaylist(adapter.songList, ReleaseInfoActivity.this);
			}
		});
		
		//set share button
		ImageButton share = (ImageButton)header.findViewById(R.id.releaseinfo_share);
		share.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final Intent intent = new Intent(Intent.ACTION_SEND);					 
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_SUBJECT, "New album!");
				intent.putExtra(Intent.EXTRA_TEXT, "I discovered the album '"+ release.name + " by "+ release.artist.name 
						+ "' using the Song Seeker app for Android! Check it out! "+ release.buyUrl);
				startActivity(Intent.createChooser(intent, "Share using..."));
			}				
		});
		
		//set image
		ImageView coverart = (ImageView) header.findViewById(R.id.releaseinfo_coverArt);
		ImageLoader.getLoader(getCacheDir()).DisplayImage(release.image, coverart, R.drawable.ic_disc_stub_large, ImageSize.MEDIUM);
		
		ImageView bkg = (ImageView) findViewById(R.id.listview_bkg);
		ImageLoader.getLoader(getCacheDir()).DisplayImage(release.image, getListView(), bkg, ImageSize.LARGE);
		
		getListView().addHeaderView(header);

		//set adapter for top tracks
		ReleaseInfoActivity.this.setListAdapter(adapter); 	
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return adapter.songList;
	}
}
