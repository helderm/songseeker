package com.android.songseeker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.songseeker.comm.EchoNestComm;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.util.MediaPlayerController;
import com.android.songseeker.util.Settings;
import com.android.songseeker.util.Util;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.PlaylistParams.PlaylistType;
import com.echonest.api.v4.Song;
import com.echonest.api.v4.Track;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class RecSongsActivity extends ListActivity {

	private final int PROGRESS_DIAG = 0;
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

	}
	
	@Override
	protected void onResume() {        
        //set up media player
        //player = new MediaPlayer();
        //player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        super.onResume();
	}
	
	@Override
	protected void onPause() {
		MediaPlayerController.getCon().release();
		super.onPause();
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
		//new StartMediaPlayerTask().execute(song);				
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
	    
	    int numArtists = getIntent().getIntExtra("num_artist", -1);	 
	    
	    for(int i=0; i<numArtists; i++){
	    	String str = getIntent().getStringExtra("artist"+i);
	    	if(str.equals(null) || str.compareTo("") == 0){
	    		Log.w("RecSongsActivity", "ignoring null artist ["+i+"]");
	    		continue;
	    	}
	    	
	    	plp.addArtist(str);	    	
	    }
	    return plp;
	}
	
	private class RecSongsAdapter extends BaseAdapter {
	
	    private Playlist playlist;	    
	    private int nowPlayingID;
	    
	    public static final int NOT_PLAYING = -1;
	    
	    public RecSongsAdapter() {    
	    	playlist = null;
	    	nowPlayingID = NOT_PLAYING;
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
	    }
	    
	    public boolean isPlaying(int position){
	    	if(nowPlayingID == position)
	    		return true;
	    	
	    	return false;
	    }
	    
	    public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
			    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			    v = vi.inflate(R.layout.rec_song_row, null);
			}
			 
			Song song = getItem(position);
			if (song != null) {
				TextView tt = (TextView) v.findViewById(R.id.recsong_firstLine);
			    TextView bt = (TextView) v.findViewById(R.id.recsong_secondLine);
			    //ImageView iv = (ImageView) v.findViewById(R.id.recsong_icon);
			    
			    bt.setText(song.getArtistName());
			    tt.setText(song.getReleaseName());
			    
			    //String coverArt = song.getCoverArt();
			    //Log.i("SongSeeker", "coverart = ["+(coverArt==null?"null":coverArt)+"]");
			    
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
			
			Log.d("SongSeeker", "Checkpoint 1");
			
			try {
				pl = EchoNestComm.getComm().createStaticPlaylist(plp[0]);
			} catch (ServiceCommException e) {
				err = e.getMessage();
				return null;
			}
				
			Log.d("SongSeeker", "Checkpoint 2");
			return pl;
		}
		
		@Override
		protected void onPostExecute(Playlist result) {
			
			Log.d("SongSeeker", "Checkpoint 3");
			dismissDialog(PROGRESS_DIAG);
			
			if(err != null){
				toast = Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG);
	    		toast.show();        		
	    		
	    		RecSongsActivity.this.finish();
	    		return;
    		}
			
			adapter.setPlaylist(result);
			Log.d("SongSeeker", "Checkpoint 4");
		}		
	}
	
	private class StartMediaPlayerTask extends AsyncTask<Song, Void, Void>{
		private String err = null;
		
		@Override
		protected Void doInBackground(Song... song) {
			
			String previewURL = null;
			
			if(isCancelled())
				return null;
							
			try{
				Log.d("SongSeeker", "Checkpoint 5");
				Track track = song[0].getTrack(EchoNestComm.SEVEN_DIGITAL);
				if(track == null)
					return null;
				
				previewURL = track.getPreviewUrl();		
				Log.d("SongSeeker", "Checkpoint 6");
			} catch(Exception e){
				err = getString(R.string.err_mediaplayer);
				Log.e("SongSeeker", "EchoNest getTrack() exception!", e);
	    		return null;
			} catch(NoSuchMethodError e){
				err = getString(R.string.err_mediaplayer);
				Log.e("SongSeeker", "EchoNest getTrack() error!", e);
				return null;
			}

			if(isCancelled())
				return null;
			
			try {
				MediaPlayerController.getCon().resetAndStart(previewURL);
			} catch (Exception e) {
				err = getString(R.string.err_mediaplayer);
				Log.e("SongSeeker", "mdia player exception!", e);
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
	    		return;
    		}
			
		}
		
	}	
	
	private class DownloadCoverArtsTask extends AsyncTask<Object, Void, Map<String, Bitmap>>{

		@Override
		protected Map<String, Bitmap> doInBackground(Object... arg) {
			Playlist pl = (Playlist)arg[0];
			int init = (Integer)arg[1];
			int end = (init+5<pl.getSongs().size())? init+5:pl.getSongs().size();
			
			Map<String, Bitmap> imageList = new HashMap<String, Bitmap>();
			for(int i=init; i<end; i++){
				String coverArt = pl.getSongs().get(i).getCoverArt();
				if(coverArt == null)
					continue;
				
				Bitmap image = Util.downloadImage(coverArt);
				if(image != null)
					imageList.put(pl.getSongs().get(i).getID(), image);
			}	
			
			
			return imageList;
		}
		
		@Override
		protected void onPostExecute(Map<String, Bitmap> result) {
		
			
			
		}
		
	}

}
