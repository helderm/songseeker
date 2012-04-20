package com.seekermob.songseeker.ui;

import java.util.ArrayList;

import android.content.Intent;
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
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.data.ReleaseInfo;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.MediaPlayerController;
import com.seekermob.songseeker.util.Util;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;
import com.seekermob.songseeker.util.MediaPlayerController.MediaStatus;


public class ReleaseInfoFragment extends SherlockListFragment{
	
	private ReleaseSongsAdapter mAdapter;
	private ReleaseInfo mRelease;
	private ReleaseDetailsTask mReleaseDetailsTask;
	
	private Bundle mSavedState;
	
	private static final String STATE_ADAPTER_DATA = "adapterData";
	private static final String STATE_RELEASE_SONGS_RUNNING = "releaseSongsRunning";
	public static final String BUNDLE_RELEASE = "release";
	public static final String BUNDLE_RELEASE_SONGS = "releaseSongs";
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		//populate the optionsMenu 
		setHasOptionsMenu(true);
			
		//fetch the selected song
		mRelease = getArguments().getParcelable(BUNDLE_RELEASE);
		ArrayList<SongInfo> releaseSongs = getArguments().getParcelableArrayList(BUNDLE_RELEASE_SONGS);
		
		mAdapter = new ReleaseSongsAdapter(releaseSongs);
		restoreLocalState(savedInstanceState);

		//set adapter				
		setListHeader();
		setListAdapter(mAdapter);
		
		//if the adapter wasnt restored, fetch the adapter
		if(mAdapter.mReleaseSongs == null){
			mReleaseDetailsTask = (ReleaseDetailsTask) new ReleaseDetailsTask().execute();
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
		if(mAdapter != null && mAdapter.mReleaseSongs != null){
			outState.putParcelableArrayList(STATE_ADAPTER_DATA, new ArrayList<Parcelable>(mAdapter.mReleaseSongs));			
		}
		
		//save the task state
		final ReleaseDetailsTask releaseTask = mReleaseDetailsTask;
        if(releaseTask != null && releaseTask.getStatus() != AsyncTask.Status.FINISHED) {
        	releaseTask.cancel(true);
        	
        	outState.putBoolean(STATE_RELEASE_SONGS_RUNNING, true);
        	mReleaseDetailsTask = null;
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
		ArrayList<SongInfo> adapterData = null;
		if(mAdapter.mReleaseSongs == null && (adapterData = savedInstanceState.getParcelableArrayList(STATE_ADAPTER_DATA)) != null){
			mAdapter.setSongList(adapterData);			
		}
		
		//restore the top tracks task
		if(savedInstanceState.getBoolean(STATE_RELEASE_SONGS_RUNNING)){
			mReleaseDetailsTask = (ReleaseDetailsTask) new ReleaseDetailsTask().execute();
		}
		
		mSavedState = null;
	}	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(mReleaseDetailsTask != null && mReleaseDetailsTask.getStatus() != AsyncTask.Status.FINISHED)
			mReleaseDetailsTask.cancel(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
        if(mSavedState != null) {
            restoreLocalState(mSavedState);
        }
	}	
		
	@Override
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.releaseinfo_menu, menu);		
		super.onCreateOptionsMenu(menu, inflater);
	}	
	
	private void setListHeader(){
		
		//set transparent background to show album image
		//getListView().setBackgroundColor(0);
		
		//set album info header
		LayoutInflater inflater = getActivity().getLayoutInflater();
		LinearLayout header = (LinearLayout)inflater.inflate(R.layout.release_info, null);
		
		TextView releaseName = (TextView) header.findViewById(R.id.releaseinfo_releaseName);
		releaseName.setText(mRelease.name);

		TextView releaseArtist = (TextView) header.findViewById(R.id.releaseinfo_artistName);
		releaseArtist.setText(mRelease.artist.name);
		
		//set image
		ImageView coverart = (ImageView) header.findViewById(R.id.releaseinfo_image);
		ImageLoader.getLoader().DisplayImage(mRelease.image, coverart, R.drawable.ic_disc_stub, ImageSize.MEDIUM);
		
		//ImageView bkg = (ImageView) findViewById(R.id.listview_bkg);
		//ImageLoader.getLoader().DisplayImage(mRelease.image, getListView(), bkg, ImageSize.LARGE);
		
		getListView().addHeaderView(header);

	}
	
	private class ReleaseSongsAdapter extends BaseAdapter {

		private ArrayList<SongInfo> mReleaseSongs;
		private LayoutInflater inflater;    

		public ReleaseSongsAdapter(ArrayList<SongInfo> s) {    
			mReleaseSongs = s;
			inflater = getActivity().getLayoutInflater();
		}

		public int getCount() {
			if(mReleaseSongs == null)
				return 0;

			return mReleaseSongs.size();
		}

		public SongInfo getItem(int position) {
			return mReleaseSongs.get(position);
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

	    private class ViewHolder{
	    	public TextView topText;
	    	public TextView botText;
	    	public ImageView image;
	    	public ImageView playPause;
	    	public ProgressBar loading;
	    	public FrameLayout mediaBtns;
	    }
		
		public void setSongList(ArrayList<SongInfo> tp){
			this.mReleaseSongs = tp;
			notifyDataSetChanged();
		}
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		SongInfo si = mAdapter.getItem(position-1);
		Intent i = new Intent(getActivity(), MusicInfoActivity.class);
		i.putExtra(SongInfoFragment.BUNDLE_SONG, si);
		i.putParcelableArrayListExtra(BUNDLE_RELEASE_SONGS, mAdapter.mReleaseSongs);
		startActivity(i);
	}
	
	private class ReleaseDetailsTask extends AsyncTask<Void, Void, ArrayList<SongInfo>>{
		String err = null;		

		@Override
		protected void onPreExecute() {
			Util.setListShown(ReleaseInfoFragment.this, false);
		}

		@Override
		protected ArrayList<SongInfo> doInBackground(Void... arg0) {
			ArrayList<SongInfo> songList;

			try{
				songList = SevenDigitalComm.getComm().queryReleaseSongList(mRelease.id);
			}catch(ServiceCommException e){
				err = e.getMessage();		
				return null;
			}			

			return songList;
		}

		@Override
		protected void onPostExecute(ArrayList<SongInfo> songList) {
			Util.setListShown(ReleaseInfoFragment.this, true);

			if(err != null){
				Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();				
				return;
			}

			mAdapter.setSongList(songList);
		}		
	}	
}
