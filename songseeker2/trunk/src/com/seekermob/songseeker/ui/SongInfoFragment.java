package com.seekermob.songseeker.ui;

import java.util.ArrayList;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.data.RecSongsPlaylist;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.MediaPlayerController;
import com.seekermob.songseeker.util.Util;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;
import com.seekermob.songseeker.util.MediaPlayerController.MediaStatus;

public class SongInfoFragment extends SherlockListFragment{
	
	private TopTracksAdapter mAdapter;
	private SongInfo mSong;
	private TopTracksTask mTopTracksTask;
	
	private Bundle mSavedState;
	
	private static final String STATE_ADAPTER_DATA = "adapterData";
	private static final String STATE_TOP_TRACKS_RUNNING = "topTracksRunning";
	public static final String BUNDLE_TOP_SONGS = "artistTopSongs";
	public static final String BUNDLE_SONG = "song";
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		//populate the optionsMenu 
		setHasOptionsMenu(true);
		
		//fetch the selected song
		mSong = getArguments().getParcelable(BUNDLE_SONG);		
		ArrayList<SongInfo> topTracks = getArguments().getParcelableArrayList(BUNDLE_TOP_SONGS);
		
		//restore adapter 
		mAdapter = new TopTracksAdapter(topTracks);
		setListHeader();
		setListAdapter(mAdapter);
		
		//restore state
		restoreLocalState(savedInstanceState);

		//fetch background image
		ImageView bkg = (ImageView) getView().findViewById(R.id.background);
		ImageLoader.getLoader().DisplayImage(mSong.release.image, getListView(), bkg, ImageSize.LARGE);		
				
		//if the adapter wasnt restored, fetch the top tracks
		if(mAdapter.mTopTracks == null && !isTaskRunning()){
			mTopTracksTask = (TopTracksTask) new TopTracksTask().execute();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.listview_progress, null);		
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		
		//save the adapter data
		if(mAdapter != null && mAdapter.mTopTracks != null){
			outState.putParcelableArrayList(STATE_ADAPTER_DATA, new ArrayList<Parcelable>(mAdapter.mTopTracks));			
		}
		
		//save the top tracks task
		final TopTracksTask topTask = mTopTracksTask;
        if(topTask != null && topTask.getStatus() != AsyncTask.Status.FINISHED) {
        	topTask.cancel(true);
        	
        	outState.putBoolean(STATE_TOP_TRACKS_RUNNING, true);
        	mTopTracksTask = null;
        }
		
		mSavedState = outState;
		
