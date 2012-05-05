package com.seekermob.songseeker.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.RdioComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceID;
import com.seekermob.songseeker.data.PlaylistInfo;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.Util;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;

public class ExportPlaylistRdioFragment extends SherlockListFragment {

	protected PlaylistsAdapter mAdapter;
	
	private AuthorizeTask mAuthorizeTask;
	private boolean mIsAuthTaskRunning;
	private UserPlaylistsTask mUserPlaylistsTask;
	private CreatePlaylistTask mCreatePlaylistTask;
		
	private Bundle mSavedState;
		
	private static final String STATE_ADAPTER_DATA = "adapterData";
	private static final String STATE_AUTH_TASK_RUNNING = "authTaskRunning";
	private static final String STATE_USER_PLAYLISTS_TASK_RUNNING = "userPlaylistsTaskRunning";
	private static final String STATE_CREATE_PLAYLIST_TASK_RUNNING = "createPlaylistTaskRunning";
	private static final String STATE_CREATE_PLAYLIST_CHOSEN = "createPlaylistTaskChosen"; //playlist chosen by the user to export
	private static final String STATE_CREATE_PLAYLIST_IDS = "createPlaylistTaskIds";
	private static final String STATE_CREATE_PLAYLIST_INDEX = "createPlaylistTaskIndex";
		
	public static final String BUNDLE_PLAYLIST = "playlist";
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		super.onActivityCreated(savedInstanceState);
		
	    //populate the optionsMenu
	    setHasOptionsMenu(true);
		
		//set adapter				
		mAdapter = new PlaylistsAdapter();		
		setListAdapter(mAdapter);

		//restore state
		restoreLocalState(savedInstanceState);
		
		//if not authorized, request auth 
		//but only if the task wasnt restored on restoreLocalState
		if(!RdioComm.getComm(getActivity()).isAuthorized() && !isAuthTaskRunning()){		
			login();
		}
		
