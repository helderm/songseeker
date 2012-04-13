package com.seekermob.songseeker.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.LastfmComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.UserProfile;
import com.seekermob.songseeker.data.UserProfile.ArtistProfile;
import com.seekermob.songseeker.ui.InputDialogFragment.OnTextEnteredListener;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.Util;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;

import de.umass.lastfm.Artist;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ProfileFragment extends SherlockListFragment implements OnTextEnteredListener {

	private ArtistsAdapter mAdapter;
	private AddArtistsProfileTask mProfileTask;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	
	    //populate the optionsMenu
	    setHasOptionsMenu(true);
	    
	    //set adapter
	    mAdapter = new ArtistsAdapter();
	    setListAdapter(mAdapter);
	    
	    //set empty view text
	    ((TextView)(getListView().getEmptyView())).setText(R.string.profile_frag_empty_list);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.listview_progress, null);		
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
			
			InputDialogFragment dialog = InputDialogFragment
					.newInstance(R.string.artist_name, "artist-name", this);
			dialog.showDialog(getActivity());			

			return true;
		case R.id.menu_import_artists:
			if(isProfileTaskRunning()){
				Toast.makeText(getActivity(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
				return true;
			}
			
			ImportDialogFragment importDiag = new ImportDialogFragment();
	    	FragmentTransaction ft = getFragmentManager().beginTransaction();
	    	importDiag.show(ft, "import-dialog");
			
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
		private UserProfile mUserProfile = mAdapter.prof;
		private ArrayList<String> mArtistsNames;
		private String mUsername; //used by Last.fm import
		private AtomicInteger mFetchCount = new AtomicInteger();
		
		private int mTaskType; //0 -> user added an artist, 1 -> import from device, 2 -> import from last.fm
		
		private View mProgressOverlay;
		private ProgressBar mUpdateProgress;
		
		private String err;
		private String msg;
       
		//protected AddArtistsProfileTask() {
        //}

        protected AddArtistsProfileTask(String input, int taskType) {
        	mFetchCount.set(0);
        	
        	mTaskType = taskType;
        	switch(mTaskType){
        	case 0:
            	//when the user added an artist manually
            	mArtistsNames = new ArrayList<String>();
            	mArtistsNames.add(input);
        		break;
        	case 1:
        		//import from device
        		break;
        	case 2:
        		//import from last.fm	
        		mUsername = input;
        		break;
        	}
        }

		@Override
		protected void onProgressUpdate(Integer... progress) {			
			if(progress[0] == -1){
				mUpdateProgress.setIndeterminate(false);
				mUpdateProgress.setMax(mArtistsNames.size());
	    		mUpdateProgress.setProgress(mFetchCount.get());
	    		return;
			}
			
			if(mArtistsNames.size() > 1)
				mUpdateProgress.setProgress(progress[0]);
		}        

		@Override
		protected void onPreExecute() {

            mProgressOverlay = Util.setProgressShown(ProfileFragment.this, true);
            
            // setup the progress overlay
            TextView mUpdateStatus = (TextView) mProgressOverlay
                    .findViewById(R.id.textViewUpdateStatus);
            mUpdateStatus.setText(R.string.loading);
           
            mUpdateProgress = (ProgressBar) mProgressOverlay
                    .findViewById(R.id.ProgressBarShowListDet);

            //show an inderteminate progress bar while we dont know
            //how many artists we will have
            mUpdateProgress.setIndeterminate(true);
            
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
			
			//fetch the artists from the device or last.fm
			switch(mTaskType){
			case 1:	//device
				try {
					mArtistsNames = Util.getArtistsFromDevice(getActivity());
				} catch (Exception e) {
					Log.e(Util.APP, "Failed to import artists from device", e);
					err = getString(R.string.import_profile_failed);
					return null;
				}
				
				if(mArtistsNames.isEmpty()){
					err = getString(R.string.import_no_artist_found);
					return null;
				}	
				break;
			case 2: //last.fm
				Collection<Artist> topArtists;
				try {
					topArtists = LastfmComm.getComm().getTopArtists(mUsername);
				} catch (ServiceCommException e) {
					//err = e.getMessage();
					err = getString(R.string.import_profile_failed);
					return null;
				}
				
				mArtistsNames = new ArrayList<String>();
				for(Artist a : topArtists){
					mArtistsNames.add(a.getName());
				}
				
				break;				
			}
			
			//set a limit to the progress bar if there is more than one artist to import
			if(mArtistsNames.size() > 1){
				publishProgress(-1);
			}
			
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
				
			if(isCancelled())
				return null;
			
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
			Util.setProgressShown(ProfileFragment.this, false);
			
			if(err != null){
				Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();
				return;
			}			
			
			Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();	
			
			mAdapter.notifyDataSetChanged();
		}
		
        @Override
        protected void onCancelled() {
        	Util.setProgressShown(ProfileFragment.this, false);
        }
        
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
	public void onDialogTextEntered(String text, String tag) {
		//callback when the user inputs some text at the 'add artist' action
		if(tag.equalsIgnoreCase("artist-name")){
			mProfileTask = (AddArtistsProfileTask) new AddArtistsProfileTask(text, 0).execute();
		}
		
		if(tag.equalsIgnoreCase("lastfm-username")){
			mProfileTask = (AddArtistsProfileTask) new AddArtistsProfileTask(text, 2).execute();
		}
	}    
	
	public class ImportDialogFragment extends DialogFragment{
		
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	    	View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_import, null, false);
	    	
	    	v.findViewById(R.id.import_from_device).setOnClickListener(new View.OnClickListener() {
	    		@Override
	    		public void onClick(View v) {
	    			mProfileTask = (AddArtistsProfileTask) new AddArtistsProfileTask(null, 1).execute();
	    			dismiss();            	
	    		}
	    	});
	    	
	    	v.findViewById(R.id.import_from_lastfm).setOnClickListener(new View.OnClickListener() {
	    		@Override
	    		public void onClick(View v) {
	    			dismiss();	    			
	    			InputDialogFragment dialog = InputDialogFragment
	    					.newInstance(R.string.lastfm_username, "lastfm-username", ProfileFragment.this);
	    			dialog.showDialog(getActivity());	    			            	
	    		}
	    	});    	
	    	
	    	Dialog dialog = new Dialog(getActivity(), R.style.DialogThemeNoTitle);
	    	dialog.setContentView(v);
	    	return dialog;
	    }
	}	
}
