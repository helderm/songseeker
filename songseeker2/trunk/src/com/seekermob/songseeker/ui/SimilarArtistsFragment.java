package com.seekermob.songseeker.ui;

import java.util.ArrayList;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.echonest.api.v4.Artist;
import com.echonest.api.v4.EchoNestException;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.EchoNestComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.Util;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;

public class SimilarArtistsFragment extends SherlockListFragment {

	private SimilarArtistsAdapter mAdapter;
	private SimilarArtistsTask mTask;
	
	private Bundle mSavedState;
	
	private static final String STATE_ADAPTER_DATA = "adapterData";
	private static final String STATE_TASK_RUNNING = "taskRunning";	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		super.onActivityCreated(savedInstanceState);
		
		//set adapter				
		mAdapter = new SimilarArtistsAdapter();		
		setListAdapter(mAdapter);

		//set empty view
		((TextView)(getListView().getEmptyView())).setText(R.string.similar_artists_frag_empty);
		
		//restore state
		restoreLocalState(savedInstanceState);
		
		//if the adapter wasnt restored, fetch the adapter
		//but only if the task wasnt restored on restoreLocalState
		if(mAdapter.mSimilarArtists == null && !isTaskRunning()){
			mTask = (SimilarArtistsTask) new SimilarArtistsTask().execute();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		
		//save the adapter data
		if(mAdapter != null && mAdapter.mSimilarArtists != null){
			outState.putParcelableArrayList(STATE_ADAPTER_DATA, new ArrayList<Parcelable>(mAdapter.mSimilarArtists));			
		}
		
		//save the task state
		final SimilarArtistsTask task = mTask;
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
		ArrayList<ArtistInfo> adapterData;
		if((adapterData = savedInstanceState.getParcelableArrayList(STATE_ADAPTER_DATA)) != null){
			mAdapter.setAdapter(adapterData);			
		}
		
		//restore the top tracks task
		if(savedInstanceState.getBoolean(STATE_TASK_RUNNING)){
			mTask = (SimilarArtistsTask) new SimilarArtistsTask().execute();
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
	
	private class SimilarArtistsAdapter extends BaseAdapter {

		private ArrayList<ArtistInfo> mSimilarArtists;	 
		private LayoutInflater mInflater;		
		
		public SimilarArtistsAdapter() {    
			mSimilarArtists = null;
			mInflater = getActivity().getLayoutInflater();
		}

		public int getCount() {
			if(mSimilarArtists == null)
				return 0;

			return mSimilarArtists.size();
		}

		public ArtistInfo getItem(int position) {
			return mSimilarArtists.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if(convertView == null) {
				convertView = mInflater.inflate(R.layout.list_item_1_image, null);

				holder = new ViewHolder();
				holder.line = (TextView) convertView.findViewById(R.id.line);
				holder.image = (ImageView) convertView.findViewById(R.id.image);

				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}

			final ArtistInfo artist = getItem(position);
			if(artist != null) {
				holder.line.setText(artist.name);			
				ImageLoader.getLoader().DisplayImage(artist.image, holder.image, R.drawable.ic_disc_stub, ImageSize.SMALL);
			}

			return convertView;
		}

		public void setAdapter(ArrayList<ArtistInfo> sa){
			mSimilarArtists = sa;
			notifyDataSetChanged();
		}

		private class ViewHolder{
			public TextView line;
			public ImageView image;
		}
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		ArtistInfo artist = mAdapter.getItem(position);
		
		Intent i = new Intent(getActivity(), MusicInfoActivity.class);
		i.putExtra(ArtistInfoFragment.BUNDLE_ARTIST, artist);
		startActivity(i);
	}
	
	private class SimilarArtistsTask extends AsyncTask<Void, Void, ArrayList<ArtistInfo>>{
		String err = null;

		@Override
		protected void onPreExecute() {
			Util.setListShown(SimilarArtistsFragment.this, false);
		}

		@Override
		protected ArrayList<ArtistInfo> doInBackground(Void... arg0) {
			ArrayList<Artist> similar;
			ArrayList<ArtistInfo> artists = new ArrayList<ArtistInfo>();
			ArtistInfo artist;

			if(isCancelled())
				return null;
			
			try {
				ArtistInfo artistInfo = getArguments().getParcelable(ArtistInfoFragment.BUNDLE_ARTIST);
				similar = EchoNestComm.getComm().getSimilarArtistsFromBucket(artistInfo.id, 10);

				for(Artist a : similar){
					if(isCancelled())
						return null;
					
					String foreignId = a.getForeignID(EchoNestComm.SEVEN_DIGITAL);

					try{
						artist = SevenDigitalComm.getComm().queryArtistDetails(foreignId.split(":")[2], getActivity());
					}catch (ServiceCommException e) {
						Log.i(Util.APP, "Artist ["+a.getForeignID(EchoNestComm.SEVEN_DIGITAL)+"] not found in 7digital, skipping...");
						continue;
					}

					artists.add(artist);
				}

			} catch (ServiceCommException e) {
				err = e.getMessage();
				return null;
			} catch (EchoNestException e) {
				err = e.getMessage();
				return null;
			}			

			return artists;
		}

		@Override
		protected void onPostExecute(ArrayList<ArtistInfo> result) {
			Util.setListShown(SimilarArtistsFragment.this, true);

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

