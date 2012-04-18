package com.seekermob.songseeker.ui;

import java.util.ArrayList;
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

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ProfileFragment extends SherlockListFragment implements OnTextEnteredListener {

	private ArtistsAdapter mAdapter;
	private ProfileTask mProfileTask;
	private Bundle mSavedState;
	
	private static final String STATE_PROFILE_ARTIST_NAMES = "profileArtistNames";
	private static final String STATE_PROFILE_INDEX = "profileIndex";
	private static final String STATE_PROFILE_RUNNING = "profileRunning";	
	
	private static final int MENU_REMOVE_ARTIST = 20;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	
	    //populate the optionsMenu
	    setHasOptionsMenu(true);
	    
	    //set adapter
	    mAdapter = new ArtistsAdapter();
	    setListAdapter(mAdapter);
		
	    //check if we are recovering the state	    
	    restoreLocalState(savedInstanceState);
	    
	    //set empty view text
	    ((TextView)(getListView().getEmptyView())).setText(R.string.profile_frag_empty_list);
	    
	    //context menu
	    registerForContextMenu(getListView());	    
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.listview_progress, null);		
	}	

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		//cancel the profile task
		if(mProfileTask != null && mProfileTask.getStatus() != AsyncTask.Status.FINISHED)
			mProfileTask.cancel(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
        if (mSavedState != null) {
            restoreLocalState(mSavedState);
        }
	}	
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
	
		//save the profile task if it is running
		final ProfileTask task = mProfileTask;
		if(task != null && task.getStatus() != AsyncTask.Status.FINISHED){
			task.cancel(true);
			
			outState.putBoolean(STATE_PROFILE_RUNNING, true);
            outState.putParcelableArrayList(STATE_PROFILE_ARTIST_NAMES, task.mArtists);
            outState.putInt(STATE_PROFILE_INDEX, task.mFetchCount.get());
            
            //save what we already fetched of the profile into the device
            task.mUserProfile.syncAddArtistsToProfile(task.mArtistsProfile, getActivity());
            
            mProfileTask = null;
		}
		
		mSavedState = outState;
		
		super.onSaveInstanceState(outState);
	}
	
	/** Restores the saved instance of this fragment*/
	private void restoreLocalState(Bundle savedInstanceState){		

		if(savedInstanceState == null){
			return;
		}
		
		//recover the profile task
		if(savedInstanceState.getBoolean(STATE_PROFILE_RUNNING)){
			ArrayList<ArtistInfo> artists = savedInstanceState.getParcelableArrayList(STATE_PROFILE_ARTIST_NAMES);
			int index = savedInstanceState.getInt(STATE_PROFILE_INDEX);
			
			if(artists != null)
				mProfileTask = (ProfileTask) new ProfileTask(index, artists).execute();
		}
		
		mSavedState = null;
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

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {	
		super.onCreateContextMenu(menu, v, menuInfo);
		
		menu.add(ContextMenu.NONE, MENU_REMOVE_ARTIST, ContextMenu.NONE, R.string.remove_artist);
	}
	
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		switch (item.getItemId()) {
		case MENU_REMOVE_ARTIST:
			UserProfile.getInstance(getActivity()).removeArtistFromProfile(info.position, this);
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
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
		
		private class ViewHolder{
			public TextView line;
			public ImageView image;
		}
	}	
	
	private class ProfileTask extends AsyncTask<Void, Integer, Void>{
		private UserProfile mUserProfile = mAdapter.prof;
		private ArrayList<ArtistInfo> mArtists;		
		private String mUsername; //used by Last.fm import
		
		private ArrayList<ArtistProfile> mArtistsProfile = new ArrayList<ArtistProfile>();
		private AtomicInteger mFetchCount = new AtomicInteger();
		
		private int mTaskType; //0 -> user added an artist, 1 -> import from device, 2 -> import from last.fm
		
		private View mProgressOverlay;
		private ProgressBar mUpdateProgress;
		
		private String err;
		private String msg;
       
		//protected AddArtistsProfileTask() {
        //}

        protected ProfileTask(String input, int taskType) {
        	mFetchCount.set(0);
        	
        	mTaskType = taskType;
        	switch(mTaskType){
        	case 0:
            	//when the user added an artist manually
            	mArtists = new ArrayList<ArtistInfo>();
            	mArtists.add(new ArtistInfo());
            	mArtists.get(0).name = input;
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

		public ProfileTask(int index, ArrayList<ArtistInfo> artists) {
			mArtists = artists;
			mFetchCount.set(index);
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {			
			if(progress[0] == -1){
				mUpdateProgress.setIndeterminate(false);
				mUpdateProgress.setMax(mArtists.size());
	    		mUpdateProgress.setProgress(mFetchCount.get());
	    		return;
			}
			
			if(mArtists.size() > 1)
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
			int alreadyProfileCount = 0;
			
			//fetch the artists from the device or last.fm
			if(mArtists == null || mArtists.size() == 0){
				switch(mTaskType){
				case 1:	//device
					try {
						mArtists = Util.getArtistsFromDevice(getActivity());
					} catch (Exception e) {
						Log.e(Util.APP, "Failed to import artists from device", e);
						err = getString(R.string.import_profile_failed);
						return null;
					}
					
					if(mArtists.isEmpty()){
						err = getString(R.string.import_no_artist_found);
						return null;
					}	
					break;
				case 2: //last.fm
					try {
						mArtists = LastfmComm.getComm().getTopArtists(mUsername);
					} catch (ServiceCommException e) {
						//err = e.getMessage();
						err = getString(R.string.import_profile_failed);
						return null;
					}
					
					break;				
				}
			}
			
			//set a limit to the progress bar if there is more than one artist to import
			if(mArtists.size() > 1){
				publishProgress(-1);
			}
			
			//import the artists to the profile
			final AtomicInteger fetchCount = mFetchCount;
			for(ArtistInfo a : mArtists){
				
				if(isCancelled()){
					return null;
				}
				
				if(mUserProfile.isAlreadyInProfile(a.name)){
					alreadyProfileCount++;
					continue;
				}
					
				try{
					artist = SevenDigitalComm.getComm().queryArtistSearch(a.name);
				}catch(ServiceCommException e) {
					if(e.getErr() == ServiceErr.IO || e.getErr() == ServiceErr.TRY_LATER){
						break;
					}
					
					Log.i(Util.APP, "Unable to add artist ["+a.name+"] to profile, skipping...");
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
				for(ArtistProfile aux : mArtistsProfile){
					if(aux.id.equalsIgnoreCase(artistProfile.id)){
						isAlreadyAdd = true;
						break;
					}
				}
				if(isAlreadyAdd){
					publishProgress(fetchCount.incrementAndGet());
					continue;
				}
			
				mArtistsProfile.add(artistProfile);
				
				publishProgress(fetchCount.incrementAndGet());
			}
				
			if(isCancelled())
				return null;
			
			if(mArtists.size() == alreadyProfileCount || (mArtistsProfile.size() == 0 && alreadyProfileCount > 0)){
				msg = getString(R.string.artists_already_profile);
				return null;
			}else if(mArtistsProfile.size() == 0){
				err = getString(R.string.err_add_artists_profile);
				return null;
			}else if(mArtistsProfile.size() < mArtists.size()){
				msg = getString(R.string.some_artists_added_profile);
			}else
				msg = getString(R.string.success_add_artists_profile);				
			
			mUserProfile.syncAddArtistsToProfile(mArtistsProfile, getActivity());
			
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
        if(!isProfileTaskRunning())
        	return;		
		
        mProfileTask.cancel(true);
        mProfileTask = null;        
    }    
    
    private boolean isProfileTaskRunning() {
        if(mProfileTask != null && mProfileTask.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        } else {
            return false;
        }
    }

	@Override
	public void onDialogTextEntered(String text, String tag) {
		//callback when the user inputs some text at the 'add artist' action
		if(tag.equalsIgnoreCase("artist-name")){
			mProfileTask = (ProfileTask) new ProfileTask(text, 0).execute();
		}
		
		if(tag.equalsIgnoreCase("lastfm-username")){
			mProfileTask = (ProfileTask) new ProfileTask(text, 2).execute();
		}
	}    
	
	public static class ImportDialogFragment extends DialogFragment{
		
		public static ImportDialogFragment newInstance(){
			return new ImportDialogFragment();
		}
		
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	    	View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_import, null, false);
	    	
	    	v.findViewById(R.id.import_from_device).setOnClickListener(new View.OnClickListener() {
	    		@Override
	    		public void onClick(View v) {
	    			int index = ((MainActivity)getActivity()).mViewPager.getCurrentItem();
	    			ProfileFragment f = (ProfileFragment) getFragmentManager().findFragmentByTag(
	                        "android:switcher:"+R.id.pager+":"+index); //that is the tag the ViewPager sets to the fragment

	    			f.mProfileTask = (ProfileTask) f.new ProfileTask(null, 1).execute();
	    			dismiss();            	
	    		}
	    	});
	    	
	    	v.findViewById(R.id.import_from_lastfm).setOnClickListener(new View.OnClickListener() {
	    		@Override
	    		public void onClick(View v) {
	    			dismiss();	    			
	    			
	    			int index = ((MainActivity)getActivity()).mViewPager.getCurrentItem();
	    			ProfileFragment f = (ProfileFragment) getFragmentManager().findFragmentByTag(
	                        "android:switcher:"+R.id.pager+":"+index); //that is the tag the ViewPager sets to the fragment
	    			
	    			InputDialogFragment dialog = InputDialogFragment
	    	    			.newInstance(R.string.lastfm_username, "lastfm-username", f);
	    	    	dialog.showDialog(getActivity());
	    		}
	    	});    	
	    	
	    	Dialog dialog = new Dialog(getActivity(), R.style.DialogThemeNoTitle);
	    	dialog.setContentView(v);
	    	return dialog;
	    }
	}
}