		super.onSaveInstanceState(outState);
	}
	
	/** Restores the saved instance of this fragment*/
	private void restoreLocalState(Bundle savedInstanceState){	
		if(savedInstanceState == null){
			return;
		}
		
		//restore the adapter
		//but the one restored from the bundle has priority over this
		ArrayList<SongInfo> adapterData;
		if(mAdapter.mTopTracks == null && (adapterData = savedInstanceState.getParcelableArrayList(STATE_ADAPTER_DATA)) != null){			
			mAdapter.setTopTracks(adapterData);			
		}
		
		//restore the top tracks task
		if(savedInstanceState.getBoolean(STATE_TOP_TRACKS_RUNNING)){
			mTopTracksTask = (TopTracksTask) new TopTracksTask().execute();
		}
		
		mSavedState = null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(mTopTracksTask != null && mTopTracksTask.getStatus() != AsyncTask.Status.FINISHED)
			mTopTracksTask.cancel(true);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		MediaPlayerController.getCon().release();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
        if (mSavedState != null) {
            restoreLocalState(mSavedState);
        }
	}	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.songinfo_menu, menu);		
		super.onCreateOptionsMenu(menu, inflater);
	}	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final Intent intent;
		
		switch(item.getItemId()) {
		case R.id.menu_buy:
			intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mSong.buyUrl));
			intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return true;

		case R.id.menu_share:
			intent = new Intent(Intent.ACTION_SEND);					 
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_song_text) + " [" + mSong.name +" - "+ mSong.artist.name +
					"] ("+ mSong.buyUrl +")");
			startActivity(Intent.createChooser(intent, getString(R.string.share_using)));
			return true;
		case R.id.menu_add_to_playlist:
			ArrayList<SongInfo> songs = new ArrayList<SongInfo>();
			songs.add(mSong);
			RecSongsPlaylist.getInstance().addSongsToPlaylist(songs, getActivity().getApplicationContext());
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}
	
	private void setListHeader(){
		//set transparent background to show album image
		//getListView().setBackgroundColor(0);

		//set song info header
		LayoutInflater inflater = getActivity().getLayoutInflater();
		LinearLayout header = (LinearLayout)inflater.inflate(R.layout.song_info, null);

		TextView tvSongName = (TextView) header.findViewById(R.id.songinfo_songName);
		tvSongName.setText(mSong.name);

		TextView tvSongVersion = (TextView) header.findViewById(R.id.songinfo_songVersion);
		if(mSong.version != null)
			tvSongVersion.setText(mSong.version);
		else
			tvSongVersion.setVisibility(View.GONE);

		TextView tvSongArtist = (TextView) header.findViewById(R.id.songinfo_artistName);
		tvSongArtist.setText(mSong.artist.name);
		
		TextView tvAlbumName = (TextView) header.findViewById(R.id.songinfo_albumName);
		tvAlbumName.setText(mSong.release.name);

		//set media buttons onclicks
		ImageView playpause = (ImageView) header.findViewById(R.id.songinfo_playpause);
		playpause.setImageResource(R.drawable.ic_image_play);
		
		playpause.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {				
				FrameLayout header = (FrameLayout) v.getParent();					
				MediaPlayerController.getCon().startStopMedia(mSong.previewUrl, mSong.id, 
						(ImageView)v, (ProgressBar) header.findViewById(R.id.songinfo_loading));				
			}
		});
		
		ProgressBar loading = (ProgressBar) header.findViewById(R.id.songinfo_loading);
		loading.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				FrameLayout header = (FrameLayout) v.getParent();					
				MediaPlayerController.getCon().startStopMedia(mSong.previewUrl, mSong.id, 
						(ImageView) header.findViewById(R.id.songinfo_playpause), (ProgressBar)v );	
			}
		});
		
		//set image
		ImageView coverart = (ImageView) header.findViewById(R.id.songinfo_image);
		ImageLoader.getLoader().DisplayImage(mSong.release.image, coverart, R.drawable.ic_disc_stub, ImageSize.MEDIUM);
		
		getListView().addHeaderView(header, null, false);		
	}
	
	private class TopTracksAdapter extends BaseAdapter {

		private ArrayList<SongInfo> mTopTracks;   
		private LayoutInflater inflater;

		public TopTracksAdapter(ArrayList<SongInfo> s){
			mTopTracks = s;
			inflater = getActivity().getLayoutInflater();
		}
		
		public int getCount() {
			if(mTopTracks == null)
				return 0;

			return mTopTracks.size();
		}

		public SongInfo getItem(int position) {
			return mTopTracks.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			
			if (convertView == null) {
			   	convertView = inflater.inflate(R.layout.list_item_2_image_media, null);
				
				holder = new ViewHolder();
				holder.topText = (TextView) convertView.findViewById(R.id.firstLine);
			    holder.botText = (TextView) convertView.findViewById(R.id.secondLine);
			    holder.image = (ImageView) convertView.findViewById(R.id.image);
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
						MediaPlayerController.getCon().startStopMedia(song.previewUrl, song.id, pos, mAdapter);
						mAdapter.notifyDataSetChanged();
					}
				}); 
				break;
			case PLAYING:
			case STOPPED:
			default:
				holder.playPause.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						MediaPlayerController.getCon().startStopMedia(song.previewUrl, song.id, pos, mAdapter);
						mAdapter.notifyDataSetChanged();
					}
				}); 
				break;
			}			    

			ImageLoader.getLoader().DisplayImage(song.release.image, holder.image, R.drawable.ic_disc_stub, ImageSize.SMALL);			

			return convertView;
		}

		public void setTopTracks(ArrayList<SongInfo> tp){
			this.mTopTracks = tp;
			notifyDataSetChanged();
		}
		
		@Override
		public boolean isEmpty() {		
			//overriding this so it always shows th header view, even when the adapter is empty
			return false;
		}
		
	    private class ViewHolder{
	    	public TextView topText;
	    	public TextView botText;
	    	public ImageView image;
	    	public ImageView playPause;
	    	public ProgressBar loading;
	    	public FrameLayout mediaBtns;
	    }
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		SongInfo si = mAdapter.getItem(position-1);
		Intent i = new Intent(getActivity(), MusicInfoActivity.class);
		i.putExtra(BUNDLE_SONG, si);
		i.putParcelableArrayListExtra(BUNDLE_TOP_SONGS, mAdapter.mTopTracks);
		startActivity(i);
	}
	
	private class TopTracksTask extends AsyncTask<Void, Void, ArrayList<SongInfo>>{
		String err = null;

		@Override
		protected void onPreExecute() {
			Util.setListShown(SongInfoFragment.this, false);
		}

		@Override
		protected ArrayList<SongInfo> doInBackground(Void... arg0) {
			ArrayList<SongInfo> topTracks;						
			
			try{
				if(isCancelled())
					return null;
				
				topTracks = SevenDigitalComm.getComm().queryArtistTopTracks(mSong.artist.id);
				
			}catch(ServiceCommException e){
				err = e.getMessage();		
				return null;
			}
			
			return topTracks;
		}

		@Override
		protected void onPostExecute(ArrayList<SongInfo> topTracks) {

			Util.setListShown(SongInfoFragment.this, true);

			if(err != null){
				Toast.makeText(getActivity().getApplicationContext(), err, Toast.LENGTH_SHORT).show();				
				return;
			}

			mAdapter.setTopTracks(topTracks);
		}		
	}	
	
	private boolean isTaskRunning(){				
		if(mTopTracksTask != null && mTopTracksTask.getStatus() != AsyncTask.Status.FINISHED){
			return true;
		}
		
		return false;
	}
}
