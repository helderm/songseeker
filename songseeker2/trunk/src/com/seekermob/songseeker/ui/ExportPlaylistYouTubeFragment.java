package com.seekermob.songseeker.ui;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.YouTubeComm;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceID;
import com.seekermob.songseeker.data.PlaylistInfo;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.data.VideoInfo;
import com.seekermob.songseeker.util.Util;

public class ExportPlaylistYouTubeFragment extends SherlockListFragment implements AccountManagerCallback<Bundle>{

	protected PlaylistsAdapter mAdapter;
	private UserPlaylistsTask mUserPlaylistsTask;
	private CreatePlaylistTask mCreatePlaylistTask;
	private boolean mIsAuthTaskRunning;
	
	private Bundle mSavedState;
		
	private static final String STATE_ADAPTER_DATA = "adapterData";
	private static final String STATE_AUTH_TASK_RUNNING = "authTaskRunning";
	private static final String STATE_PLAYLISTS_TASK_RUNNING = "playlistsTaskRunning";
	private static final String STATE_CREATE_PLAYLIST_TASK_RUNNING = "createPlaylistTaskRunning";
	private static final String STATE_CREATE_PLAYLIST_CHOSEN = "createPlaylistTaskChosen"; //playlist chosen by the user to export
	private static final String STATE_CREATE_PLAYLIST_IDS = "createPlaylistTaskIds";
	private static final String STATE_CREATE_PLAYLIST_FETCH_INDEX = "createPlaylistTaskFetchIndex";
	private static final String STATE_CREATE_PLAYLIST_ADD_INDEX = "createPlaylistTaskAddIndex";
	
	public static final String BUNDLE_PLAYLIST = "playlist";
	private static final int REQUEST_AUTHENTICATE = 0;
		
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
		if(!YouTubeComm.getComm(getActivity()).isAuthorized() && !mIsAuthTaskRunning){		
			login();
		}
		
		//if the adapter wasnt restored, fetch the adapter
		//but only if the task wasnt restored on restoreLocalState
		if(YouTubeComm.getComm(getActivity()).isAuthorized() &&	mAdapter.mPlaylists == null && 
			!mIsAuthTaskRunning && !isUserPlaylistsTaskRunning()){
			
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
		if(mIsAuthTaskRunning){
			outState.putBoolean(STATE_AUTH_TASK_RUNNING, true);
		}
        
        final UserPlaylistsTask userPlsTask = mUserPlaylistsTask;
        if(userPlsTask != null && userPlsTask.getStatus() != AsyncTask.Status.FINISHED) {
        	userPlsTask.cancel(true);
        	
        	outState.putBoolean(STATE_PLAYLISTS_TASK_RUNNING, true);
        	mUserPlaylistsTask = null;
        }
		
        final CreatePlaylistTask createPlsTask = mCreatePlaylistTask;
        if(createPlsTask != null && createPlsTask.getStatus() != AsyncTask.Status.FINISHED) {
        	createPlsTask.cancel(true);
        	
        	outState.putBoolean(STATE_CREATE_PLAYLIST_TASK_RUNNING, true);
        	outState.putParcelable(STATE_CREATE_PLAYLIST_CHOSEN, createPlsTask.mChosenPlaylist);
        	outState.putStringArrayList(STATE_CREATE_PLAYLIST_IDS, createPlsTask.mVideoIds);
        	outState.putInt(STATE_CREATE_PLAYLIST_FETCH_INDEX, createPlsTask.mFetchCount.get());
        	outState.putInt(STATE_CREATE_PLAYLIST_ADD_INDEX, createPlsTask.mAddCount.get());
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
			((TextView)(getListView().getEmptyView())).setText(R.string.not_authorized);
			mIsAuthTaskRunning = true;
		}
		
		if(savedInstanceState.getBoolean(STATE_PLAYLISTS_TASK_RUNNING)){
			mUserPlaylistsTask = (UserPlaylistsTask) new UserPlaylistsTask().execute();
		}		
		
		PlaylistInfo chosenPlaylist;
		ArrayList<String> videoIds;
		if(savedInstanceState.getBoolean(STATE_CREATE_PLAYLIST_TASK_RUNNING) &&			
			(chosenPlaylist = savedInstanceState.getParcelable(STATE_CREATE_PLAYLIST_CHOSEN)) != null &&
			(videoIds = savedInstanceState.getStringArrayList(STATE_CREATE_PLAYLIST_IDS)) != null){
	
			int fetchIndex = savedInstanceState.getInt(STATE_CREATE_PLAYLIST_FETCH_INDEX);
			int addIndex = savedInstanceState.getInt(STATE_CREATE_PLAYLIST_ADD_INDEX);
			
			mCreatePlaylistTask = (CreatePlaylistTask) new CreatePlaylistTask(chosenPlaylist, videoIds, fetchIndex, addIndex).execute(); 
		}
		
		mSavedState = null;
	}		
	
