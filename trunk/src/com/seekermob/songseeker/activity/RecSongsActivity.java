package com.seekermob.songseeker.activity;

import java.util.ArrayList;
import java.util.List;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.EchoNestComm;
import com.seekermob.songseeker.data.ArtistsParcel;
import com.seekermob.songseeker.data.IdsParcel;
import com.seekermob.songseeker.data.RecSongsPlaylist;
import com.seekermob.songseeker.data.RecSongsPlaylist.PlaylistListener;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.data.SongNamesParcel;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;
import com.seekermob.songseeker.util.MediaPlayerController;
import com.seekermob.songseeker.util.Settings;
import com.seekermob.songseeker.util.Util;
import com.seekermob.songseeker.util.MediaPlayerController.MediaStatus;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.PlaylistParams.PlaylistType;
import com.echonest.api.v4.Song;
import com.google.android.apps.analytics.easytracking.TrackedListActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
//import android.os.Debug;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class RecSongsActivity extends TrackedListActivity implements PlaylistListener {

	private final int PROGRESS_DIAG = 0;
	private final int EXPORT_DIAG = 1;
	
	private RecSongsAdapter adapter;	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
			
		super.onCreate(savedInstanceState);

		// Use a custom layout file
		setContentView(R.layout.listview);

		// Tell the list view which view to display when the list is empty
		getListView().setEmptyView(findViewById(R.id.empty));

		//set EN logo at the end of the listview
		setLogoFooter();
		
		// Set up our adapter
		adapter = new RecSongsAdapter();
		setListAdapter(adapter);
		
		//register a listener for playlist data changes
		RecSongsPlaylist.getInstance().registerListener(this);

		registerForContextMenu(getListView());    	

		//check if we have an empty playlist
		ArtistsParcel ss = getIntent().getExtras().getParcelable("searchSeed");
		if(ss == null || ss.getArtistList().size() == 0){

			if(RecSongsPlaylist.getInstance().isEmpty()){
				Toast.makeText(getApplicationContext(), "There is no songs in your playlist!", Toast.LENGTH_SHORT).show();
				finish();
			}else{
				RecSongsPlaylist.getInstance().setPlaylist(adapter);
			}
			return;
		}

		//get the playlist
		PlaylistParams plp = buildPlaylistParams();	    
		RecSongsPlaylist.getInstance().getPlaylist(plp, this, PROGRESS_DIAG);
	}

	@Override
	protected void onPause() {
		MediaPlayerController.getCon().release();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		//ImageLoader.getLoader(getCacheDir()).stopThread();
		//ImageLoader.getLoader(getCacheDir()).clearCache();
		
		//clears the data from the playlist
		RecSongsPlaylist.getInstance().clearPlaylist();
		
		//unregister the listener
		RecSongsPlaylist.getInstance().unregisterListener(this);
		
		super.onDestroy();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Song song = adapter.getItem(position);

		try {
			String foreignId = song.getString("tracks[0].foreign_id");
		
			SongInfo songInfo = new SongInfo();
			songInfo.id = foreignId.split(":")[2];
			songInfo.name = song.getReleaseName();
			songInfo.artist.name = song.getArtistName();
			songInfo.previewUrl = song.getString("tracks[0].preview_url");			
			
			Intent i = new Intent(RecSongsActivity.this, MusicInfoTab.class);
			i.putExtra("songId", songInfo); 
			startActivity(i);
		} catch (IndexOutOfBoundsException e) {
			Toast.makeText(this, "Unable to retrieve track details!", Toast.LENGTH_SHORT).show();
			return;
		} catch (Exception e){
			Toast.makeText(this, "Unable to retrieve track details!", Toast.LENGTH_SHORT).show();
			Log.e(Util.APP, e.getMessage(), e);
			return;
		}
	}
	
	@Override
    protected Dialog onCreateDialog(int id) {

		switch(id){
		case PROGRESS_DIAG:
		    ProgressDialog pd = new ProgressDialog(this);
		    pd.setMessage("Retrieving data from Echo Nest...");
		    pd.setIndeterminate(true);
		    pd.setCancelable(true);
		    return pd;
		case EXPORT_DIAG:		  	
			final CharSequence[] items = {"Rdio Playlist", "Last.fm Playlist", "YouTube Playlist",
											"Grooveshark Playlist"};

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Export as...");
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {			        
					
			    	IdsParcel songIds = new IdsParcel();
			    	SongNamesParcel songNames = new SongNamesParcel();
			    	ArtistsParcel songArtists = new ArtistsParcel();
			    	
					for(Song song : adapter.playlist){
						songIds.addId(song.getID());
						songNames.addName(song.getReleaseName());
						songArtists.addArtist(song.getArtistName());
					}
			    	
					Intent i;
					
					switch(item){
					case 0:
						i = new Intent(RecSongsActivity.this, CreatePlaylistRdioActivity.class);
						i.putExtra("songIds", songIds);
						break;
					case 1:
						i = new Intent(RecSongsActivity.this, CreatePlaylistLastfmActivity.class);
						i.putExtra("songNames", songNames);
						i.putExtra("songArtists", songArtists);
						break;
					case 2:
						i = new Intent(RecSongsActivity.this, CreatePlaylistYoutubeActivity.class);
						i.putExtra("songNames", songNames);
						i.putExtra("songArtists", songArtists);
						break;
					case 3:
						i = new Intent(RecSongsActivity.this, CreatePlaylistGroovesharkActivity.class);
						i.putExtra("songNames", songNames);
						i.putExtra("songArtists", songArtists);
						break;						
					default:
						return;							
					}		    	
			    	
			    	startActivity(i);
			    	
			    }
			});
			AlertDialog alert = builder.create();
			return alert;			
		default:
			return null;
		}
        
    }
	
	private PlaylistParams buildPlaylistParams(){
	    PlaylistParams plp;
	    
		plp = new PlaylistParams();
	    
		if(Settings.getInstance(getCacheDir()).getSettings().isSimilar)
			plp.setType(PlaylistType.ARTIST_RADIO);
		else
			plp.setType(PlaylistType.ARTIST);
		
	    plp.setResults(Settings.getInstance().getMaxResults());	    
	    plp.addIDSpace(EchoNestComm.SEVEN_DIGITAL);
	    plp.includeTracks();
	    plp.setLimit(true);
	    
	    if(Settings.getInstance().getVariety() != -1.0f)
	    	plp.setVariety(Settings.getInstance().getVariety());
	    
	    if(Settings.getInstance().getMinEnergy() != -1.0f){
		    plp.setMinEnergy(Settings.getInstance().getMinEnergy());
		    plp.setMaxEnergy(Settings.getInstance().getMaxEnergy());	    	
	    }
	    
	    if(Settings.getInstance().getMinDanceability() != -1.0f){
		    plp.setMinDanceability(Settings.getInstance().getMinDanceability());
		    plp.setMaxDanceability(Settings.getInstance().getMaxDanceability());
	    }
	    
	    if(Settings.getInstance().getMinTempo() != -1.0f){
		    plp.setMinTempo(Settings.getInstance().getMinTempo());
		    plp.setMaxTempo(Settings.getInstance().getMaxTempo());	
	    }

	    if(Settings.getInstance().getMinHotness() != -1.0f){
		    plp.setSongMinHotttnesss(Settings.getInstance().getMinHotness());
		    plp.setSongtMaxHotttnesss(Settings.getInstance().getMaxHotness());
	    }
	    
	    if(Settings.getInstance().getSettings().isSimilar){
	    	List<String> moods = Settings.getInstance().getMood();
		    if(moods != null){
			    for(String mood : moods){
			    	plp.add("mood", mood);
			    }
			    moods = null;
		    }
	    }
	    
	    ArtistsParcel ss = getIntent().getExtras().getParcelable("searchSeed");	    
	    for(String artist : ss.getArtistList()){
	    	plp.addArtist(artist);	
	    }
	    
	    return plp;
	}
	
	public class RecSongsAdapter extends BaseAdapter {
    
	    private ArrayList<Song> playlist;
		private LayoutInflater inflater;
	    
	    public RecSongsAdapter() {    
	    	playlist = null;
	    	inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    }
	
	    public int getCount() {
	        if(playlist == null)
	        	return 0;
	        
	    	return playlist.size();
	    }
	
	    public Song getItem(int position) {
	        return playlist.get(position);
	    }
	
	    public long getItemId(int position) {
	        return position;
	    }
	    
	    public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
	    	
			if (convertView == null) {
			   	convertView = inflater.inflate(R.layout.list_row, null);
				
				holder = new ViewHolder();
				holder.topText = (TextView) convertView.findViewById(R.id.firstLine);
			    holder.botText = (TextView) convertView.findViewById(R.id.secondLine);
			    holder.coverArt = (ImageView) convertView.findViewById(R.id.coverart);
			    holder.mediaBtns = (FrameLayout) convertView.findViewById(R.id.media_btns);
			    holder.playPause = (ImageView) convertView.findViewById(R.id.playpause);
			    holder.loading = (ProgressBar) convertView.findViewById(R.id.loading);			    
			    
			    convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			 
			final Song song = getItem(position);
			final int pos = position;
			if (song != null) {
			    
				holder.botText.setText(song.getArtistName());
				holder.topText.setText(song.getReleaseName());
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
			            	try{
			            		String previewUrl = song.getString("tracks[0].preview_url");
			            		MediaPlayerController.getCon().startStopMedia(previewUrl, pos, adapter);
			            		adapter.notifyDataSetChanged();
			            	}catch(IndexOutOfBoundsException e){
			            		Log.i(Util.APP, "Preview Url for song ["+song.getReleaseName()+" - "+song.getArtistName()+"] not found!", e);
			            	}		            	
			            }
			        }); 	
					
					break;
				case PLAYING:
				case STOPPED:
				default:
					holder.playPause.setOnClickListener(new View.OnClickListener() {
			            public void onClick(View v) {		            	
			            	try{
			            		String previewUrl = song.getString("tracks[0].preview_url");
			            		MediaPlayerController.getCon().startStopMedia(previewUrl, pos, adapter);
			            		adapter.notifyDataSetChanged();
			            	}catch(IndexOutOfBoundsException e){
			            		Log.i(Util.APP, "Preview Url for song ["+song.getReleaseName()+" - "+song.getArtistName()+"] not found!", e);
			            	}		            	
			            }
			        });
					
					break;
				}
			    
				//load coverart image
			    try{
			    	ImageLoader.getLoader(getCacheDir()).DisplayImage(song.getString("tracks[0].release_image"), holder.coverArt, R.drawable.ic_disc_stub, ImageSize.SMALL);
			    }catch(IndexOutOfBoundsException e){
			    	Log.i(Util.APP, "Unable to fetch the release image from Echo Nest!");
			    }
			}
			
			return convertView;
		}
	    
	    public void setPlaylist(ArrayList<Song> pl){
	    	this.playlist = pl;
	    	notifyDataSetChanged();
	    }	    
		
	    private class ViewHolder{
	    	public TextView topText;
	    	public TextView botText;
	    	public ImageView coverArt;
	    	public ImageView playPause;
	    	public ProgressBar loading;
	    	public FrameLayout mediaBtns;
	    }
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.recsongs_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int itemId = item.getItemId();
        
        if(itemId == R.id.play){
        	
        	if(adapter == null || adapter.playlist == null)
        		return false;
        	
        	Intent i = new Intent(RecSongsActivity.this, PlayPlaylistActivity.class);
	    	
        	SongNamesParcel songNames = new SongNamesParcel();
	    	ArtistsParcel songArtists = new ArtistsParcel();	    	
	    	
			for(Song song : adapter.playlist){
				songNames.addName(song.getReleaseName());
				songArtists.addArtist(song.getArtistName());
			}
			i.putExtra("songNames", songNames);
			i.putExtra("songArtists", songArtists);
			startActivity(i);
        	return true;
        }
        
        if(itemId == R.id.pl_options){
        	Intent i = new Intent(RecSongsActivity.this, PlaylistOptionsActivity.class);
            startActivity(i);	
            return true;
        }
        
        if(itemId == R.id.refresh){
    	    //get the playlist
        	RecSongsPlaylist.getInstance().clearPlaylist();
        	PlaylistParams plp = buildPlaylistParams();	    
    	    RecSongsPlaylist.getInstance().getPlaylist(plp, this, PROGRESS_DIAG);
            return true;	
        }
        
        if(itemId == R.id.export){
        	showDialog(EXPORT_DIAG);
        	return true;
        }
    	
        return super.onOptionsItemSelected(item);
    }
    
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.setHeaderTitle("Options");
		
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.recsong_contextmenu, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		if(item.getItemId() == R.id.remove_song) {
			RecSongsPlaylist.getInstance().removeSongFromPlaylist(info.position, this);
			return true;
		}
		
		return super.onContextItemSelected(item);		
	}
	
	private void setLogoFooter() {		
		
		//set logo footer
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout footer = (LinearLayout)inflater.inflate(R.layout.logo, null);
		
		ImageView logo = (ImageView)footer.findViewById(R.id.logo);
		logo.setImageResource(R.drawable.echonest_logo_pwd);
		
		logo.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Util.ECHONEST_URL));
				intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		});
		
		getListView().addFooterView(footer);		
	}

	@Override
	//updates the playlist with the fetched songs
	public void onDataChanged(ArrayList<Song> playlist) {
		if(adapter != null)
			adapter.setPlaylist(playlist);
	}
}


