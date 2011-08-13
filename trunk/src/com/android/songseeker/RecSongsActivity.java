package com.android.songseeker;

import java.util.List;

import com.android.songseeker.comm.EchoNestComm;
import com.android.songseeker.comm.ServiceCommException;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.PlaylistParams.PlaylistType;
import com.echonest.api.v4.Song;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class RecSongsActivity extends ListActivity {

	private final int PROGRESS_DIAG = 0;
	private RecSongsAdapter adapter;
	
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
	    
	    PlaylistParams plp = buildPlaylistParams();
	    
	    new GetPlaylistTask().execute(plp, null, null);

	}

	private void populateRecommendedSongs(Playlist pl) {
		/*LinearLayout l = (LinearLayout) findViewById(R.id.rec_songs_list_layout);
		LayoutInflater linflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
		
		for(Song song : pl.getSongs()){			
			
			/*String previewURL = null;
			try {
				previewURL = song.getTrack("7digital").getPreviewUrl();
			} catch (EchoNestException e) {
				Log.w("SongSeeker", e);
			} catch (NullPointerException e){
				Log.w("SongSeeker", e);
			} catch (Exception e){
				Log.e("SongSeeker", "bug", e);
			}
			
			Log.i("SongSeeker", "previewURL = ["+previewURL+"]");
			
		    View myView = linflater.inflate(R.layout.rec_song, null);                    
		    TextView t = (TextView) myView.findViewById(R.id.song_info);          
		    t.setText((song.getReleaseName()).toString() + " - " + song.getArtistName());
		    t.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    		Toast toast = Toast.makeText(RecSongsActivity.this, "Teste - "+getPackageResourcePath(), Toast.LENGTH_LONG);
		    		toast.show();  
		      	}
		    }); 
	          
	        l.addView(myView);		
	        
		}*/		
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
	    plp.setVariety(Settings.getVariety());
	    plp.setMinEnergy(Settings.getMinEnergy());
	    plp.setMaxEnergy(Settings.getMaxEnergy());
	    plp.setMinDanceability(Settings.getMinDanceability());
	    plp.setMaxDanceability(Settings.getMaxDanceability());
	    plp.setMinTempo(Settings.getMinTempo());
	    plp.setMaxTempo(Settings.getMaxTempo());
	    plp.setSongMinHotttnesss(Settings.getMinHotness());
	    plp.setSongtMaxHotttnesss(Settings.getMaxHotness());
	    
	    plp.addIDSpace("7digital");
	    plp.setLimit(true);
	    
	    List<String> moods = Settings.getMood();
	    for(String mood : moods){
	    	plp.add("mood", mood);
	    }
	    moods = null;
	    
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
			
			dismissDialog(PROGRESS_DIAG);
			
			if(err != null){
				Toast toast = Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG);
	    		toast.show();        		
	    		
	    		RecSongsActivity.this.finish();
	    		return;
    		}
			
			adapter.setPlaylist(result);
	
			//populateRecommendedSongs(result);			
		}
		
	}
	
	 public class RecSongsAdapter extends BaseAdapter {

	        private Playlist playlist;
	        //private Context mContext;
	        
	        public RecSongsAdapter() {    
	        	playlist = null;
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
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.rec_song_row, null);
                }
                
                Song song = getItem(position);
                if (song != null) {
                	TextView tt = (TextView) v.findViewById(R.id.recsong_firstLine);
                    TextView bt = (TextView) v.findViewById(R.id.recsong_secondLine);
                   
                    bt.setText(song.getArtistName());
                    tt.setText(song.getReleaseName());
                }
                return v;
	        }
	        
	        public void setPlaylist(Playlist pl){
	        	this.playlist = pl;
	        	notifyDataSetChanged();
	        }
	 }
	 
	

}
