package com.seekermob.songseeker.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.android.apps.analytics.easytracking.TrackedListActivity;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.GroovesharkComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceID;
import com.seekermob.songseeker.data.ArtistsParcel;
import com.seekermob.songseeker.data.SongNamesParcel;
import com.seekermob.songseeker.data.UserPlaylistsData;
import com.seekermob.songseeker.util.Util;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
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

public class CreatePlaylistGroovesharkActivity extends TrackedListActivity implements OnCancelListener {

	private PlaylistsAdapter adapter;
	
	private static final int REQUEST_AUTH_DIAG = 1;
	private static final int FETCH_SONG_IDS_DIAG = 2;
	private static final int CREATE_PLAYLIST_DIAG = 3;
	private static final int NEW_PLAYLIST_DIAG = 4;
	private static final int USER_AUTH_DIAG = 5;
	
	private CreatePlaylistTask createPlaylistTask = null;
	private ProgressDialog fetchSongIdsDiag = null;
	SharedPreferences settings;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);	    
	    
	    setContentView(R.layout.listview);		
		getListView().setEmptyView(findViewById(R.id.empty));
		
        adapter = new PlaylistsAdapter();
        setListAdapter(adapter);
				
        settings = getApplicationContext().getSharedPreferences(Util.APP, Context.MODE_PRIVATE);
        
        if(!GroovesharkComm.getComm(settings).isAuthorized()){
        	showDialog(USER_AUTH_DIAG);
        	return;
        }
        
        //check orientation change
        UserPlaylistsData savedPls = (UserPlaylistsData) getLastNonConfigurationInstance();
        if(savedPls == null)
        	new GetUserPlaylistsTask().execute();
        else
        	adapter.setPlaylists(savedPls);
	}
	
	private class PlaylistsAdapter extends BaseAdapter{
		private UserPlaylistsData playlists = null;
		private LayoutInflater inflater;
		
		public PlaylistsAdapter() {
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		public int getCount() {
			if(playlists == null)
				return 1;
			
			return playlists.getPlaylistsSize()+1;
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
			   	convertView = inflater.inflate(R.layout.list_row, null);
				
				holder = new ViewHolder();
				holder.topText = (TextView) convertView.findViewById(R.id.firstLine);
			    holder.botText = (TextView) convertView.findViewById(R.id.secondLine);
			    holder.image = (ImageView) convertView.findViewById(R.id.coverart);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}				
			
			holder.image.setImageResource(R.drawable.ic_playlist_stub);
		    
		    if(position == 0){
		    	holder.topText.setText("New Playlist...");		    	
		    	holder.botText.setText("");
		    }else{			    
		    	holder.topText.setText(playlists.getPlaylistName(position-1));		
		    	holder.botText.setText("");
		    }	
		    
		    return convertView;
		}
		
		public void setPlaylists(UserPlaylistsData pls){
			playlists = pls;
			notifyDataSetChanged();
		}
		
		public String getPlaylistId(int i){
			if(i == 0)
				return null;
			
			return playlists.getPlaylistId(i-1);
		}
		
	    private class ViewHolder{
	    	public TextView topText;
	    	public TextView botText;
	    	public ImageView image;
	    }
	}
	
	private class RequestAuthorizeTask extends AsyncTask<HashMap<String, String>, Void, Void>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			showDialog(REQUEST_AUTH_DIAG);
		}
		
		@Override
		protected Void doInBackground(HashMap<String, String>... args) {
			String user = args[0].get("user");
			String pwd = args[0].get("pwd");
			
			try {				
				GroovesharkComm.getComm().requestAuthorize(user, pwd, settings);
			} catch (ServiceCommException e) {
				err = e.getMessage();
				return null;
			}
			
			return null; 
		}
		
		@Override
		protected void onPostExecute(Void result) {
			removeDialog(REQUEST_AUTH_DIAG);
			
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				CreatePlaylistGroovesharkActivity.this.finish();
				return;
			}
			
			new GetUserPlaylistsTask().execute();
		}		
	}	

	private class GetUserPlaylistsTask extends AsyncTask<Void, Void, UserPlaylistsData>{
		private String err = null;		
		
		@Override
		protected void onPreExecute() {
			Toast.makeText(CreatePlaylistGroovesharkActivity.this, "Fetching your Grooveshark playlists...", 
								Toast.LENGTH_LONG).show();
		}
		
		@Override
		protected UserPlaylistsData doInBackground(Void... arg0) {			
			UserPlaylistsData data = null;
			
			try{
				data = GroovesharkComm.getComm().getUserPlaylists(settings);
			} catch(ServiceCommException e) {
				err = e.getMessage();
				return null;
			}
			
			return data;
		}
		
		@Override
		protected void onPostExecute(UserPlaylistsData data) {
			
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				CreatePlaylistGroovesharkActivity.this.finish();
				return;
			}
			
			adapter.setPlaylists(data);
		}		
	}	
	
	private class CreatePlaylistTask extends AsyncTask<HashMap<String, String>, Integer, Void>{
		
		private String err = null;		
		
		private List<String> songNames = new ArrayList<String>();
		private List<String> artistNames = new ArrayList<String>(); 
		
		@Override
		protected void onPreExecute() {
			SongNamesParcel sn = getIntent().getExtras().getParcelable("songNames");
			ArtistsParcel ar = getIntent().getExtras().getParcelable("songArtists");
			
			showDialog(FETCH_SONG_IDS_DIAG);
			
			//access is limited today to 32 ws calls/ip/minute, so i'll need to truncate the playlist 
			if(sn.getSongNames().size() > 30){
				
				songNames =  sn.getSongNames().subList(0, 30);
				artistNames = ar.getArtistList().subList(0, 30);
				Toast.makeText(getApplicationContext(), "Truncating playlist to 30 songs, due to technical reasons...", Toast.LENGTH_LONG).show();
			}else{
				songNames = sn.getSongNames();
				artistNames = ar.getArtistList();
			}
			
			fetchSongIdsDiag.setMax(songNames.size());
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
		
			ArrayList<String> songIDs = new ArrayList<String>();
			
			if(!GroovesharkComm.getComm().isAuthorized()){
				ServiceCommException e = new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.NOT_AUTH);
				err = e.getMessage();
				return null;
			}
			
			int count = 0;
			for(int i=0; i<songNames.size(); i++){
				
				//check if the task was cancelled by the user
				if(Thread.interrupted()){
					return null;
				}
				
				try {					
					String gsID = GroovesharkComm.getComm().getSongID(songNames.get(i), artistNames.get(i), settings);
					songIDs.add(gsID);					
				}catch (ServiceCommException e) {
					if(e.getErr() == ServiceErr.SONG_NOT_FOUND){
						publishProgress(++count);
						Log.i(Util.APP, "Song ["+songNames.get(i)+" - "+artistNames.get(i)+"] not found in Grooveshark, ignoring...");
						continue;
					}
					
					err = e.getMessage();
					return null;
				} 
				
				publishProgress(++count);
			}
			
			//check if the task was cancelled by the user
			if(Thread.interrupted()){
				return null;
			}
			
			if(songIDs.isEmpty()){
				err = "No song found!";
				return null;
			}
			
			//show createPlaylist diag
			publishProgress(-1);
			
			try{
				//export playlist
				
				String plName = params[0].get("name");
				String plID = params[0].get("id");
				
				if(plName != null){					
					//create new playlist if the name is given
					GroovesharkComm.getComm().createPlaylist(plName, songIDs, settings);
				}else{
					//add songs to existing playlist, if id is given					
					if(Thread.interrupted()){
						return null;
					}					
					
					ArrayList<String> plSongs = GroovesharkComm.getComm().getPlaylistSongs(plID, settings);
					
					if(Thread.interrupted()){
						return null;
					}
					
					songIDs.addAll(plSongs);					
					GroovesharkComm.getComm().setPlaylistSongs(plID, songIDs, settings);					
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
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.pl_created_str), Toast.LENGTH_LONG).show();
			}
			
			CreatePlaylistGroovesharkActivity.this.finish();
		}
		
	}
	
	
	@SuppressWarnings("unchecked")
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if(position == 0){
			showDialog(NEW_PLAYLIST_DIAG);
		}else{		
			HashMap<String, String> plId = new HashMap<String, String>();
			plId.put("id", adapter.getPlaylistId(position));
			createPlaylistTask = (CreatePlaylistTask) new CreatePlaylistTask().execute(plId);
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {

		switch(id){
		case REQUEST_AUTH_DIAG:
			ProgressDialog rad = new ProgressDialog(this);
			rad.setMessage("Requesting authorization from Grooveshark...");
			rad.setIndeterminate(true);
			rad.setCancelable(true);
			return rad;
		case FETCH_SONG_IDS_DIAG:
			fetchSongIdsDiag = new ProgressDialog(this);
			fetchSongIdsDiag.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			fetchSongIdsDiag.setMessage("Fetching song data...");
			fetchSongIdsDiag.setCancelable(true);
			fetchSongIdsDiag.setOnCancelListener(this);
			return fetchSongIdsDiag;
		case CREATE_PLAYLIST_DIAG:
			ProgressDialog cpd = new ProgressDialog(this);
			cpd.setMessage("Creating playlist on Grooveshark...");
			cpd.setIndeterminate(true);
			cpd.setCancelable(true);
			cpd.setOnCancelListener(this);
			return cpd;	
		case NEW_PLAYLIST_DIAG:
			Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.new_playlist_diag);
			dialog.setTitle("Playlist Name:");
			
			Button create_but = (Button)dialog.findViewById(R.id.create_pl_but);		
			
			create_but.setOnClickListener(new View.OnClickListener() {
				@SuppressWarnings("unchecked")
				public void onClick(View v) {
	               	View p = (View)v.getParent();	            	
	            	View parent = (View)p.getParent();	            	
	            	EditText textInput = (EditText) parent.findViewById(R.id.pl_name_input); 
	            	
	                //check if the edit text is empty
	            	if(textInput.getText().toString().compareTo("") == 0){
	            		    		
	            		Toast.makeText(CreatePlaylistGroovesharkActivity.this, 
	            						getResources().getText(R.string.invalid_args_str), Toast.LENGTH_SHORT).show();
	            		
	            		removeDialog(NEW_PLAYLIST_DIAG);
	            		return;
	            	}
	            	
	            	//remove the soft input window from view
	            	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
	            	imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0); 
	            		            	
	            	removeDialog(NEW_PLAYLIST_DIAG);
	            	HashMap<String, String> plName = new HashMap<String, String>();
	            	plName.put("name", textInput.getText().toString());
	            	
	            	createPlaylistTask = (CreatePlaylistTask)new CreatePlaylistTask().execute(plName);	            	
	            }
	        }); 
			
			return dialog;
		
		case USER_AUTH_DIAG:
			Dialog uad = new Dialog(this);
			uad.setContentView(R.layout.user_auth_diag);
			uad.setTitle("Login into Grooveshark");
			uad.setCancelable(true);
			
			Button auth_but = (Button)uad.findViewById(R.id.auth_but);			
			
			auth_but.setOnClickListener(new View.OnClickListener() {
	            @SuppressWarnings("unchecked")
				public void onClick(View v) {
	            
	            	View p = (View)v.getParent();	            	
	            	View parent = (View)p.getParent();	            	
	            	EditText userInput = (EditText) parent.findViewById(R.id.user_name_input); 
	            	EditText pwdInput = (EditText) parent.findViewById(R.id.user_pwd_input);
	            	
	                //check if the edit text is empty
	            	if(userInput.getText().toString().compareTo("") == 0 ||
	            		pwdInput.getText().toString().compareTo("") == 0){
	            		    		
	            		Toast.makeText(CreatePlaylistGroovesharkActivity.this, getResources().getText(R.string.invalid_args_str), Toast.LENGTH_SHORT).show();
	            		//CreatePlaylistLastfmActivity.this.finish();
	            		return;
	            	}
	            	
	            	//remove the soft input window from view
	            	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
	            	imm.hideSoftInputFromWindow(userInput.getWindowToken(), 0); 
	            	
	            	removeDialog(USER_AUTH_DIAG);
	            	
	            	HashMap<String, String> plUserPwd = new HashMap<String, String>();
	            	plUserPwd.put("user", userInput.getText().toString());	            	
	            	plUserPwd.put("pwd", pwdInput.getText().toString());
	            	new RequestAuthorizeTask().execute(plUserPwd);	            	
	            }
	        }); 
			
			return uad;
			
		default:
			return null;
		}
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		if(createPlaylistTask != null)
			createPlaylistTask.cancel(true);
		
		Toast.makeText(getApplicationContext(), getString(R.string.op_cancel_str), Toast.LENGTH_SHORT).show();
		finish();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return adapter.playlists;
	}
}