	@Override
	public void onDestroy() {
		super.onDestroy();
			
		if(isUserPlaylistsTaskRunning())
			mUserPlaylistsTask.cancel(true);
		
		if(isCreatePlaylistTaskRunning())
			mCreatePlaylistTask.cancel(true);
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
		switch (item.getItemId()) {
		case R.id.menu_new_playlist:
			if(!YouTubeComm.getComm(getActivity()).isAuthorized()){
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
		case R.id.menu_login:
			if(isAnyTaskRunning()){
				Toast.makeText(getActivity(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
				return true;
			}
			
			login();			
			return true;
		default:
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
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}				
						
		    holder.topText.setText(mPlaylists.get(position).name);
		    holder.botText.setText(mPlaylists.get(position).numSongs + " " + getString(R.string.videos));
		    return convertView;
		}
		
		public void setAdapter(ArrayList<PlaylistInfo> pls){
			mPlaylists = pls;
			notifyDataSetChanged();
		}
		
	    private class ViewHolder{
	    	public TextView topText;
	    	public TextView botText;
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
		//check if the user has a Google account configured
		final String[] names = YouTubeComm.getComm().getAccountsNames(getActivity());
		if(names.length <= 0){
			Toast.makeText(getActivity(), R.string.no_google_account_found, Toast.LENGTH_SHORT).show();			
			startActivity(new Intent(Settings.ACTION_SYNC_SETTINGS));
			getActivity().finish();
			return;
		}
		
		((TextView)(getListView().getEmptyView())).setText(R.string.not_authorized);
		AccountsDialogFragment dialog = AccountsDialogFragment.newInstance(names);
		dialog.showDialog(getActivity());		
	}
	
	private void authorize(String account){
		mIsAuthTaskRunning = true;
		Util.setListShown(ExportPlaylistYouTubeFragment.this, false);
		
		//resets the adapter
		mAdapter.setAdapter(null);
		
		YouTubeComm.getComm(getActivity()).requestAuthorize(account, ExportPlaylistYouTubeFragment.this, getActivity());		
	}	

	//requestAuthorize callback function
	@Override
	public void run(AccountManagerFuture<Bundle> future) {
				
		try {
			Bundle bundle = future.getResult();
			if(bundle.containsKey(AccountManager.KEY_INTENT)) {
				//user didn't yet authorize our app within Google, sending an Intent to do so
				Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
				intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivityForResult(intent, REQUEST_AUTHENTICATE);
			} else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
				//user already authorized us with Google
				mIsAuthTaskRunning = false;
				Util.setListShown(ExportPlaylistYouTubeFragment.this, true);
				YouTubeComm.getComm().setAccessToken(bundle.getString(AccountManager.KEY_AUTHTOKEN), getActivity());				
				mUserPlaylistsTask = (UserPlaylistsTask) new UserPlaylistsTask().execute();
			}
		} catch (Exception e) {
			mIsAuthTaskRunning = false;
			Util.setListShown(ExportPlaylistYouTubeFragment.this, true);
			handleException(e);
		}		
	}
	
	//startActivityForResult(intent, REQUEST_AUTHENTICATE) callback
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		mIsAuthTaskRunning = false;
		Util.setListShown(ExportPlaylistYouTubeFragment.this, true);
		
		switch (requestCode) {
		case REQUEST_AUTHENTICATE:
			if (resultCode == Activity.RESULT_OK) {
				//user authorized us within Google
				mUserPlaylistsTask = (UserPlaylistsTask) new UserPlaylistsTask().execute();
			} else {
				//user denied our app
				YouTubeComm.getComm(getActivity()).unauthorizeUser(getActivity());
				ServiceCommException e = new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.NOT_AUTH);
				Toast.makeText(getActivity(), e.getMessage(getActivity()), Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}

	void handleException(Exception e){
		if (e instanceof HttpResponseException) {
			HttpResponse response = ((HttpResponseException) e).getResponse();
			int statusCode = response.getStatusCode();

			if (statusCode == 401 || statusCode == 403) {	          
				YouTubeComm.getComm().unauthorizeUser(getActivity());
				
				ServiceCommException ex = new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.NOT_AUTH);
				Toast.makeText(getActivity(), ex.getMessage(getActivity()), Toast.LENGTH_SHORT).show();
				return;
			}
			
			ServiceCommException ex = new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.UNKNOWN);
			Toast.makeText(getActivity(), ex.getMessage(getActivity()), Toast.LENGTH_SHORT).show();			
		}
		
		Log.e(Util.APP, "YouTube auth failed!", e);
	}

	private class UserPlaylistsTask extends AsyncTask<Void, Void, ArrayList<PlaylistInfo>>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			Util.setListShown(ExportPlaylistYouTubeFragment.this, false);
			((TextView)(getListView().getEmptyView())).setText(R.string.user_playlists_frag_empty);
		}
		
		@Override
		protected ArrayList<PlaylistInfo> doInBackground(Void... arg0) {
			ArrayList<PlaylistInfo> playlists;
			try{
				playlists = YouTubeComm.getComm().getUserPlaylists(getActivity());
			} catch(ServiceCommException e){
				err = e.getMessage(getActivity());
				return null;
			}
			
			return playlists;
		}
		
