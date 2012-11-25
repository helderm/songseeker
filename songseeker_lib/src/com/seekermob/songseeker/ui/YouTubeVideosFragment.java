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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.YouTubeComm;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.data.VideoInfo;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.Util;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;

public class YouTubeVideosFragment extends SherlockListFragment{

	private VideosAdapter mAdapter;
	private VideosTask mTask;

	private Bundle mSavedState;
	
	private static final String STATE_ADAPTER_DATA = "adapterData";
	private static final String STATE_TASK_RUNNING = "taskRunning";
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		super.onActivityCreated(savedInstanceState);
		
		//set adapter				
		mAdapter = new VideosAdapter();		
		setListAdapter(mAdapter);

		//set empty view
		((TextView)(getListView().getEmptyView())).setText(R.string.youtube_videos_frag_empty);
		
		//restore state
		restoreLocalState(savedInstanceState);
		
		//if the adapter wasnt restored, fetch the adapter
		//but only if the task wasnt restored on restoreLocalState
		if(mAdapter.mVideos == null && !isTaskRunning()){
			mTask = (VideosTask) new VideosTask().execute();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		
		//save the adapter data
		if(mAdapter != null && mAdapter.mVideos != null){
			outState.putParcelableArrayList(STATE_ADAPTER_DATA, new ArrayList<Parcelable>(mAdapter.mVideos));			
		}
		
		//save the task state
		final VideosTask task = mTask;
        if(task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
        	task.cancel(true);
        	
        	outState.putBoolean(STATE_TASK_RUNNING, true);
        	mTask = null;
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
		ArrayList<VideoInfo> adapterData;
		if((adapterData = savedInstanceState.getParcelableArrayList(STATE_ADAPTER_DATA)) != null){
			mAdapter.setAdapter(adapterData);			
		}
		
		//restore the top tracks task
		if(savedInstanceState.getBoolean(STATE_TASK_RUNNING)){
			mTask = (VideosTask) new VideosTask().execute();
		}
		
		mSavedState = null;
	}	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(isTaskRunning())
			mTask.cancel(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
        if(mSavedState != null) {
            restoreLocalState(mSavedState);
        }
	}	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.listview_progress, container, false);
	}

	private class VideosAdapter extends BaseAdapter {

		private ArrayList<VideoInfo> mVideos;	 
		private LayoutInflater mInflater;

		public VideosAdapter() {    
			mVideos = null;
			mInflater = getActivity().getLayoutInflater();
		}

		public int getCount() {
			if(mVideos == null)
				return 0;

			return mVideos.size();
		}

		public VideoInfo getItem(int position) {
			return mVideos.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if(convertView == null) {
				convertView = mInflater.inflate(R.layout.list_item_2_image_media, null);

				holder = new ViewHolder();
				holder.topText = (TextView) convertView.findViewById(R.id.firstLine);
				holder.botText = (TextView) convertView.findViewById(R.id.secondLine);
				holder.image = (ImageView) convertView.findViewById(R.id.image);		

				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}

			final VideoInfo video = getItem(position);
			if(video != null) {

				holder.topText.setText(video.title);
				holder.botText.setText(video.description);

				ImageLoader.getLoader().DisplayImage(video.image, holder.image, R.drawable.ic_disc_stub, ImageSize.SMALL);
			}

			return convertView;
		}

		public void setAdapter(ArrayList<VideoInfo> vf){
			this.mVideos = vf;
			notifyDataSetChanged();
		}

		private class ViewHolder{
			public TextView topText;
			public TextView botText;
			public ImageView image;			
		}
	}	
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		String videoId = mAdapter.mVideos.get(position).id;		
		
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v="+videoId));
		startActivity(i);
	}

	private class VideosTask extends AsyncTask<Void, Void, ArrayList<VideoInfo>>{
		String err = null;

		@Override
		protected void onPreExecute() {
			Util.setListShown(YouTubeVideosFragment.this, false);
		}

		@Override
		protected ArrayList<VideoInfo> doInBackground(Void... arg0) {
			ArrayList<VideoInfo> videos;

			if(isCancelled())
				return null;
			
			try {
				ArtistInfo artistInfo = getArguments().getParcelable(ArtistInfoFragment.BUNDLE_ARTIST);
				SongInfo songInfo = getArguments().getParcelable(SongInfoFragment.BUNDLE_SONG);

				if(songInfo != null){
					videos = YouTubeComm.getComm().searchVideo(songInfo.name, songInfo.artist.name, 20);
				}else{
					videos = YouTubeComm.getComm().searchVideo("", artistInfo.name, 20);
				}

				return videos;

			} catch (ServiceCommException e) {
				err = e.getMessage(getActivity());
				return null;
			}
		}

		@Override
		protected void onPostExecute(ArrayList<VideoInfo> result) {
			Util.setListShown(YouTubeVideosFragment.this, true);

			if(err != null){
				Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();				
				return;
			}

			mAdapter.setAdapter(result);
		}		
	}
	
	private boolean isTaskRunning(){				
		if(mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED){
			return true;
		}
		
		return false;
	}
}
