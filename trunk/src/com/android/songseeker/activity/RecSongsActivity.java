package com.android.songseeker.activity;

import java.util.List;
import com.android.songseeker.R;
import com.android.songseeker.comm.EchoNestComm;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.data.ArtistsParcel;
import com.android.songseeker.data.IdsParcel;
import com.android.songseeker.data.SongNamesParcel;
import com.android.songseeker.util.MediaPlayerController;
import com.android.songseeker.util.Settings;
import com.android.songseeker.util.Util;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.PlaylistParams.PlaylistType;
import com.echonest.api.v4.Song;
import com.android.songseeker.util.ImageLoader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.os.Debug;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class RecSongsActivity extends ListActivity {

	private final int PROGRESS_DIAG = 0;
	private final int EXPORT_DIAG = 1;
	
	private RecSongsAdapter adapter;
	private Toast toast;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
			
		super.onCreate(savedInstanceState);
	    
        // Use a custom layout file
        setContentView(R.layout.listview);
        
        // Tell the list view which view to display when the list is empty
        getListView().setEmptyView(findViewById(R.id.empty));
        
        // Set up our adapter
        adapter = new RecSongsAdapter();
        setListAdapter(adapter);
        
	    //get the playlist
	    PlaylistParams plp = buildPlaylistParams();	    
	    new GetPlaylistTask().execute(plp, null, null);

	    //Debug.startMethodTracing("myapp");
	}
	
	@Override
	protected void onPause() {
		MediaPlayerController.getCon().release();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		//TODO decomment, leave it there to save bandwidht
		//ImageLoader.getLoader(getCacheDir()).stopThread();
		//ImageLoader.getLoader(getCacheDir()).clearCache();
		
		
		//Debug.stopMethodTracing();
		super.onDestroy();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Song song = adapter.getItem(position);

		try {
			String foreignId = song.getString("tracks[0].foreign_id");
			String previewURL = song.getString("tracks[0].preview_url");
			
	    	IdsParcel songIds = new IdsParcel();
	    	SongNamesParcel songNames = new SongNamesParcel();
	    	ArtistsParcel songArtists = new ArtistsParcel();    	
			
	    	songIds.addId(foreignId.split(":")[2]);
	    	songNames.addName(song.getReleaseName());
	    	songArtists.addArtist(song.getArtistName());
		
	    	//add other info from song into the same parcel
	    	songNames.addName(previewURL);
	    	
			Intent i = new Intent(RecSongsActivity.this, SongInfoActivity.class);
			i.putExtra("songId", songIds); 
			i.putExtra("songName", songNames);
			i.putExtra("songArtist", songArtists);  
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
			final CharSequence[] items = {"Rdio Playlist", "Last.fm Playlist", "Youtube Playlist"};

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Export as...");
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {			        
					
			    	IdsParcel songIds = new IdsParcel();
			    	SongNamesParcel songNames = new SongNamesParcel();
			    	ArtistsParcel songArtists = new ArtistsParcel();
			    	
					for(Song song : adapter.playlist.getSongs()){
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
	    
	    plp.setType(PlaylistType.ARTIST_RADIO);
	    plp.setResults(Settings.getMaxResults());	    
	    plp.addIDSpace(EchoNestComm.SEVEN_DIGITAL);
	    //plp.addIDSpace(EchoNestComm.RDIO);
	    //plp.addIDSpace("playme");
	    //plp.add("bucket", "rdio");
	    plp.includeTracks();
	    plp.setLimit(true);
	    
	    if(Settings.getVariety() != -1.0f)
	    	plp.setVariety(Settings.getVariety());
	    
	    if(Settings.getMinEnergy() != -1.0f){
		    plp.setMinEnergy(Settings.getMinEnergy());
		    plp.setMaxEnergy(Settings.getMaxEnergy());	    	
	    }
	    
	    if(Settings.getMinDanceability() != -1.0f){
		    plp.setMinDanceability(Settings.getMinDanceability());
		    plp.setMaxDanceability(Settings.getMaxDanceability());
	    }
	    
	    if(Settings.getMinTempo() != -1.0f){
		    plp.setMinTempo(Settings.getMinTempo());
		    plp.setMaxTempo(Settings.getMaxTempo());	
	    }

	    if(Settings.getMinHotness() != -1.0f){
		    plp.setSongMinHotttnesss(Settings.getMinHotness());
		    plp.setSongtMaxHotttnesss(Settings.getMaxHotness());
	    }
	    
	    List<String> moods = Settings.getMood();
	    if(moods != null){
		    for(String mood : moods){
		    	plp.add("mood", mood);
		    }
		    moods = null;
	    }
	    
	    ArtistsParcel ss = getIntent().getExtras().getParcelable("searchSeed");	    
	    for(String artist : ss.getArtistList()){
	    	plp.addArtist(artist);	
	    }
	    
	    return plp;
	}
	
	private class RecSongsAdapter extends BaseAdapter {
	
	    private Playlist playlist;	    
	    private LayoutInflater inflater;
	    
	    public RecSongsAdapter() {    
	    	playlist = null;
	    	inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    }
	
	    public int getCount() {
	        if(playlist == null)
	        	return 0;
	        
	    	return playlist.getSongs().size();
	    }
	
	    public Song getItem(int position) {
	        return playlist.getSongs().get(position);
	    }
	
	    public long getItemId(int position) {
	        return position;
	    }
	    
	    public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
	    	
			if (convertView == null) {
			   	convertView = inflater.inflate(R.layout.rec_song_row, null);
				
				holder = new ViewHolder();
				holder.topText = (TextView) convertView.findViewById(R.id.recsong_firstLine);
			    holder.botText = (TextView) convertView.findViewById(R.id.recsong_secondLine);
			    holder.coverArt = (ImageView) convertView.findViewById(R.id.recsong_coverart);
			    holder.playPause = (ImageView) convertView.findViewById(R.id.recsong_playpause);	
			    
			    convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			 
			final Song song = getItem(position);
			final int pos = position;
			if (song != null) {
			    
				holder.botText.setText(song.getArtistName());
				holder.topText.setText(song.getReleaseName());

				switch(MediaPlayerController.getCon().getStatus(pos)){				
				case PLAYING:
					holder.playPause.setImageResource(R.drawable.pause);
					break;
				case LOADING:
				case PREPARED:
					holder.playPause.setImageResource(R.drawable.icon);
					break;				
				case STOPPED:
				default:
					holder.playPause.setImageResource(R.drawable.play);
					break;
					
				}
				
				holder.playPause.setOnClickListener(new View.OnClickListener() {
		            public void onClick(View v) {		            	
		            	try{
		            		String previewUrl = song.getString("tracks[0].preview_url");
		            		MediaPlayerController.getCon().startStopMedia(previewUrl, pos, adapter);
		            		adapter.notifyDataSetChanged();
		            	}catch(IndexOutOfBoundsException e){
		            		Log.w(Util.APP, "Preview Url for song ["+song.getReleaseName()+" - "+song.getArtistName()+"] not found!", e);
		            	}		            	
		            }
		        }); 			    
			    
			    try{
			    	ImageLoader.getLoader(getCacheDir()).DisplayImage(song.getString("tracks[0].release_image"), holder.coverArt, R.drawable.blankdisc);
			    }catch(IndexOutOfBoundsException e){
			    	Log.w(Util.APP, "Unable to fetch the release image from Echo Nest!");
			    }
			}
			
			return convertView;
		}
	    
	    public void setPlaylist(Playlist pl){
	    	this.playlist = pl;
	    	notifyDataSetChanged();
	    }
	    
		
	    private class ViewHolder{
	    	public TextView topText;
	    	public TextView botText;
	    	public ImageView coverArt;
	    	public ImageView playPause;
	    }
	}
	
	private class GetPlaylistTask extends AsyncTask<PlaylistParams, Void, Playlist>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			showDialog(PROGRESS_DIAG);
		}
		
		@Override
		protected Playlist doInBackground(PlaylistParams... plp) {
			Playlist pl = null;
		
			try {
				pl = EchoNestComm.getComm().createStaticPlaylist(plp[0]);
			} catch (ServiceCommException e) {
				err = e.getMessage();
				return null;
			}
			
			return pl;
		}
		
		@Override
		protected void onPostExecute(Playlist result) {
						
			//dismissDialog(PROGRESS_DIAG);
			removeDialog(PROGRESS_DIAG);
			
			if(err != null){
				toast = Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG);
	    		toast.show();        		
	    		
	    		RecSongsActivity.this.finish();
	    		return;
    		}
			
			adapter.setPlaylist(result);			
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
        switch (item.getItemId()) {
        case R.id.settings:
        	//create settings activity
        	return true;
        case R.id.pl_options:
        	Intent i = new Intent(RecSongsActivity.this, PlaylistOptionsActivity.class);
            startActivity(i);	
            return true;
        case R.id.refresh:
    	    //get the playlist
    	    PlaylistParams plp = buildPlaylistParams();	    
    	    new GetPlaylistTask().execute(plp, null, null);
            return true;
        case R.id.export:
        	showDialog(EXPORT_DIAG);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}


