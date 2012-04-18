package com.seekermob.songseeker.ui;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.ReleaseInfo;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;
import com.seekermob.songseeker.util.Util;

public class ArtistInfoFragment extends SherlockListFragment{
	
	private ArtistReleasesAdapter mAdapter;
	private ArtistInfo mArtist;
	private ArtistDetailsTask mArtistDetailsTask;
	
	private Bundle mSavedState;
	
	private static final String STATE_ADAPTER_DATA = "adapterData";
	private static final String STATE_ARTIST_DETAILS_RUNNING = "artistDetailsRunning";
	private static final String STATE_ARTIST_IMAGE = "artistImage";
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		//populate the optionsMenu 
		setHasOptionsMenu(true);
			
		//fetch the selected song
		mArtist = getArguments().getParcelable("artist");
		
		mAdapter = new ArtistReleasesAdapter();
		restoreLocalState(savedInstanceState);

		//set adapter				
		//setListHeader();
		setListAdapter(mAdapter);
		
		//if the adapter wasnt restored, fetch the adapter
		if(mAdapter.mReleases == null || mArtist.image == null){
			mArtistDetailsTask = (ArtistDetailsTask) new ArtistDetailsTask(mAdapter.mReleases).execute();
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
		if(mAdapter != null && mAdapter.mReleases != null){
			outState.putParcelableArrayList(STATE_ADAPTER_DATA, new ArrayList<Parcelable>(mAdapter.mReleases));			
		}
		
		//save the task state
		final ArtistDetailsTask artistTask = mArtistDetailsTask;
        if(artistTask != null && artistTask.getStatus() != AsyncTask.Status.FINISHED) {
        	artistTask.cancel(true);
        	
        	outState.putBoolean(STATE_ARTIST_DETAILS_RUNNING, true);
        	mArtistDetailsTask = null;
        }
        
        //save the image
        if(mArtist.image != null){
        	outState.putString(STATE_ARTIST_IMAGE, mArtist.image);
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
		ArrayList<ReleaseInfo> adapterData = null;
		if((adapterData = savedInstanceState.getParcelableArrayList(STATE_ADAPTER_DATA)) != null){
			mAdapter.setArtistReleases(adapterData);			
		}
		
		//restore the artist image
		String image;
		if((image = savedInstanceState.getString(STATE_ARTIST_IMAGE)) != null){
			mArtist.image = image;
		}
		
		//restore the top tracks task
		if(savedInstanceState.getBoolean(STATE_ARTIST_DETAILS_RUNNING)){
			mArtistDetailsTask = (ArtistDetailsTask) new ArtistDetailsTask(mAdapter.mReleases).execute();
		}
		
		mSavedState = null;
	}		
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(mArtistDetailsTask != null && mArtistDetailsTask.getStatus() != AsyncTask.Status.FINISHED)
			mArtistDetailsTask.cancel(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
        if(mSavedState != null) {
            restoreLocalState(mSavedState);
        }
	}
	
	private class ArtistReleasesAdapter extends BaseAdapter {

		private ArrayList<ReleaseInfo> mReleases;    
		private LayoutInflater inflater;
		
		public ArtistReleasesAdapter() {    
			mReleases = null;
			inflater = getActivity().getLayoutInflater();
		}

		public int getCount() {
			if(mReleases == null)
				return 0;

			return mReleases.size();
		}

		public ReleaseInfo getItem(int position) {
			return mReleases.get(position);
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
			    			    			    
			    convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}

			ReleaseInfo release = getItem(position);
			if (release == null) {
				return convertView;
			}
			
			holder.botText.setText(release.artist.name);
			holder.topText.setText(release.name);
										
			ImageLoader.getLoader().DisplayImage(release.image, holder.image, R.drawable.ic_disc_stub, ImageSize.SMALL);			

			return convertView;
		}

		public void setArtistReleases(ArrayList<ReleaseInfo> tp){
			this.mReleases = tp;
			notifyDataSetChanged();
		}
		
	    public class ViewHolder{
	    	public TextView topText;
	    	public TextView botText;
	    	public ImageView image;
	    }
	}	
	
	private class ArtistDetailsTask extends AsyncTask<Void, Void, ArrayList<ReleaseInfo>>{
		ArrayList<ReleaseInfo> mReleases;
		String err = null;		
		
		public ArtistDetailsTask(ArrayList<ReleaseInfo> releases){
			mReleases = releases;
		}		
		
		@Override
		protected void onPreExecute() {
			Util.setListShown(ArtistInfoFragment.this, false);
		}

		@Override
		protected ArrayList<ReleaseInfo> doInBackground(Void... args) {
			ArrayList<ReleaseInfo> releases;

			try{
				if(isCancelled())
					return null;
				
				//will need to fetch artist details, since we dont have the artist image url 
				if(mArtist.image == null){				
					mArtist = SevenDigitalComm.getComm().queryArtistDetails(mArtist.id, getActivity());
				}	
				
				if(isCancelled())
					return null;
				
				if(mReleases == null){
					releases = SevenDigitalComm.getComm().getArtistReleases(mArtist.id);
				} else{
					releases = mReleases;
				}		
				
			}catch(ServiceCommException e){
				err = e.getMessage();		
				return null;
			}			
			
			return releases;
		}

		@Override
		protected void onPostExecute(ArrayList<ReleaseInfo> releases) {
			Util.setListShown(ArtistInfoFragment.this, true);

			if(err != null){
				Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();
				return;
			}
			
			mAdapter.setArtistReleases(releases);
		}		
	}		
}
