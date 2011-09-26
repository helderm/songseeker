package com.android.songseeker.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.songseeker.R;
import com.android.songseeker.comm.EchoNestComm;
import com.android.songseeker.comm.RdioComm;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.data.UserPlaylistsData;
import com.android.songseeker.data.SongIdsParcel;
import com.android.songseeker.util.ImageLoader;
import com.android.songseeker.util.Util;

import com.echonest.api.v4.Song;
import com.echonest.api.v4.SongParams;

public class CreatePlaylistRdioActivity extends ListActivity{

	private RdioPlaylistsAdapter adapter;
	
	private static final int REQUEST_AUTH_DIAG = 1;
	private static final int FETCH_SONG_IDS_DIAG = 2;
	private static final int CREATE_PLAYLIST_DIAG = 3;
	private static final int NEW_PLAYLIST_DIAG = 4;
	
	private ProgressDialog fetchSongIdsDiag;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		        
        setContentView(R.layout.playlists_list);
		
		getListView().setEmptyView(findViewById(R.id.empty));
		
        adapter = new RdioPlaylistsAdapter();
        setListAdapter(adapter);
        
		SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
		if(!RdioComm.getComm(settings).isAuthorized()) {
			new RequestAuthorizeTask().execute(null, null, null);			
		} else {
			new GetUserPlaylistsTask().execute(null, null, null);
		}			
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if(position == 0){
			showDialog(NEW_PLAYLIST_DIAG);
		}else{		
			HashMap<String, String> plId = new HashMap<String, String>();
			plId.put("id", adapter.getPlaylistId(position));
			new CreatePlaylistTask().execute(plId, null, null);
		}
	}
	
	private class RdioPlaylistsAdapter extends BaseAdapter {

		UserPlaylistsData data;
				
	    public RdioPlaylistsAdapter() {	 
	    	data = null;	    	
	    }
		
		//@Override
		public int getCount() {			
			if(data == null){
				return 1; //only the "New..." item
			}
			
			return data.getPlaylistsSize()+1;
		}

		//@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		//@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		//@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			
			if (v == null) {
			    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			    v = vi.inflate(R.layout.playlist_row, null);
			}			 

			TextView tt = (TextView) v.findViewById(R.id.pl_firstLine);
		    TextView bt = (TextView) v.findViewById(R.id.pl_secondLine);
		    ImageView img = (ImageView) v.findViewById(R.id.pl_art);
		    		    
		    if(position == 0){
		    	tt.setText("New");
		    	img.setImageResource(R.drawable.plus2);
		    	bt.setText("Playlist...");
		    }else{			    
			    bt.setText(data.getPlaylistNumSongs(position-1)+" songs");
			    tt.setText(data.getPlaylistName(position-1));
			    ImageLoader.getLoader(getCacheDir()).DisplayImage(data.getPlaylistImage(position-1), img, R.drawable.plus2);
		    }		
						
			return v;
		}
		
		private void setUserData(UserPlaylistsData d){
			data = d;
			notifyDataSetChanged();
		}
		
		private String getPlaylistId(int position){
			if(position == 0)
				return null;
			
			return data.getPlaylistId(position-1);
		}
		
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {

		switch(id){
		case REQUEST_AUTH_DIAG:
			ProgressDialog rad = new ProgressDialog(this);
			rad.setMessage("Requesting authorization from Rdio...");
			rad.setIndeterminate(true);
			rad.setCancelable(true);
			return rad;
		case FETCH_SONG_IDS_DIAG:
			fetchSongIdsDiag = new ProgressDialog(this);
			fetchSongIdsDiag.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			fetchSongIdsDiag.setMessage("Fetching song data...");
			fetchSongIdsDiag.setCancelable(false);			
			return fetchSongIdsDiag;
		case CREATE_PLAYLIST_DIAG:
			ProgressDialog cpd = new ProgressDialog(this);
			cpd.setMessage("Creating playlist on Rdio...");
			cpd.setIndeterminate(true);
			cpd.setCancelable(false);
			return cpd;	
		case NEW_PLAYLIST_DIAG:
			//Context mContext = getApplicationContext();
			Dialog dialog = new Dialog(this);

			dialog.setContentView(R.layout.new_playlist_diag);
			dialog.setTitle("Name the playlist!");
			
			Button create_but = (Button)dialog.findViewById(R.id.create_pl_but);
			
			
			create_but.setOnClickListener(new View.OnClickListener() {
	            @SuppressWarnings("unchecked")
				public void onClick(View v) {
	               	View p = (View)v.getParent();	            	
	            	View parent = (View)p.getParent();	            	
	            	EditText textInput = (EditText) parent.findViewById(R.id.pl_name_input); 
	            	
	                //check if the edit text is empty
	            	if(textInput.getText().toString().compareTo("") == 0){
	            		    		
	            		Toast toast = Toast.makeText(CreatePlaylistRdioActivity.this, 
	            						getResources().getText(R.string.invalid_args_str), Toast.LENGTH_SHORT);
	            		toast.show();
	            		removeDialog(NEW_PLAYLIST_DIAG);
	            		return;
	            	}
	            	
	            	//remove the soft input window from view
	            	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
	            	imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0); 
	            		            	
	            	removeDialog(NEW_PLAYLIST_DIAG);
	            	HashMap<String, String> plName = new HashMap<String, String>();
	            	plName.put("name", textInput.getText().toString());
	            	
	            	new CreatePlaylistTask().execute(plName, null, null);
	            	
	            }
	        }); 
			
			return dialog;		
		default:
			return null;
		}
	}
		
	private class RequestAuthorizeTask extends AsyncTask<Void, Void, Boolean>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			showDialog(REQUEST_AUTH_DIAG);
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {

			try {				
				RdioComm.getComm().requestAuthorize(CreatePlaylistRdioActivity.this);
			} catch (ServiceCommException e) {
				Log.e(Util.APP, "Unable to request access to Rdio!", e);
				err = e.getMessage();
				return false;
			} 

			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if(err != null){
				removeDialog(REQUEST_AUTH_DIAG);
				Toast.makeText(getApplicationContext(), err , Toast.LENGTH_SHORT).show();
				CreatePlaylistRdioActivity.this.finish();
			}
			
		}
		
	}

	private class CreatePlaylistTask extends AsyncTask<HashMap<String, String>, Integer, Void>{
		
		private SongIdsParcel sl = getIntent().getExtras().getParcelable("songIds");
		private String err = null;		
		
		@Override
		protected void onPreExecute() {
			showDialog(FETCH_SONG_IDS_DIAG);
			fetchSongIdsDiag.setMax(sl.getSongIDs().size());
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			
			if(progress[0] >= 0)
				fetchSongIdsDiag.setProgress(progress[0]);
			else{
				removeDialog(FETCH_SONG_IDS_DIAG);
				showDialog(CREATE_PLAYLIST_DIAG);
			}
		}
		
		
		@Override
		protected Void doInBackground(HashMap<String, String>... params) {
		
			List<String> songIDs = new ArrayList<String>();
					
			int count = 0;
			for(String id : sl.getSongIDs()){
				Song song = null;
				SongParams sp = new SongParams();
				sp.setID(id);
				sp.addIDSpace(EchoNestComm.RDIO);
				
				try {
					song = EchoNestComm.getComm().getSongs(sp);
					
					String rdioID = song.getString("foreign_ids[0].foreign_id");
					
					String[] split = rdioID.split(":");
					songIDs.add(split[2]);					
				}catch (NoSuchMethodError err){
					Log.e(Util.APP, "NoSuchMethodErr from jEN strikes again!", err);
					continue;
				}catch (ServiceCommException e) {
					err = e.getMessage();
					return null;
				} catch (IndexOutOfBoundsException e){					
					if(song != null){
						Log.d(Util.APP, "Song ["+ song.getReleaseName()+" - " +song.getArtistName()+"] not found on EchoNest! Trying Rdio...");
						
						try{
							songIDs.add(RdioComm.getComm().queryTrackID(song.getReleaseName(), song.getArtistName()));							
						}catch(ServiceCommException ex){
							/* This will need a cleanup of tokens inside queryTrackID
							 * if(ex.getErr() == ServiceErr.NOT_AUTH){
								err = e.getMessage();	
								return null;
							}*/
								
							Log.w(Util.APP, "Err while fetching track data from Rdio! Ignoring track...", ex);
						}
					}					
				}
				
				publishProgress(++count);
			}
			
			Log.i(Util.APP, "SongIDs fetched! Creating playlist...");
			
			//show createPlaylist diag
			publishProgress(-1);
			
			try{
				SharedPreferences settings = getPreferences(MODE_PRIVATE);
				
				String plName = params[0].get("name");
				String plId = params[0].get("id");
				
				if(plName != null){					
					RdioComm.getComm().createPlaylist(plName, songIDs, settings);
				}else{
					RdioComm.getComm().addToPlaylist(plId, songIDs, settings);
				}
			}catch(ServiceCommException e){
				err = e.getMessage();				
			}			
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			removeDialog(CREATE_PLAYLIST_DIAG);
			
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();
			}else{
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.pl_created_str), Toast.LENGTH_LONG).show();
			}
			
			CreatePlaylistRdioActivity.this.finish();
		}
		
	}

	private class GetUserPlaylistsTask extends AsyncTask<Void, Void, UserPlaylistsData>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			Toast.makeText(CreatePlaylistRdioActivity.this, "Fetching your Rdio playlists...", Toast.LENGTH_LONG).show();
		}
		
		@Override
		protected UserPlaylistsData doInBackground(Void... arg0) {
			UserPlaylistsData data;
			try{
				data = RdioComm.getComm().getUserPlaylists();
			} catch(ServiceCommException e){
				err = e.getMessage();
				return null;
			}
			
			return data;
		}
		
		@Override
		protected void onPostExecute(UserPlaylistsData data) {
			if(err != null){
				Toast.makeText(CreatePlaylistRdioActivity.this, "Unable to fetch the user playlists...", Toast.LENGTH_SHORT).show();
				return;
			}
			
			adapter.setUserData(data);
		}
		
	}
	
	@Override
    protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Uri uri = intent.getData();

		removeDialog(REQUEST_AUTH_DIAG);
		
		//Verificando se a chamada vem realmente do callback esperado
		if (uri == null || !uri.toString().contains("oauth")) {
			return;
		}

		Log.d(Util.APP, "OAuth callback started!");
		
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		
		try{
			RdioComm.getComm().retrieveAccessTokens(uri, settings);
		}catch (ServiceCommException e){
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
			return;
		}		
		
		new GetUserPlaylistsTask().execute(null, null, null);
		//new CreatePlaylistTask().execute(null, null, null);		
	}	
}
