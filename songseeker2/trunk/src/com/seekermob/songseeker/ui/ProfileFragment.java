package com.seekermob.songseeker.ui;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.UserProfile;
import com.seekermob.songseeker.data.UserProfile.ArtistProfile;
import com.seekermob.songseeker.ui.InputDialogFragment.onTextEnteredListener;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.Util;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ProfileFragment extends SherlockListFragment implements onTextEnteredListener {

	private ArtistsAdapter mAdapter;
	private AddArtistsProfileTask mProfileTask;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    //populate the optionsMenu
	    setHasOptionsMenu(true);
	    
	    //set adapter
	    mAdapter = new ArtistsAdapter();
	    setListAdapter(mAdapter);
	    
	    //set listview selector
	    getListView().setSelector(R.drawable.list_selector_holo_dark);
	    
	    setEmptyText(getString(R.string.profile_frag_empty_list));	 
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.profile_menu, menu);		
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_artist:	
			if(isProfileTaskRunning()){
				Toast.makeText(getActivity(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
				return true;
			}
			
			InputDialogFragment newFragment = InputDialogFragment
					.newInstance(R.string.artist_search, R.string.artist_name, this);
			newFragment.showDialog(getActivity());			
			
			//TEST
			/*ArrayList<String> test = new ArrayList<String>();
			test.add(new String("Eric Clapton"));
			test.add(new String("The Derek Trucks Band"));
			test.add(new String("U2"));
			mProfileTask = (AddArtistsProfileTask) new AddArtistsProfileTask(test, 0).execute();*/
			//TEST
			
			return true;
		case R.id.menu_import_artists:
			if(isProfileTaskRunning()){
				Toast.makeText(getActivity(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
				return true;
			}
			return true;
		case R.id.menu_clear_profile:
			if(isProfileTaskRunning()){
				onCancelTasks();
			}
			
			mAdapter.prof.clearProfile(getActivity());
			mAdapter.notifyDataSetChanged();
			
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}		

	public class ArtistsAdapter extends BaseAdapter {

		private UserProfile prof;	 
		private LayoutInflater inflater;

		public ArtistsAdapter() {    
			prof = UserProfile.getInstance(getActivity());
			inflater = getActivity().getLayoutInflater();
		}

		public int getCount() {
			if(prof == null || prof.getProfile() == null)
				return 0;

			return prof.getProfile().artists.size();
		}

		public ArtistProfile getItem(int position) {
			return prof.getProfile().artists.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if(convertView == null) {
				convertView = inflater.inflate(R.layout.list_item_1_image, null);

				holder = new ViewHolder();
				holder.line = (TextView) convertView.findViewById(R.id.line);
				holder.image = (ImageView) convertView.findViewById(R.id.image);

				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}

			final ArtistProfile artist = getItem(position);
			if(artist != null) {

				holder.line.setText(artist.name);
				ImageLoader.getLoader().DisplayImage(artist.image, holder.image, R.drawable.ic_disc_stub, ImageSize.SMALL);	
			}

			return convertView;
		}

		public void addArtistsProfile(ArrayList<ArtistProfile> artists){
			prof.getProfile().artists.addAll(artists);
			notifyDataSetChanged();
		}
		
		private class ViewHolder{
			public TextView line;
			public ImageView image;
		}
	}	
	
	private class AddArtistsProfileTask extends AsyncTask<Void, Integer, Void>{
		private View mProgressOverlay;
		private ProgressBar mUpdateProgress;
		
		private UserProfile mUserProfile = mAdapter.prof;
		private ArrayList<String> mArtistsNames;
		private AtomicInteger mFetchCount = new AtomicInteger();
		
		private String err;
		private String msg;
       
		protected AddArtistsProfileTask() {
        }
		
        protected AddArtistsProfileTask(ArrayList<String> artists, int i) {
        	mArtistsNames = artists;
        	mFetchCount.set(i);
        }	        

		@Override
		protected void onProgressUpdate(Integer... progress) {			
			if(mArtistsNames.size() > 1)
				mUpdateProgress.setProgress(progress[0]);
		}        

		@Override
		protected void onPreExecute() {

            // see if we already inflated the progress overlay
            mProgressOverlay = getActivity().findViewById(R.id.overlay_update);
            if (mProgressOverlay == null) {
                mProgressOverlay = ((ViewStub) getActivity().findViewById(R.id.stub_update)).inflate();
            }                        
            showOverlay(mProgressOverlay);
            
            // setup the progress overlay
            TextView mUpdateStatus = (TextView) mProgressOverlay
                    .findViewById(R.id.textViewUpdateStatus);
            mUpdateStatus.setText(R.string.loading);
           
            mUpdateProgress = (ProgressBar) mProgressOverlay
                    .findViewById(R.id.ProgressBarShowListDet);

            //show an inderteminate progress bar if we have only one artist to add
            if(mArtistsNames.size() == 1){
            	mUpdateProgress.setIndeterminate(true);
            }else{
          		mUpdateProgress.setMax(mArtistsNames.size());
    			mUpdateProgress.setProgress(mFetchCount.get());
            }
            
            View cancelButton = mProgressOverlay.findViewById(R.id.overlayCancel);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onCancelTasks();
                }
            });
		}       
        
		@Override
		protected Void doInBackground(Void... params) {
			ArtistInfo artist;
			ArrayList<ArtistProfile> artistsProfile = new ArrayList<ArtistProfile>();
			int alreadyProfileCount = 0;
			
			//import the artists to the profile
			final AtomicInteger fetchCount = mFetchCount;
			for(String artistName : mArtistsNames){
				
				if(isCancelled()){
					return null;
				}
				
				if(mUserProfile.isAlreadyInProfile(artistName)){
					alreadyProfileCount++;
					continue;
				}
					
				try{
					artist = SevenDigitalComm.getComm().queryArtistSearch(artistName);
				}catch(ServiceCommException e) {
					if(e.getErr() == ServiceErr.IO || e.getErr() == ServiceErr.TRY_LATER){
						break;
					}
					
					Log.i(Util.APP, "Unable to add artist ["+artistName+"] to profile, skipping...");
					publishProgress(fetchCount.incrementAndGet());
					continue;
				}
				
				//the string passed by the user may be diff from what is stored at the profile
				if(mUserProfile.isAlreadyInProfile(artist.name)){
					alreadyProfileCount++;
					continue;
				}
				
				ArtistProfile artistProfile = new ArtistProfile();
				artistProfile.name = artist.name;
				artistProfile.image = artist.image;
				artistProfile.id = artist.id;
				artistProfile.buyUrl = artist.buyUrl;	
				
				//check if the artist was already added to the list
				boolean isAlreadyAdd = false;
				for(ArtistProfile aux : artistsProfile){
					if(aux.id.equalsIgnoreCase(artistProfile.id)){
						isAlreadyAdd = true;
						break;
					}
				}
				if(isAlreadyAdd){
					publishProgress(fetchCount.incrementAndGet());
					continue;
				}
			
				artistsProfile.add(artistProfile);
				
				publishProgress(fetchCount.incrementAndGet());
			}
						
			if(mArtistsNames.size() == alreadyProfileCount || (artistsProfile.size() == 0 && alreadyProfileCount > 0)){
				msg = getString(R.string.artists_already_profile);
				return null;
			}else if(artistsProfile.size() == 0){
				err = getString(R.string.err_add_artists_profile);
				return null;
			}else if(artistsProfile.size() < mArtistsNames.size()){
				msg = getString(R.string.some_artists_added_profile);
			}else
				msg = getString(R.string.success_add_artists_profile);				
			
			mUserProfile.syncAddArtistsToProfile(artistsProfile, getActivity());
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void artistsProfile) {
			hideOverlay(mProgressOverlay);
			
			if(err != null){
				Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();
				return;
			}			
			
			Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();	
			
			mAdapter.notifyDataSetChanged();
		}
		
        @Override
        protected void onCancelled() {
        	hideOverlay(mProgressOverlay);
        }
	}
	
    private void showOverlay(View overlay) {
        overlay.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(),
        		R.anim.fade_in));
        overlay.setVisibility(View.VISIBLE);
    }

    private void hideOverlay(View overlay) {
    	overlay.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(),
                R.anim.fade_out));
        overlay.setVisibility(View.GONE);
    }	
    
    private void onCancelTasks() {
        if (mProfileTask != null && mProfileTask.getStatus() == AsyncTask.Status.RUNNING) {
        	mProfileTask.cancel(true);
        	mProfileTask = null;
        }
    }    
    
    private boolean isProfileTaskRunning() {
        if(mProfileTask != null && mProfileTask.getStatus() == AsyncTask.Status.RUNNING) {
            return true;
        } else {
            return false;
        }
    }

	@Override
	public void onDialogTextEntered(String text) {
		ArrayList<String> artist = new ArrayList<String>();
		artist.add(text);
		mProfileTask = (AddArtistsProfileTask) new AddArtistsProfileTask(artist, 0).execute();
	}    
}
