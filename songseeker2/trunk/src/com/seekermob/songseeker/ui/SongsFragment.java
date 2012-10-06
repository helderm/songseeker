package com.seekermob.songseeker.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.PlaylistParams.PlaylistType;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.EchoNestComm;
import com.seekermob.songseeker.comm.GroovesharkComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.PlaylistOptions;
import com.seekermob.songseeker.data.RecSongsPlaylist;
import com.seekermob.songseeker.data.RecSongsPlaylist.PlaylistListener;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.data.UserProfile;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.Util;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;
import com.seekermob.songseeker.util.MediaPlayerController;
import com.seekermob.songseeker.util.MediaPlayerController.MediaStatus;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class SongsFragment extends SherlockListFragment implements PlaylistListener{
	protected SongsAdapter mAdapter;
	private PlaySongsTask mPlaySongsTask;
	private SongDetailsTask mSongDetailsTask;
	private Bundle mSavedState;
	
	private static final String STATE_PLAYLIST = "playlist";
	private static final String STATE_GET_PLAYLIST_RUNNING = "playlistRunning";
	private static final String STATE_PLAY_SONGS_IDS = "playSongsIds";
	private static final String STATE_PLAY_SONGS_INDEX = "playSongsIndex";
	private static final String STATE_PLAY_SONGS_RUNNING = "playSongsRunning";
	private static final String STATE_PLAY_SONGS_PLAYER = "playSongsPlayer";
	private static final String STATE_SONG_DETAILS_RUNNING = "songDetailsRunning";
	private static final String STATE_SONG_DETAILS_SONG = "songDetailsSong";
	
	private static final int MENU_REMOVE_SONG = 10;
	
	public static final int TAB_ID = 0;	
	public static final String DIALOG_ARTIST_NAME = "songsArtistName";
		
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		//populate the optionsMenu 
		setHasOptionsMenu(true);

		//register a listener for playlist data changes
		RecSongsPlaylist.getInstance().registerListener(this);
		
		//set adapter
		mAdapter = new SongsAdapter();
		//setLogoFooter();
		setListAdapter(mAdapter);
		
		//check if we are recovering the state
		restoreLocalState(savedInstanceState);

		//set main onClick on emptyView that fetches data from EN		
		((TextView)(getListView().getEmptyView())).setText(R.string.songs_frag_empty_list);
		getListView().getEmptyView().setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				getNewPlaylist(null);
			}
		});	
		
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

		//cancel the playlist task
		RecSongsPlaylist.getInstance().cancelTask();		
		
		//clears the data from the playlist
		RecSongsPlaylist.getInstance().clearPlaylist();

		//unregister the listener
		RecSongsPlaylist.getInstance().unregisterListener(this);

		//cancel the play songs task
		if(mPlaySongsTask != null)
			mPlaySongsTask.cancel(true);
		
		//cancel the song details task
		if(mSongDetailsTask != null)
			mSongDetailsTask.cancel(true);		
		
		super.onDestroy();
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
		
		//save the playlist
		if(mAdapter != null && mAdapter.playlist != null){
			outState.putParcelableArrayList(STATE_PLAYLIST, new ArrayList<Parcelable>(mAdapter.playlist));			
		}

		//save if the playlist task was running or not
		if(RecSongsPlaylist.getInstance().isTaskRunning()){
			RecSongsPlaylist.getInstance().cancelTask();
			
			outState.putBoolean(STATE_GET_PLAYLIST_RUNNING, true);
		}
		
		//save the progress of the 'Play Songs' dialog
        final PlaySongsTask task = mPlaySongsTask;
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            task.cancel(true);

            outState.putBoolean(STATE_PLAY_SONGS_RUNNING, true);
            outState.putStringArrayList(STATE_PLAY_SONGS_IDS, task.mSongIds);
            outState.putInt(STATE_PLAY_SONGS_INDEX, task.mFetchCount.get());
            outState.putInt(STATE_PLAY_SONGS_PLAYER, task.mPlayer);

            mPlaySongsTask = null;
        }        
        
		//save the progress of the SongDetails task
        final SongDetailsTask songTask = mSongDetailsTask;
        if (songTask != null && songTask.getStatus() != AsyncTask.Status.FINISHED) {
        	songTask.cancel(true);

            outState.putBoolean(STATE_SONG_DETAILS_RUNNING, true);
            outState.putParcelable(STATE_SONG_DETAILS_SONG, songTask.mSelectedSong);
            mSongDetailsTask = null;
        }        
        
        mSavedState = outState;
		
		super.onSaveInstanceState(outState);
	}
	
	/** Restores the saved instance of this fragment*/
	private void restoreLocalState(Bundle savedInstanceState){		

		if(savedInstanceState == null){
			return;
		}
		
		//restore the playlist
		ArrayList<SongInfo> savedPlaylist = null;
		if((savedPlaylist = savedInstanceState.getParcelableArrayList(STATE_PLAYLIST)) != null){
			if(RecSongsPlaylist.getInstance().isEmpty()){
				RecSongsPlaylist.getInstance().setSongs(savedPlaylist);
			}
			savedPlaylist = null;

			mAdapter.setPlaylist(RecSongsPlaylist.getInstance().getPlaylist());			
		}	
		
		//restore the playlist task
		if(savedInstanceState.getBoolean(STATE_GET_PLAYLIST_RUNNING)) {
			getNewPlaylist(null); //FIXME: im not restoring the artist name from the input here
		}
		
		//restore the playsongs task
		if(savedInstanceState.getBoolean(STATE_PLAY_SONGS_RUNNING)) {
			ArrayList<String> ids = savedInstanceState.getStringArrayList(STATE_PLAY_SONGS_IDS);
			int index = savedInstanceState.getInt(STATE_PLAY_SONGS_INDEX);
			int player = savedInstanceState.getInt(STATE_PLAY_SONGS_PLAYER);

			if (ids != null) {
				mPlaySongsTask = (PlaySongsTask) new PlaySongsTask(ids, index, player).execute();
			}
		}
		
		//restore the song details task
		if(savedInstanceState.getBoolean(STATE_SONG_DETAILS_RUNNING)) {
			SongInfo song = savedInstanceState.getParcelable(STATE_SONG_DETAILS_SONG);
			mSongDetailsTask = (SongDetailsTask) new SongDetailsTask(song).execute();			
		}		
		
		mSavedState = null;
	}	

	@Override
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.songs_menu, menu);		
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		
		if (item.getItemId() == R.id.menu_play_songs) {
			
			PlayerDialogFragment playerDiag = PlayerDialogFragment.newInstance();
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			playerDiag.show(ft, "player-dialog");
			
			//playSongsIntoDoodsMusic();
			return true;
		} else if (item.getItemId() == R.id.menu_refresh_songs) {
			if(isAnyTaskRunning()){
				Toast.makeText(getActivity(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
				return true;
			}
			
			getNewPlaylist(null);
			return true;
		} else if (item.getItemId() == R.id.menu_playlist_options) {
			i = new Intent(getActivity(), PlaylistOptionsActivity.class);
			startActivity(i);
			return true;
		} else if (item.getItemId() == R.id.menu_export_playlist) {
			if(isAnyTaskRunning()){
				Toast.makeText(getActivity(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
				return true;
			}
			
			if(mAdapter == null || mAdapter.playlist == null || mAdapter.playlist.size() == 0){
				Toast.makeText(getActivity().getBaseContext(), R.string.no_song_found, Toast.LENGTH_SHORT).show();			
				return true;
			}
			ExportDialogFragment exportDiag = ExportDialogFragment.newInstance(mAdapter.playlist);
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			exportDiag.show(ft, "export-dialog");
			return true;
		} else if (item.getItemId() == R.id.menu_search_artist) {
			if(isAnyTaskRunning()){
				Toast.makeText(getActivity(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
				return true;
			}
			
			InputDialogFragment newFragment = InputDialogFragment.newInstance(R.string.artist_name, DIALOG_ARTIST_NAME);
			newFragment.showDialog(getActivity());
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}		
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {	
		super.onCreateContextMenu(menu, v, menuInfo);
		
		menu.add(ContextMenu.NONE, MENU_REMOVE_SONG, ContextMenu.NONE, R.string.remove_song);
	}
	
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		switch (item.getItemId()) {
		case MENU_REMOVE_SONG:
			RecSongsPlaylist.getInstance().removeSongFromPlaylist(info.position);
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}	
	
	public class SongsAdapter extends BaseAdapter {

		private ArrayList<SongInfo> playlist;
		private LayoutInflater inflater;

		public SongsAdapter() {    
			playlist = null;
			inflater = getActivity().getLayoutInflater();
		}

		public int getCount() {
			if(playlist == null)
				return 0;

			return playlist.size();
		}

		public SongInfo getItem(int position) {
			return playlist.get(position);
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
			if (song != null) {

				holder.botText.setText(song.artist.name);
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

				//load coverart image
				ImageLoader.getLoader().DisplayImage(song.release.image, holder.image, R.drawable.ic_disc_stub, ImageSize.SMALL);
			}

			return convertView;
		}

		public void setPlaylist(ArrayList<SongInfo> pl){
			this.playlist = pl;
			notifyDataSetChanged();
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
		SongInfo song = mAdapter.getItem(position);
		
		if(isAnyTaskRunning()){
			Toast.makeText(getActivity(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
			return;
		}		
		
		//check if we have enough info of the song to show the tabs
		if(song.buyUrl == null || song.artist.buyUrl == null || song.artist.id == null ||
			song.release.artist == null || song.release.buyUrl == null || song.release.id == null || 
			song.release.image == null || song.release.name == null){

			mSongDetailsTask = (SongDetailsTask) new SongDetailsTask(song).execute();
			return;
		}

		Intent i = new Intent(getActivity(), MusicInfoActivity.class);		
		i.putExtra(SongInfoFragment.BUNDLE_SONG, song); 		
		startActivity(i);		
	}
	
	/** Fetches the full SongInfo from 7digital and call the MusicInfoActivity*/
	private class SongDetailsTask extends AsyncTask<Void, Void, SongInfo>{
		String err = null;
		SongInfo mSelectedSong;
		private View mProgressOverlay;
		
		public SongDetailsTask(SongInfo s) {
			mSelectedSong = s;
		}
		
		@Override
		protected void onPreExecute() {
			// see if we already inflated the progress overlay
            mProgressOverlay = Util.setProgressShown(SongsFragment.this, true);
            
            // setup the progress overlay
            ((ProgressBar) mProgressOverlay.findViewById(R.id.ProgressBarShowListDet)).setIndeterminate(true);
            
            TextView mUpdateStatus = (TextView) mProgressOverlay
                    .findViewById(R.id.textViewUpdateStatus);
            mUpdateStatus.setText(R.string.loading);

            View cancelButton = mProgressOverlay.findViewById(R.id.overlayCancel);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onCancelTasks();
                }
            });
		}

		@Override
		protected SongInfo doInBackground(Void... args) {						
			SongInfo song;
			
			if(isCancelled())
				return null;
			
			try{
				song = SevenDigitalComm.getComm().querySongDetails(mSelectedSong.id, 
						mSelectedSong.name, mSelectedSong.artist.name);				
			}catch(ServiceCommException e){
				err = e.getMessage(getActivity());		
				return null;
			}

			return song;
		}

		@Override
		protected void onPostExecute(SongInfo song) {
			Util.setProgressShown(SongsFragment.this, false);
			
			if(err != null){
				Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();				
				return;
			}
	
			Intent i = new Intent(getActivity(), MusicInfoActivity.class);		
			i.putExtra("song", song); 		
			startActivity(i);
		}
		
        @Override
        protected void onCancelled() {
        	Util.setProgressShown(SongsFragment.this, false);
        }
	}	
	
	/** Gets a new playlist from Echo Nest*/
	public void getNewPlaylist(String artistName) {
		
		//cancel if the profile is empty
		if(artistName == null && UserProfile.getInstance(getActivity()).isEmpty()){
			Toast.makeText(getActivity().getApplicationContext(), R.string.req_add_artists_profile, Toast.LENGTH_LONG).show();
			
			//TODO: HARDCODED the profile tab index here, need to update when its index change
			try{
				((SherlockFragmentActivity)getActivity()).getSupportActionBar().setSelectedNavigationItem(ProfileFragment.TAB_ID);
			}catch(IndexOutOfBoundsException e){
				Log.d(Util.APP, ">>> Hey Dev, update the goddamn Profile tab index!<<<");
			}
			return;
		}
		
		//cancel if the task is already running
		if(RecSongsPlaylist.getInstance().isTaskRunning()){
			Toast.makeText(getActivity().getApplicationContext(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
			return;
		}
		
		//cancel the media player
		MediaPlayerController.getCon().release();
		
		//clears the playlist				
		RecSongsPlaylist.getInstance().clearPlaylist();
		
		//get the playlist
		if(artistName == null){
			RecSongsPlaylist.getInstance()
				.getNewPlaylist(buildPlaylistParams(UserProfile.getInstance(getActivity()).getRandomArtists(5)), 
													SongsFragment.this);
		}else{
			ArtistInfo artist = new ArtistInfo();
			artist.name = artistName;
			ArrayList<ArtistInfo> artists = new ArrayList<ArtistInfo>();
			artists.add(artist);
			RecSongsPlaylist.getInstance().getNewPlaylist(buildPlaylistParams(artists),SongsFragment.this);
		}		
	}	
	
	private PlaylistParams buildPlaylistParams(ArrayList<ArtistInfo> artists){
		PlaylistParams plp = new PlaylistParams();

		if(PlaylistOptions.getInstance(getActivity()).getSettings().isSimilar)
			plp.setType(PlaylistType.ARTIST_RADIO);
		else
			plp.setType(PlaylistType.ARTIST);

		plp.setResults(PlaylistOptions.getInstance().getMaxResults());	    
		plp.addIDSpace(EchoNestComm.SEVEN_DIGITAL);
		plp.includeTracks();
		plp.setLimit(true);

		if(PlaylistOptions.getInstance().getVariety() != -1.0f)
			plp.setVariety(PlaylistOptions.getInstance().getVariety());

		if(PlaylistOptions.getInstance().getMinEnergy() != -1.0f){
			plp.setMinEnergy(PlaylistOptions.getInstance().getMinEnergy());
			plp.setMaxEnergy(PlaylistOptions.getInstance().getMaxEnergy());	    	
		}

		if(PlaylistOptions.getInstance().getMinDanceability() != -1.0f){
			plp.setMinDanceability(PlaylistOptions.getInstance().getMinDanceability());
			plp.setMaxDanceability(PlaylistOptions.getInstance().getMaxDanceability());
		}

		if(PlaylistOptions.getInstance().getMinTempo() != -1.0f){
			plp.setMinTempo(PlaylistOptions.getInstance().getMinTempo());
			plp.setMaxTempo(PlaylistOptions.getInstance().getMaxTempo());	
		}

		if(PlaylistOptions.getInstance().getMinHotness() != -1.0f){
			plp.setSongMinHotttnesss(PlaylistOptions.getInstance().getMinHotness());
			plp.setSongtMaxHotttnesss(PlaylistOptions.getInstance().getMaxHotness());
		}

		if(PlaylistOptions.getInstance().getSettings().isSimilar){
			List<String> moods = PlaylistOptions.getInstance().getMood();
			if(moods != null){
				for(String mood : moods){
					plp.add("mood", mood);
				}
				moods = null;
			}
		}
	    	    
	    for(ArtistInfo artist : artists){
	    	plp.addArtist(artist.name);	
	    }

		return plp;
	}

	@Override
	//updates the playlist with the fetched songs
	public void onDataChanged(ArrayList<SongInfo> playlist) {
		if(mAdapter != null)
			mAdapter.setPlaylist(playlist);
	}

	/** play the playlist into Dood's Music*/
	private void playSongsIntoDoodsMusic(){

		if(isAnyTaskRunning()){
			Toast.makeText(getActivity(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
			return;
		}
		
		if(mAdapter == null || mAdapter.playlist == null || mAdapter.playlist.size() == 0){
			Toast.makeText(getActivity().getBaseContext(), R.string.no_song_found, Toast.LENGTH_SHORT).show();			
			return;
		}

		//check if DMS is installed on the device
		if(!Util.isAppInstalled("com.mysticdeath.md_gs_app", getActivity())){
			Toast.makeText(getActivity().getApplicationContext(), R.string.install_dms, Toast.LENGTH_LONG).show();

			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.mysticdeath.md_gs_app"));
			intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return;
		}

		//start task to fetch song ids from grooveshark
		if(mPlaySongsTask != null)
			mPlaySongsTask.cancel(true);
		mPlaySongsTask = (PlaySongsTask) new PlaySongsTask(PlaySongsTask.DMS_PLAYER).execute();
	}
	
	private void playSongsIntoGrooveshark(){
		
		if(isAnyTaskRunning()){
			Toast.makeText(getActivity(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
			return;
		}
		
		if(mAdapter == null || mAdapter.playlist == null || mAdapter.playlist.size() == 0){
			Toast.makeText(getActivity().getBaseContext(), R.string.no_song_found, Toast.LENGTH_SHORT).show();			
			return;
		}
		
		//start task to fetch song ids from grooveshark
		if(mPlaySongsTask != null)
			mPlaySongsTask.cancel(true);
		mPlaySongsTask = (PlaySongsTask) new PlaySongsTask(PlaySongsTask.GROOVESHARK_PLAYER).execute();
	}

	private class PlaySongsTask extends AsyncTask<Void, Integer, int[]>{

		private String err;
		
		private int mPlayer;	//0 -> DMS, 1-> Grooveshark		
		public static final int DMS_PLAYER = 0;
		public static final int GROOVESHARK_PLAYER = 1;
		
		private List<SongInfo> mSongs = new ArrayList<SongInfo>(mAdapter.playlist);
		private View mProgressOverlay;
		private ProgressBar mUpdateProgress;
		
		private ArrayList<String> mSongIds = new ArrayList<String>();
		final AtomicInteger mFetchCount = new AtomicInteger();
		
        protected PlaySongsTask(int player) {
        	mPlayer = player;
        }
		
        protected PlaySongsTask(ArrayList<String> ids, int i, int player) {
        	mSongIds = ids;
        	mFetchCount.set(i);
        	mPlayer = player;
        }
		
		@Override
		protected void onPreExecute() {

            // see if we already inflated the progress overlay
            mProgressOverlay = Util.setProgressShown(SongsFragment.this, true);
            
            // setup the progress overlay
            TextView mUpdateStatus = (TextView) mProgressOverlay
                    .findViewById(R.id.textViewUpdateStatus);
            mUpdateStatus.setText(R.string.loading);

            mUpdateProgress = (ProgressBar) mProgressOverlay
                    .findViewById(R.id.ProgressBarShowListDet);

            View cancelButton = mProgressOverlay.findViewById(R.id.overlayCancel);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onCancelTasks();
                }
            });
			
            //show dms warning only once
			if(mFetchCount.get() == 0){
				if(mPlayer == DMS_PLAYER)
					Toast.makeText(getActivity().getBaseContext(), R.string.check_dms_setting_on, Toast.LENGTH_LONG).show();
				else
					Toast.makeText(getActivity().getBaseContext(), R.string.requires_flash, Toast.LENGTH_LONG).show();
			}

			//access is limited today to some ws calls/ip/minute, so i'll need to truncate the playlist 
			if(mSongs.size() > GroovesharkComm.RATE_LIMIT){
				mSongs = mSongs.subList(0, GroovesharkComm.RATE_LIMIT);				
			}

			mUpdateProgress.setIndeterminate(false);
			mUpdateProgress.setMax(mSongs.size());
			mUpdateProgress.setProgress(mFetchCount.get());
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {			
			mUpdateProgress.setProgress(progress[0]);
		}		

		@Override
		protected int[] doInBackground(Void... params) {			
			int i = 0;
			int ids[];

			//fetch the song ids from grooveshark
			final AtomicInteger fetchCount = mFetchCount;
			for(i=fetchCount.get(); i<mSongs.size(); i++){
				SongInfo song = mSongs.get(i);
				
				//check if the task was cancelled by the user
				if(isCancelled()){
					return null;
				}

				try {					
					String gsID = GroovesharkComm.getComm().getSongID(song.name, song.artist.name, getActivity());					
					mSongIds.add(gsID);
				}catch (ServiceCommException e) {
					if(e.getErr() == ServiceErr.SONG_NOT_FOUND){
						publishProgress(fetchCount.incrementAndGet());
						Log.i(Util.APP, "Song ["+song.name+" - "+song.artist.name+"] not found in Grooveshark, ignoring...");
						continue;
					}
					
					mSongs = null;
					
					//in case of error, return what song ids we have
					i = 0;
					ids = new int[mSongIds.size()];
					for(String id : mSongIds){
						ids[i++] = Integer.parseInt(id);
					}					

					err = e.getMessage(getActivity());
					return ids;
				} 

				publishProgress(fetchCount.incrementAndGet());
			}
			mSongs = null;
			
			//check if the task was cancelled by the user
			if(isCancelled()){
				return null;
			}

			//convert from string list to static array
			i = 0;
			ids = new int[mSongIds.size()];
			for(String id : mSongIds){
				ids[i++] = Integer.parseInt(id);
			}

			return ids;
		}

		@Override
		protected void onPostExecute(int[] ids) {

			Util.setProgressShown(SongsFragment.this, false);
			
			if(err != null){
				Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();
			}

			if(ids == null || ids.length == 0){
				Toast.makeText(getActivity(), R.string.no_song_found, Toast.LENGTH_SHORT).show();
				return;
			}
			
			if(mPlayer == DMS_PLAYER){			
				Intent intent = new Intent();
				intent.setAction("com.mysticdeath.md_gs_app.remotelist");
				intent.putExtra("name", Util.APP);
				intent.putExtra("songIDs", ids);
				getActivity().sendBroadcast(intent);
			}else{
				//build url with fetched song ids
				StringBuilder sb = new StringBuilder();				
				sb.append(GroovesharkComm.WIDGET_URL);			
				for(int id : ids){
					sb.append(id+",");
				}
				sb.deleteCharAt(sb.length()-1);	
				
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sb.toString()));
				startActivity(intent);
			}
		}
		
        @Override
        protected void onCancelled() {
        	Util.setProgressShown(SongsFragment.this, false);
        }
	}
    
    private void onCancelTasks() {
    	if(mPlaySongsTask != null && mPlaySongsTask.getStatus() != AsyncTask.Status.FINISHED) {
        	mPlaySongsTask.cancel(true);
            mPlaySongsTask = null;
        }  
        
        if(mSongDetailsTask != null && mSongDetailsTask.getStatus() != AsyncTask.Status.FINISHED) {
        	mSongDetailsTask.cancel(true);
        	mSongDetailsTask = null;
        }
                
    }
    
    private boolean isAnyTaskRunning() {
        if(mPlaySongsTask != null && mPlaySongsTask.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        
        if(mSongDetailsTask != null && mSongDetailsTask.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        
        return false;
    }
	
	public static class ExportDialogFragment extends DialogFragment{
		
		private static final String PLAYLIST_TAG = "playlist";
		
		public static ExportDialogFragment newInstance(ArrayList<SongInfo> playlist){
			ExportDialogFragment diag = new ExportDialogFragment();
			Bundle args = new Bundle();
			args.putParcelableArrayList(PLAYLIST_TAG, playlist);
			diag.setArguments(args);
			return diag;
		}
		
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	    	final ArrayList<SongInfo> playlist = getArguments().getParcelableArrayList(PLAYLIST_TAG);
	    	
	    	View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_export, null, false);
	    	
	    	v.findViewById(R.id.export_to_grooveshark).setOnClickListener(new View.OnClickListener() {
	    		@Override
	    		public void onClick(View v) {
					Intent intent = new Intent(getActivity(), ExportPlaylistGroovesharkActivity.class);
					intent.putExtra(ExportPlaylistGroovesharkFragment.BUNDLE_PLAYLIST, playlist);
					startActivity(intent);	    			
	    			dismiss();	    			
	    		}
	    	});
	    	
	    	v.findViewById(R.id.export_to_youtube).setOnClickListener(new View.OnClickListener() {
	    		@Override
	    		public void onClick(View v) {
					Intent intent = new Intent(getActivity(), ExportPlaylistYouTubeActivity.class);
					intent.putExtra(ExportPlaylistYouTubeFragment.BUNDLE_PLAYLIST, playlist);
					startActivity(intent);	    			
	    			dismiss();
	    		}
	    	});  
	    	
	    	v.findViewById(R.id.export_to_rdio).setOnClickListener(new View.OnClickListener() {
	    		@Override
	    		public void onClick(View v) {
					Intent intent = new Intent(getActivity(), ExportPlaylistRdioActivity.class);
					intent.putExtra(ExportPlaylistRdioFragment.BUNDLE_PLAYLIST, playlist);
					startActivity(intent);
	    			dismiss();   
	    		}
	    	});
	    	
	    	v.findViewById(R.id.export_to_lastfm).setOnClickListener(new View.OnClickListener() {
	    		@Override
	    		public void onClick(View v) {
					Intent intent = new Intent(getActivity(), ExportPlaylistLastfmActivity.class);
					intent.putExtra(ExportPlaylistLastfmFragment.BUNDLE_PLAYLIST, playlist);
					startActivity(intent);	    			
	    			dismiss();	    	    					    			
	    		}
	    	});   	    	
	    	
	    	Dialog dialog = new Dialog(getActivity(), R.style.DialogThemeNoTitle);
	    	dialog.setContentView(v);
	    	return dialog;
	    }
	}
		
	public static class PlayerDialogFragment extends DialogFragment{
		
		public static PlayerDialogFragment newInstance(){
			PlayerDialogFragment diag = new PlayerDialogFragment();
			return diag;
		}
		
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	    		    	
	    	View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_player, null, false);
	    	
	    	v.findViewById(R.id.play_in_dms).setOnClickListener(new View.OnClickListener() {
	    		@Override
	    		public void onClick(View v) {
	    			int index = ((MainActivity)getActivity()).mViewPager.getCurrentItem();
	    			SongsFragment f = (SongsFragment) getFragmentManager().findFragmentByTag(
	                        "android:switcher:"+R.id.pager+":"+index); //that is the tag the ViewPager sets to the fragment
	    			
	    			f.playSongsIntoDoodsMusic();	    			
	    			dismiss();	    			
	    		}
	    	});
	    	
	    	v.findViewById(R.id.play_in_grooveshark).setOnClickListener(new View.OnClickListener() {
	    		@Override
	    		public void onClick(View v) {	    			
	    			int index = ((MainActivity)getActivity()).mViewPager.getCurrentItem();
	    			SongsFragment f = (SongsFragment) getFragmentManager().findFragmentByTag(
	                        "android:switcher:"+R.id.pager+":"+index); //that is the tag the ViewPager sets to the fragment
	    			
	    			f.playSongsIntoGrooveshark();	    				    			
	    			dismiss();
	    		}
	    	});  	    	
    	
	    	Dialog dialog = new Dialog(getActivity(), R.style.DialogThemeNoTitle);
	    	dialog.setContentView(v);
	    	return dialog;
	    }
	}	
	
	/*private void setLogoFooter() {		
		
		//set logo footer		
		LinearLayout footer = (LinearLayout)getActivity().getLayoutInflater().inflate(R.layout.logo, null);
		
		ImageView logo = (ImageView)footer.findViewById(R.id.logo);
		//logo.setImageResource(R.drawable.echonest_logo_pwd);
		
		logo.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Util.ECHONEST_URL));
				intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		});
		
		getListView().addFooterView(footer, null, false);		
	}*/
}