		@Override
		protected void onPostExecute(ArrayList<PlaylistInfo> playlists) {
			Util.setListShown(ExportPlaylistYouTubeFragment.this, true);
			
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

		private ArrayList<SongInfo> mPlaylist;
		private ArrayList<String> mVideoIds;
		private AtomicInteger mFetchCount = new AtomicInteger();
		private AtomicInteger mAddCount = new AtomicInteger();
		
		private View mProgressOverlay;
		private ProgressBar mUpdateProgress;
		
		private String err;

		public CreatePlaylistTask(PlaylistInfo pl) {
			mChosenPlaylist = pl;
			mPlaylist = getArguments().getParcelableArrayList(BUNDLE_PLAYLIST);
			mVideoIds = new ArrayList<String>();
		}

		//used when recovering the state
		public CreatePlaylistTask(PlaylistInfo pl, ArrayList<String> videoIds, int fetchIndex, int addIndex) {
			mChosenPlaylist = pl;
			mVideoIds = videoIds;
			mFetchCount.set(fetchIndex);
			mAddCount.set(addIndex);
			mPlaylist = getArguments().getParcelableArrayList(BUNDLE_PLAYLIST);			
		}	
		
		@Override
		protected void onPreExecute() {

			// see if we already inflated the progress overlay
			mProgressOverlay = Util.setProgressShown(ExportPlaylistYouTubeFragment.this, true);

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

			mUpdateProgress.setIndeterminate(true);
			mUpdateProgress.setMax(mPlaylist.size());			
		}		

		@Override
		protected Void doInBackground(Void... params) {
			
			try{
				//fetch video ids		
				final AtomicInteger fetchCount = mFetchCount;
				final ArrayList<SongInfo> playlist = new ArrayList<SongInfo>(mPlaylist.subList(fetchCount.get(), mPlaylist.size()));
				for(SongInfo song : playlist){
					if(isCancelled()){
						return null;
					}
					
					ArrayList<VideoInfo> vids = YouTubeComm.getComm().searchVideo(song.name, song.artist.name, 1);
					
					if(vids.size() == 0){
						Log.i(Util.APP, "Song ["+song.name + "-" + song.artist.name+"] not found on YouTube, ignoring...");
						mFetchCount.incrementAndGet();
						continue;
					}
				
					mVideoIds.add(vids.get(0).id);
					mFetchCount.incrementAndGet();
				}
				
				if(isCancelled()){
					return null;
				}
				
				//create a playlist
				if(mChosenPlaylist.id == null){					
					mChosenPlaylist.id = YouTubeComm.getComm().createPlaylist(mChosenPlaylist.name, getActivity());					
				}
				
				//add videos to the playlist
				final AtomicInteger addCount = mAddCount;
				final ArrayList<String> videoIds = new ArrayList<String>(mVideoIds.subList(addCount.get(), mVideoIds.size()));
				for(String videoId : videoIds){					
					
					if(isCancelled()){
						return null;
					}

					YouTubeComm.getComm().addVideosToPlaylist(mChosenPlaylist.id, videoId, getActivity());
					mAddCount.incrementAndGet();
				}		
			}catch (ServiceCommException e){
				err = e.getMessage(getActivity());
				return null;
			}
			
			return null;
		}		

		@Override
		protected void onPostExecute(Void result) {
			Util.setProgressShown(ExportPlaylistYouTubeFragment.this, false);

			if(err != null){
				Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(getActivity(), getResources().getString(R.string.playlist_created), Toast.LENGTH_LONG).show();
			}
		}		

		@Override
		protected void onCancelled() {
			Util.setProgressShown(ExportPlaylistYouTubeFragment.this, false);
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
		if(isUserPlaylistsTaskRunning() || isCreatePlaylistTaskRunning())
			return true;
		
		return false;
	}
	
	public static class AccountsDialogFragment extends DialogFragment{
		
		private static final String TAG = "accounts-dialog";
		
		public static AccountsDialogFragment newInstance(String[] accounts){
			AccountsDialogFragment dialog = new AccountsDialogFragment();
			Bundle args = new Bundle();
			args.putStringArray("accounts", accounts);
			dialog.setArguments(args);
			return dialog;
		}
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final String[] accounts = getArguments().getStringArray("accounts");
			
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.select_google_account);
			
			builder.setItems(accounts, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {					
					dismiss();
					ExportPlaylistYouTubeFragment f = (ExportPlaylistYouTubeFragment) getFragmentManager().
							findFragmentByTag(ExportPlaylistYouTubeActivity.FRAGMENT_TAG);
					f.authorize(accounts[which]);
				}
			});
			
			return builder.create();
		}
		
		public void showDialog(FragmentActivity activity){
			//show dialog
			FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
			Fragment prev = activity.getSupportFragmentManager().findFragmentByTag(TAG);
			if(prev != null){
				ft.remove(prev);
			}
			
			show(ft, TAG);					
		}
	}
}
