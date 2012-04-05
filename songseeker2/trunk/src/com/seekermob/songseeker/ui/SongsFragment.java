package com.seekermob.songseeker.ui;

import java.util.ArrayList;
import java.util.List;


import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.PlaylistParams.PlaylistType;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.EchoNestComm;
import com.seekermob.songseeker.data.PlaylistOptions;
import com.seekermob.songseeker.data.RecSongsPlaylist;
import com.seekermob.songseeker.data.RecSongsPlaylist.PlaylistListener;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;
import com.seekermob.songseeker.util.MediaPlayerController;
import com.seekermob.songseeker.util.MediaPlayerController.MediaStatus;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SongsFragment extends SherlockListFragment implements PlaylistListener {
	private SongsAdapter mAdapter;

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
				RecSongsPlaylist.getInstance().getPlaylist(buildPlaylistParams(), getActivity().getApplicationContext(), 0);
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {

		if(mAdapter != null && mAdapter.playlist != null){
			outState.putParcelableArrayList("savedPlaylist", new ArrayList<Parcelable>(mAdapter.playlist));	//TODO: Check if this leaks mem!		
		}

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroy() {

		//clears the data from the playlist
		RecSongsPlaylist.getInstance().clearPlaylist();

		//unregister the listener
		RecSongsPlaylist.getInstance().unregisterListener(this);

		super.onDestroy();
	}	

	@Override
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.songs_menu, menu);		
		super.onCreateOptionsMenu(menu, inflater);
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
}