		//if the adapter wasnt restored, fetch the adapter
		//but only if the task wasnt restored on restoreLocalState
		if(RdioComm.getComm(getActivity()).isAuthorized() &&	mAdapter.mPlaylists == null && 
			!isAuthTaskRunning() && !isUserPlaylistsTaskRunning()){
			
			mUserPlaylistsTask = (UserPlaylistsTask) new UserPlaylistsTask().execute();
		}		
	}	
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		
		//save the adapter data
		if(mAdapter != null && mAdapter.mPlaylists != null){
			outState.putParcelableArrayList(STATE_ADAPTER_DATA, new ArrayList<Parcelable>(mAdapter.mPlaylists));			
		}
		
		//save the task state
		final AuthorizeTask authTask = mAuthorizeTask;
        if(mIsAuthTaskRunning || (authTask != null && authTask.getStatus() != AsyncTask.Status.FINISHED)) {
        	if(authTask != null)
        		authTask.cancel(true);
        	
        	outState.putBoolean(STATE_AUTH_TASK_RUNNING, true);        	
        	mAuthorizeTask = null;
        }
        
		final UserPlaylistsTask userPlsTask = mUserPlaylistsTask;
        if(userPlsTask != null && userPlsTask.getStatus() != AsyncTask.Status.FINISHED) {
        	userPlsTask.cancel(true);
        	
        	outState.putBoolean(STATE_USER_PLAYLISTS_TASK_RUNNING, true);
        	mUserPlaylistsTask = null;
        }
		
        final CreatePlaylistTask createPlsTask = mCreatePlaylistTask;
        if(createPlsTask != null && createPlsTask.getStatus() != AsyncTask.Status.FINISHED) {
        	createPlsTask.cancel(true);
        	
        	outState.putBoolean(STATE_CREATE_PLAYLIST_TASK_RUNNING, true);
        	outState.putStringArrayList(STATE_CREATE_PLAYLIST_IDS, createPlsTask.mSongIds);
        	outState.putInt(STATE_CREATE_PLAYLIST_INDEX, createPlsTask.mFetchCount.get());
        	outState.putParcelable(STATE_CREATE_PLAYLIST_CHOSEN, createPlsTask.mChosenPlaylist);
        	mCreatePlaylistTask = null;
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
		ArrayList<PlaylistInfo> adapterData;
		if((adapterData = savedInstanceState.getParcelableArrayList(STATE_ADAPTER_DATA)) != null){
			mAdapter.setAdapter(adapterData);			
		}
		
		//restore the tasks
		if(savedInstanceState.getBoolean(STATE_AUTH_TASK_RUNNING)){			
			//the oauth login is difficult to restore properly
			//i'm not restoring the task here intentionally
			mIsAuthTaskRunning = true;
			Util.setListShown(ExportPlaylistRdioFragment.this, false);
			((TextView)(getListView().getEmptyView())).setText(R.string.not_authorized);		
		}
		
		if(savedInstanceState.getBoolean(STATE_USER_PLAYLISTS_TASK_RUNNING)){
			mUserPlaylistsTask = (UserPlaylistsTask) new UserPlaylistsTask().execute();
		}		
		
		ArrayList<String> songIds;
		PlaylistInfo chosenPlaylist;
		int index;
		if(savedInstanceState.getBoolean(STATE_CREATE_PLAYLIST_TASK_RUNNING) &&
			(songIds = savedInstanceState.getStringArrayList(STATE_CREATE_PLAYLIST_IDS)) != null &&
			(chosenPlaylist = savedInstanceState.getParcelable(STATE_CREATE_PLAYLIST_CHOSEN)) != null){
			
			index = savedInstanceState.getInt(STATE_CREATE_PLAYLIST_INDEX);
			
			mCreatePlaylistTask = (CreatePlaylistTask) new CreatePlaylistTask(chosenPlaylist, songIds, index).execute(); 
		}
		
		mSavedState = null;
	}	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(isAuthTaskRunning() && mAuthorizeTask != null)
			mAuthorizeTask.cancel(true);
		
		/*if(isUserPlaylistsTaskRunning())
			mUserPlaylistsTask.cancel(true);
		
		if(isCreatePlaylistTaskRunning())
			mCreatePlaylistTask.cancel(true);*/
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
	
	@Override
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.export_playlist_menu, menu);		
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_new_playlist) {
			if(!RdioComm.getComm(getActivity()).isAuthorized()){
				Toast.makeText(getActivity(), R.string.not_authorized, Toast.LENGTH_SHORT).show();
				return true;
			}
			if(isAnyTaskRunning()){
				Toast.makeText(getActivity().getApplicationContext(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
				return true;
			}
			InputDialogFragment dialog = InputDialogFragment.newInstance(R.string.playlist_name, "playlistName");
			dialog.showDialog(getActivity());
			return true;
		} else if (item.getItemId() == R.id.menu_login) {
			if(isAnyTaskRunning()){
				Toast.makeText(getActivity(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
				return true;
			}
			RdioComm.getComm(getActivity()).cleanAuthTokens(getActivity());
			login();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}			
	
	private class PlaylistsAdapter extends BaseAdapter{
		private ArrayList<PlaylistInfo> mPlaylists;
		private LayoutInflater mInflater;
		
		public PlaylistsAdapter() {
			mInflater = getActivity().getLayoutInflater();
		}
		
		public int getCount() {
			if(mPlaylists == null)
				return 0;
			
			return mPlaylists.size();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return position;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
	    	
			if (convertView == null) {
			   	convertView = mInflater.inflate(R.layout.list_item_2_image_media, null);
				
				holder = new ViewHolder();
				holder.topText = (TextView) convertView.findViewById(R.id.firstLine);
			    holder.botText = (TextView) convertView.findViewById(R.id.secondLine);
			    holder.image = (ImageView) convertView.findViewById(R.id.image);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}				
						
		    holder.topText.setText(mPlaylists.get(position).name);
		    holder.botText.setText(mPlaylists.get(position).numSongs + " " + getString(R.string.songs).toLowerCase());
		    
		    ImageLoader.getLoader().DisplayImage(mPlaylists.get(position).imageUrl, holder.image, R.drawable.ic_disc_stub, ImageSize.SMALL);
		    return convertView;
		}
		
		public void setAdapter(ArrayList<PlaylistInfo> pls){
			mPlaylists = pls;
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
		if(isCreatePlaylistTaskRunning()){
			Toast.makeText(getActivity().getApplicationContext(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
			return;
		}
		
		mCreatePlaylistTask = (CreatePlaylistTask) new CreatePlaylistTask(mAdapter.mPlaylists.get(position)).execute();
	}	
	
	private void login() {	
		mIsAuthTaskRunning = true;
		((TextView)(getListView().getEmptyView())).setText(R.string.not_authorized);		
		mAuthorizeTask = (AuthorizeTask) new AuthorizeTask().execute();		
	}
	
	public void authorize(String verifier){
		mAuthorizeTask = (AuthorizeTask) new AuthorizeTask(verifier).execute();
	}
	
	private class AuthorizeTask extends AsyncTask<Void, Void, Void>{
		private boolean mIsRequest; 	//true if we are fetching the request token, false if we are requesting the access tokens
		private String mVerifier;		//used when retrieving access tokens
		private String err = null;
		
		public AuthorizeTask() {
			mIsRequest = true;
		}
		
		public AuthorizeTask(String verifier){
			mIsRequest = false;
			mVerifier = verifier;
		}

		@Override
		protected void onPreExecute() {
			Util.setListShown(ExportPlaylistRdioFragment.this, false);
		    ((TextView)(getListView().getEmptyView())).setText(R.string.not_authorized);
		}
		
		@Override
		protected Void doInBackground(Void... args) {

			try {		
				if(isCancelled())
					return null;
				
				if(mIsRequest)
					RdioComm.getComm(getActivity()).retrieveRequestToken(getActivity());
				else
					RdioComm.getComm(getActivity()).retrieveAccessTokens(mVerifier, getActivity());				
				
			} catch (ServiceCommException e) {
				err = e.getMessage(getActivity());
				return null;
			}
			
			return null; 
		}
		
		@Override
		protected void onPostExecute(Void result) {
				
			//resets the adapter
			mAdapter.setAdapter(null);
			
			if(err != null){				
				Util.setListShown(ExportPlaylistRdioFragment.this, true);
				Toast.makeText(getActivity().getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				return;
			}
			
			if(!mIsRequest){
				mIsAuthTaskRunning = false;
				mUserPlaylistsTask = (UserPlaylistsTask) new UserPlaylistsTask().execute();
			}
			
		}		
	}
	
	private boolean isAuthTaskRunning(){
		if(mIsAuthTaskRunning || (mAuthorizeTask != null && mAuthorizeTask.getStatus() != AsyncTask.Status.FINISHED))
			return true;
		
		return false;
	}
	
	private class UserPlaylistsTask extends AsyncTask<Void, Void, ArrayList<PlaylistInfo>>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			Util.setListShown(ExportPlaylistRdioFragment.this, false);
			((TextView)(getListView().getEmptyView())).setText(R.string.user_playlists_frag_empty);
		}
		
		@Override
		protected ArrayList<PlaylistInfo> doInBackground(Void... arg0) {
			ArrayList<PlaylistInfo> playlists;
			try{
				playlists = RdioComm.getComm().getUserPlaylists();
			} catch(ServiceCommException e){
				err = e.getMessage(getActivity());
				return null;
			}
			
			return playlists;
		}
		
		@Override
		protected void onPostExecute(ArrayList<PlaylistInfo> playlists) {
			Util.setListShown(ExportPlaylistRdioFragment.this, true);
			
			if(err != null){
				Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();
				return;
			}
			
			Toast.makeText(getActivity(), R.string.choose_playlist_export_tip, Toast.LENGTH_LONG).show();
			mAdapter.setAdapter(playlists);
		}		
	}
	
	private boolean isUserPlaylistsTaskRunning(){
		if(mUserPlaylistsTask != null && mUserPlaylistsTask.getStatus() != AsyncTask.Status.FINISHED)
			return true;
		
		return false;
	}	
	
	private class CreatePlaylistTask extends AsyncTask<Void, Integer, Void>{
		private PlaylistInfo mChosenPlaylist;
		
		private List<SongInfo> mPlaylist;
		private ArrayList<String> mSongIds;
		
		private View mProgressOverlay;
		private ProgressBar mUpdateProgress;
		final AtomicInteger mFetchCount = new AtomicInteger();
		
		private String err;		
		
		public CreatePlaylistTask(PlaylistInfo pl) {
			mChosenPlaylist = pl;
			mPlaylist = getArguments().getParcelableArrayList(BUNDLE_PLAYLIST);
			mSongIds = new ArrayList<String>();
		}
		
		//used when recovering the state
		public CreatePlaylistTask(PlaylistInfo pl, ArrayList<String> songIds, int index) {
			mChosenPlaylist = pl;
			mSongIds = songIds;
			mFetchCount.set(index);
			mPlaylist = getArguments().getParcelableArrayList(BUNDLE_PLAYLIST);			
		}		
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			if(progress[0] == -1){
				mUpdateProgress.setIndeterminate(true);
				return;
			}

			mUpdateProgress.setProgress(progress[0]);
		}			
		
		@Override
		protected void onPreExecute() {
			
            // see if we already inflated the progress overlay
            mProgressOverlay = Util.setProgressShown(ExportPlaylistRdioFragment.this, true);
            
            // setup the progress overlay
            TextView mUpdateStatus = (TextView) mProgressOverlay
                    .findViewById(R.id.textViewUpdateStatus);
            mUpdateStatus.setText(R.string.exporting);

            mUpdateProgress = (ProgressBar) mProgressOverlay
                    .findViewById(R.id.ProgressBarShowListDet);

            View cancelButton = mProgressOverlay.findViewById(R.id.overlayCancel);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    cancelCreatePlaylistTask();
                }
            });
			
			mUpdateProgress.setIndeterminate(false);
			mUpdateProgress.setMax(mPlaylist.size());
			mUpdateProgress.setProgress(mFetchCount.get());
		}	
		
		@Override
		protected Void doInBackground(Void... params) {
			
			if(!RdioComm.getComm().isAuthorized()){
				ServiceCommException e = new ServiceCommException(ServiceID.RDIO, ServiceErr.NOT_AUTH);
				err = e.getMessage(getActivity());
				return null;
			}
			
			final AtomicInteger fetchCount = mFetchCount;
			final ArrayList<SongInfo> playlist = new ArrayList<SongInfo>(mPlaylist.subList(fetchCount.get(), mPlaylist.size()));			
			for(SongInfo song : playlist){
				
				if(isCancelled()){
					return null;
				}
				
				try {					
					mSongIds.add(RdioComm.getComm().queryTrackID(song.name, song.artist.name));										
				}catch (ServiceCommException e) {
					if(e.getErr() == ServiceErr.SONG_NOT_FOUND){
						publishProgress(fetchCount.incrementAndGet());
						Log.i(Util.APP, "Song ["+song.name+" - "+song.artist.name+"] not found in Rdio, ignoring...");
						continue;
					}
					
					Log.w(Util.APP, "Failed to export playlist to Rdio!", e);
					err = e.getMessage(getActivity());
					return null;
				} 
				
				publishProgress(fetchCount.incrementAndGet());
			}
			
			if(isCancelled()){
				return null;
			}
			
			if(mSongIds.isEmpty()){
				err = getString(R.string.no_song_found);
				return null;
			}
			
			publishProgress(-1);
			
			try{
				if(mChosenPlaylist.id != null){					
					//add songs to existing playlist, if id is given					
					RdioComm.getComm().addToPlaylist(mChosenPlaylist.id, mSongIds, getActivity());
				}else{
					//create new playlist if the name is given
					RdioComm.getComm().createPlaylist(mChosenPlaylist.name, mSongIds, getActivity());										
				}
			}catch(ServiceCommException e){
				err = e.getMessage(getActivity());				
			}			
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			Util.setProgressShown(ExportPlaylistRdioFragment.this, false);
			
			if(err != null){
				Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(getActivity(), getResources().getString(R.string.playlist_created), Toast.LENGTH_LONG).show();
			}
		}		
		
		@Override
		protected void onCancelled() {
			Util.setProgressShown(ExportPlaylistRdioFragment.this, false);
		}
	}	
	
	public void exportToNewPlaylist(String playlistName){
		PlaylistInfo pl = new PlaylistInfo();
		pl.name = playlistName;
		mCreatePlaylistTask = (CreatePlaylistTask) new CreatePlaylistTask(pl).execute();
	}
	
	private boolean isCreatePlaylistTaskRunning(){
		if(mCreatePlaylistTask != null && mCreatePlaylistTask.getStatus() != AsyncTask.Status.FINISHED)
			return true;
		
		return false;
	}
	
	private void cancelCreatePlaylistTask(){
		if(!isCreatePlaylistTaskRunning())
			return;
		
		mCreatePlaylistTask.cancel(true);
		mCreatePlaylistTask = null;
	}	

	private boolean isAnyTaskRunning(){
		if(isAuthTaskRunning() || isUserPlaylistsTaskRunning() || isCreatePlaylistTaskRunning())
			return true;
		
		return false;
	}
}
