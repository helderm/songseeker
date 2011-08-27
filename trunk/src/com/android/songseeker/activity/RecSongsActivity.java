package com.android.songseeker.activity;

import java.util.List;
import com.android.songseeker.R;
import com.android.songseeker.comm.EchoNestComm;
import com.android.songseeker.comm.RdioComm;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.data.SearchSeed;
import com.android.songseeker.data.SongList;
import com.android.songseeker.util.MediaPlayerController;
import com.android.songseeker.util.Settings;
import com.android.songseeker.util.Util;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.PlaylistParams.PlaylistType;
import com.echonest.api.v4.Song;
import com.echonest.api.v4.Track;
import com.android.songseeker.util.ImageLoader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
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
	private final int EXPORT_DIALOG = 1;
	
	private RecSongsAdapter adapter;
		
	private StartMediaPlayerTask mp_task = new StartMediaPlayerTask();
	private Toast toast;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
			
		super.onCreate(savedInstanceState);
	    
        // Use a custom layout file
        setContentView(R.layout.rec_songs_list);
        
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
	protected void onResume() {        
        super.onResume();
	}
	
	@Override
	protected void onPause() {
		MediaPlayerController.getCon().release();
		adapter.setNowPlaying(RecSongsAdapter.NOT_PLAYING);
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		adapter.imageLoader.stopThread();
		adapter.imageLoader.clearCache();
		//dismissDialog(PROGRESS_DIAG);
		
		//Debug.stopMethodTracing();
		super.onDestroy();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Song song = adapter.getItem(position);
		
		if(song == null || 
			(MediaPlayerController.getCon().isPlaying() && adapter.isPlaying(position))){
			
			MediaPlayerController.getCon().stop();
			adapter.setNowPlaying(RecSongsAdapter.NOT_PLAYING);
			return;
		}
		
		MediaPlayerController.getCon().stop();
		
		mp_task.cancel(true);
		mp_task = new StartMediaPlayerTask();
		mp_task.execute(song);
		adapter.setNowPlaying(position);

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
		case EXPORT_DIALOG:		  	
			final CharSequence[] items = {"Rdio Playlist"};

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Export as...");
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {			        
					
			    	SongList songList = new SongList();
			    	
					for(Song song : adapter.playlist.getSongs()){
						songList.addSongID(song.getID());
					}
			    	
			    	Intent i = new Intent(RecSongsActivity.this, RdioComm.class);
			    	i.putExtra("songList", songList);
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
	    //plp.addIDSpace("playme");
	    //plp.add("bucket", EchoNestComm.RDIO);
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
	    
	    SearchSeed ss = getIntent().getExtras().getParcelable("searchSeed");	    
	    for(String artist : ss.getArtistList()){
	    	plp.addArtist(artist);	
	    }
	    
	    return plp;
	}
	
	private class RecSongsAdapter extends BaseAdapter {
	
	    private Playlist playlist;	    
	    private int nowPlayingID;
	    public ImageLoader imageLoader; 
	    
	    public static final int NOT_PLAYING = -1;
	    
	    public RecSongsAdapter() {    
	    	playlist = null;
	    	nowPlayingID = NOT_PLAYING;
	    	imageLoader=new ImageLoader(getApplicationContext());
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
	
	    public void setNowPlaying(int position){
	    	nowPlayingID = position;
	    	notifyDataSetChanged();
	    }
	    
	    public boolean isPlaying(int position){
	    	if(nowPlayingID == position)
	    		return true;
	    	
	    	return false;
	    }
	    
	    /*public class ViewHolder{
	    	public TextView username;
	    	public TextView message;
	    	public ImageView image;
	    }*/
	    
	    public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			//ViewHolder holder;
			
			if (v == null) {
			    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			    v = vi.inflate(R.layout.rec_song_row, null);
			}
			 
			Song song = getItem(position);
			if (song != null) {
				TextView tt = (TextView) v.findViewById(R.id.recsong_firstLine);
			    TextView bt = (TextView) v.findViewById(R.id.recsong_secondLine);
			    ImageView coverart = (ImageView) v.findViewById(R.id.recsong_coverart);
			    ImageView playpause = (ImageView) v.findViewById(R.id.recsong_playpause);
			    
			    bt.setText(song.getArtistName());
			    tt.setText(song.getReleaseName());
			    
			    if(nowPlayingID == position)
			    	playpause.setImageResource(R.drawable.pause);
			    else
			    	playpause.setImageResource(R.drawable.play);
			    			    
			    imageLoader.DisplayImage(song.getString("tracks[0].release_image"), RecSongsActivity.this, coverart);
			    
			    //String coverArt = song.getCoverArt();
			    //Log.i(Util.APP, "coverart = ["+(coverArt==null?"null":coverArt)+"]");
			    
				//iv.setImageBitmap(Util.downloadImage(song.getCoverArt()));
		
			}
			
			return v;
		}
	    
	    public void setPlaylist(Playlist pl){
	    	this.playlist = pl;
	    	notifyDataSetChanged();
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
			
			Log.d(Util.APP, "Checkpoint 1");
			
			try {
				pl = EchoNestComm.getComm().createStaticPlaylist(plp[0]);
			} catch (ServiceCommException e) {
				err = e.getMessage();
				return null;
			}
				
			Log.d(Util.APP, "Checkpoint 2");
			return pl;
		}
		
		@Override
		protected void onPostExecute(Playlist result) {
			
			Log.d(Util.APP, "Checkpoint 3");
			//dismissDialog(PROGRESS_DIAG);
			removeDialog(PROGRESS_DIAG);
			
			if(err != null){
				toast = Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG);
	    		toast.show();        		
	    		
	    		RecSongsActivity.this.finish();
	    		return;
    		}
			
			adapter.setPlaylist(result);
			Log.d(Util.APP, "Checkpoint 4");
		}		
	}
	
	private class StartMediaPlayerTask extends AsyncTask<Song, Void, Void> implements OnCompletionListener{
		private String err = null;
		
		@Override
		protected Void doInBackground(Song... song) {
			
			String previewURL = null;
			
			if(isCancelled())
				return null;
				
			MediaPlayerController.getCon().setOnCompletionListener(this);
			
			try{
				Log.d(Util.APP, "Checkpoint 5");
				Track track = song[0].getTrack(EchoNestComm.SEVEN_DIGITAL);
				if(track == null)
					return null;
				
				previewURL = track.getPreviewUrl();		
				Log.d(Util.APP, "Checkpoint 6");
			} catch(Exception e){
				err = getString(R.string.err_mediaplayer);
				Log.e(Util.APP, "EchoNest getTrack() exception!", e);
	    		return null;
			} catch(NoSuchMethodError e){
				err = getString(R.string.err_mediaplayer);
				Log.e(Util.APP, "EchoNest getTrack() error!", e);
				return null;
			}

			if(isCancelled())
				return null;
			
			try {
				MediaPlayerController.getCon().resetAndStart(previewURL);
			} catch (Exception e) {
				err = getString(R.string.err_mediaplayer);
				Log.e(Util.APP, "media player exception!", e);
				return null;
			} 
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(err != null){
				toast = Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG);
	    		toast.show();        		
	    		
	    		err = null;
	    		adapter.setNowPlaying(RecSongsAdapter.NOT_PLAYING);
	    		return;
    		}
			
			
		}

		public void onCompletion(MediaPlayer mp) {
			adapter.setNowPlaying(RecSongsAdapter.NOT_PLAYING);			
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
        	showDialog(EXPORT_DIALOG);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

}
