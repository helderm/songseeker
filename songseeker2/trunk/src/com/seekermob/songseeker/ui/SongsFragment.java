package com.seekermob.songseeker.ui;

import java.util.ArrayList;
import java.util.List;


import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.PlaylistParams.PlaylistType;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.EchoNestComm;
import com.seekermob.songseeker.comm.GroovesharkComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.data.PlaylistOptions;
import com.seekermob.songseeker.data.RecSongsPlaylist;
import com.seekermob.songseeker.data.RecSongsPlaylist.PlaylistListener;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.Util;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;
import com.seekermob.songseeker.util.MediaPlayerController;
import com.seekermob.songseeker.util.MediaPlayerController.MediaStatus;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SongsFragment extends SherlockListFragment implements PlaylistListener, OnCancelListener {
	private SongsAdapter mAdapter;
	private AsyncTask<?,?,?> task;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		//populate the optionsMenu 
		setHasOptionsMenu(true);

		//set adapter
		mAdapter = new SongsAdapter();
		setListAdapter(mAdapter);

		//register a listener for playlist data changes
		RecSongsPlaylist.getInstance().registerListener(this);

		//check if we are recovering the state
		ArrayList<SongInfo> savedPlaylist = null;
		if(savedInstanceState != null && (savedPlaylist = savedInstanceState.getParcelableArrayList("savedPlaylist")) != null){
			if(RecSongsPlaylist.getInstance().isEmpty()){
				RecSongsPlaylist.getInstance().setSongs(savedPlaylist);
			}
			savedPlaylist = null;

			mAdapter.setPlaylist(RecSongsPlaylist.getInstance().getPlaylist());
			return;
		}

		//set main onClick on emptyView that fetches data from EN
		setEmptyText(getString(R.string.tap_to_get_songs));	    
		getListView().getEmptyView().setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {		            	

				//if(UserProfile.getInstance(getActivity()).isEmpty()){
				//	Toast.makeText(getActivity().getApplicationContext(), R.string.req_add_artists_profile, Toast.LENGTH_LONG).show();
				//}

				//get the playlist
				RecSongsPlaylist.getInstance().getPlaylist(buildPlaylistParams(), SongsFragment.this);
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if(mAdapter != null && mAdapter.playlist != null){
			outState.putParcelableArrayList("savedPlaylist", new ArrayList<Parcelable>(mAdapter.playlist));			
		}

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroy() {

		//clears the data from the playlist
		RecSongsPlaylist.getInstance().clearPlaylist();

		//unregister the listener
		RecSongsPlaylist.getInstance().unregisterListener(this);

		//cancel the task
		if(task != null)
			task.cancel(true);
		
		super.onDestroy();
	}	

	@Override
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.songs_menu, menu);		
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_play_songs:
			playSongsIntoDoodsMusic();            	
			return true;
		case R.id.menu_save_playlist:
			return true;
		case R.id.menu_refresh_songs:
			//if(UserProfile.getInstance(getActivity()).isEmpty()){
			//	Toast.makeText(getActivity().getApplicationContext(), R.string.req_add_artists_profile, Toast.LENGTH_LONG).show();
			//}

			//cancel the media player
			MediaPlayerController.getCon().release();
			
			//get the playlist
			RecSongsPlaylist.getInstance().clearPlaylist();
			RecSongsPlaylist.getInstance().getPlaylist(buildPlaylistParams(), SongsFragment.this);
			return true;
		case R.id.menu_playlist_options:
        	Intent i = new Intent(getActivity(), PlaylistOptionsActivity.class);
            startActivity(i);	
			return true;
		case R.id.menu_export_playlist:
			return true;
		case R.id.menu_search_artist:
			return true;
		case R.id.menu_settings:
			return true;
		case R.id.menu_about:
			return true;
		case R.id.menu_donate:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}		
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
				convertView = inflater.inflate(R.layout.list_item_2_image, null);

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

	private PlaylistParams buildPlaylistParams(){
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

		/* TODO: FIX! ArrayList<ArtistInfo> artists = UserProfile.getInstance(getActivity()).getRandomArtists(5);	    	    
	    for(ArtistInfo artist : artists){
	    	plp.addArtist(artist.name);	
	    }*/
		plp.addArtist("Eric Clapton");

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
		if(task != null)
			task.cancel(true);
		task = new GetSongIdsGroovesharkTask().execute();
	}

	private class GetSongIdsGroovesharkTask extends AsyncTask<Void, Integer, int[]>{

		private String err;
		private DialogFragment dialogFrag;
		private List<SongInfo> songs = new ArrayList<SongInfo>(mAdapter.playlist);

		@Override
		protected void onPreExecute() {

			Toast.makeText(getActivity().getBaseContext(), R.string.check_dms_setting_on, Toast.LENGTH_LONG).show();

			//access is limited today to some ws calls/ip/minute, so i'll need to truncate the playlist 
			if(songs.size() > GroovesharkComm.RATE_LIMIT){

				songs = songs.subList(0, GroovesharkComm.RATE_LIMIT);
				//Toast.makeText(getActivity(), "Truncating playlist to " + GroovesharkComm.RATE_LIMIT + " songs, due to technical reasons...", Toast.LENGTH_SHORT).show();
			}

			dialogFrag = ProgressDialogFragment.showDialog(R.string.loading, songs.size(), getFragmentManager());			
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {			
			if(dialogFrag.getDialog() == null)
				return;
			
			((ProgressDialog)dialogFrag.getDialog()).setProgress(progress[0]);			
			dialogFrag.getDialog().setOnCancelListener(SongsFragment.this);
		}		

		@Override
		protected int[] doInBackground(Void... params) {
			SharedPreferences settings = getActivity().getSharedPreferences(Util.APP, Context.MODE_PRIVATE);
			ArrayList<String> ids = new ArrayList<String>();
			int songIds[] = null;
			int i = 0;			
			int count = 0;

			//fetch the song ids from grooveshark
			for(SongInfo song : songs){

				//check if the task was cancelled by the user
				if(Thread.interrupted()){
					return null;
				}

				try {					
					String gsID = GroovesharkComm.getComm().getSongID(song.name, song.artist.name, settings);					
					ids.add(gsID);
				}catch (ServiceCommException e) {
					if(e.getErr() == ServiceErr.SONG_NOT_FOUND){
						publishProgress(++count);
						Log.i(Util.APP, "Song ["+song.name+" - "+song.artist.name+"] not found in Grooveshark, ignoring...");
						continue;
					}

					songIds = new int[ids.size()];
					for(String id : ids){
						songIds[i++] = Integer.parseInt(id);
					}

					if(e.getErr() == ServiceErr.TRY_LATER){
						//err = "Some songs were not fetched due to technical reasons, try later!";
						return songIds;
					}

					err = e.getMessage();
					return songIds;
				} 

				publishProgress(++count);
			}

			//check if the task was cancelled by the user
			if(Thread.interrupted()){
				return null;
			}

			//convert from string list to static array
			songIds = new int[ids.size()];
			for(String id : ids){
				songIds[i++] = Integer.parseInt(id);
			}

			return songIds;
		}

		@Override
		protected void onPostExecute(int songIds[]) {

			dialogFrag.dismiss();
			
			if(err != null){
				Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();
			}

			if(songIds == null || songIds.length == 0){
				Toast.makeText(getActivity(), R.string.no_song_found, Toast.LENGTH_SHORT).show();
				return;
			}

			Intent intent = new Intent();
			intent.setAction("com.mysticdeath.md_gs_app.remotelist");
			intent.putExtra("name", Util.APP);
			intent.putExtra("songIDs", songIds);
			getActivity().sendBroadcast(intent);			
		}		
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		if(task != null)
			task.cancel(true);		
	}

}
