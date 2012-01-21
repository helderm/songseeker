package com.seekermob.songseeker.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.LastfmComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceID;
import com.seekermob.songseeker.data.ArtistsParcel;
import com.seekermob.songseeker.data.SongNamesParcel;
import com.seekermob.songseeker.util.Util;

import de.umass.lastfm.Playlist;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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

public class CreatePlaylistLastfmActivity extends ListActivity implements OnCancelListener {

	private LastfmPlaylistAdapter adapter;
	SharedPreferences settings;

	private ProgressDialog addTracksDiag;
	private CreatePlaylistTask createPlaylistTask = null;
	
	private static final int REQUEST_AUTH_DIAG = 1;
	private static final int CREATE_PLAYLIST_DIAG = 2;
	private static final int NEW_PLAYLIST_DIAG = 3;
	private static final int ADD_TRACKS_DIAG = 4;
	private static final int USER_AUTH_DIAG = 5;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setContentView(R.layout.listview);
		
		getListView().setEmptyView(findViewById(R.id.empty));
		
        adapter = new LastfmPlaylistAdapter();
        setListAdapter(adapter);
				
        settings = getApplicationContext().getSharedPreferences(Util.APP, Context.MODE_PRIVATE);
		if(!LastfmComm.getComm(settings).isAuthorized()){
			showDialog(USER_AUTH_DIAG);			
			return;
		}
		
		//check orientation change
		@SuppressWarnings("unchecked")
		List<Playlist> savedPls = (List<Playlist>) getLastNonConfigurationInstance();
		if(savedPls == null)
			new GetUserPlaylistsTask().execute(null, null, null);
		else
			adapter.setPlaylists(savedPls);		
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if(position == 0){
			showDialog(NEW_PLAYLIST_DIAG);
		}else{		
			HashMap<String, String> plId = new HashMap<String, String>();
			plId.put("id", adapter.getPlaylistId(position));
			createPlaylistTask = (CreatePlaylistTask) new CreatePlaylistTask().execute(plId, null, null);
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {

		switch(id){
		case REQUEST_AUTH_DIAG:
			ProgressDialog rad = new ProgressDialog(this);
			rad.setMessage("Requesting authorization from Last.fm...");
			rad.setIndeterminate(true);
			rad.setCancelable(true);
			return rad;
		case CREATE_PLAYLIST_DIAG:
			ProgressDialog cpd = new ProgressDialog(this);
			cpd.setMessage("Creating playlist on Last.fm...");
			cpd.setIndeterminate(true);
			cpd.setCancelable(true);
			cpd.setOnCancelListener(this);
			return cpd;	
		case ADD_TRACKS_DIAG:	
			addTracksDiag = new ProgressDialog(this);
			addTracksDiag.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			addTracksDiag.setMessage("Adding songs to the playlist...");
			addTracksDiag.setCancelable(true);
			addTracksDiag.setOnCancelListener(this);	
			return addTracksDiag;
		case NEW_PLAYLIST_DIAG:
			Dialog npd = new Dialog(this);
			npd.setContentView(R.layout.new_playlist_diag);
			npd.setTitle("Playlist Name:");
			
			Button create_but = (Button)npd.findViewById(R.id.create_pl_but);			
			
			create_but.setOnClickListener(new View.OnClickListener() {
	            @SuppressWarnings("unchecked")
				public void onClick(View v) {
	            
	            	View p = (View)v.getParent();	            	
	            	View parent = (View)p.getParent();	            	
	            	EditText textInput = (EditText) parent.findViewById(R.id.pl_name_input); 
	            	
	                //check if the edit text is empty
	            	if(textInput.getText().toString().compareTo("") == 0){
	            		    		
	            		Toast toast = Toast.makeText(CreatePlaylistLastfmActivity.this, 
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
	            	createPlaylistTask = (CreatePlaylistTask) new CreatePlaylistTask().execute(plName, null, null);	            	
	            }
	        }); 
			
			return npd;	
		case USER_AUTH_DIAG:
			Dialog uad = new Dialog(this);
			uad.setContentView(R.layout.user_auth_diag);
			uad.setTitle("Login into Last.fm");
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
	            		    		
	            		Toast toast = Toast.makeText(CreatePlaylistLastfmActivity.this, 
	            						getResources().getText(R.string.invalid_args_str), Toast.LENGTH_SHORT);
	            		toast.show();
	            		CreatePlaylistLastfmActivity.this.finish();
	            		return;
	            	}
	            	
	            	//remove the soft input window from view
	            	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
	            	imm.hideSoftInputFromWindow(userInput.getWindowToken(), 0); 
	            	
	            	removeDialog(USER_AUTH_DIAG);
	            	
	            	HashMap<String, String> plUserPwd = new HashMap<String, String>();
	            	plUserPwd.put("user", userInput.getText().toString());	            	
	            	plUserPwd.put("pwd", pwdInput.getText().toString());
	            	new RequestAuthorizeTask().execute(plUserPwd, null, null);	            	
	            }
	        }); 
			
			return uad;	
		default:
			return null;
		}
	}
	
	
	private class LastfmPlaylistAdapter extends BaseAdapter{
		private List<Playlist> pls = null;
		
		public int getCount() {
			if(pls == null)
				return 1;
			
			return pls.size()+1;
		}

		public Object getItem(int position) {
			return pls.get(position-1);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			
			if (v == null) {
			    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			    v = vi.inflate(R.layout.list_row, null);
			}			 

			TextView tt = (TextView) v.findViewById(R.id.firstLine);
		    TextView bt = (TextView) v.findViewById(R.id.secondLine);
		    
		    ImageView img = (ImageView) v.findViewById(R.id.coverart);
		    img.setImageResource(R.drawable.ic_playlist_stub);
		    
		    if(position == 0){
		    	tt.setText("New Playlist...");		    	
		    	bt.setText("");
		    }else{			    
			    bt.setText(pls.get(position-1).getSize()+" songs");
			    tt.setText(pls.get(position-1).getTitle());			    
		    }	
		    
		    return v;
		}
		
		public void setPlaylists(List<Playlist> playlists){
			pls = playlists;
			notifyDataSetChanged();
		}
		
		public String getPlaylistId(int i){
			if(i == 0)
				return null;
			
			return String.valueOf(pls.get(i-1).getId());
		}
	}
	
	private class RequestAuthorizeTask extends AsyncTask<HashMap<String,String>, Void, Void>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			removeDialog(USER_AUTH_DIAG);
			showDialog(REQUEST_AUTH_DIAG);
		}
		
		@Override
		protected Void doInBackground(HashMap<String, String>... args) {
			String user = args[0].get("user");
			String pwd = args[0].get("pwd");
			
			try{			
				LastfmComm.getComm().requestAuthorize(user, pwd, settings);
			}catch(ServiceCommException e){
				err = e.getMessage();
			}
			
			return null;
		}
				
		@Override
		protected void onPostExecute(Void result) {
			
			removeDialog(REQUEST_AUTH_DIAG);
			
			if(err != null){				
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				CreatePlaylistLastfmActivity.this.finish();
				return;
			}
			
			new GetUserPlaylistsTask().execute(null, null, null);
		}
		
	}
	
	private class GetUserPlaylistsTask extends AsyncTask<Void, Void, List<Playlist>>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			Toast.makeText(CreatePlaylistLastfmActivity.this, "Fetching your Last.fm playlists...", Toast.LENGTH_LONG).show();
		}
		
		@Override
		protected List<Playlist> doInBackground(Void... arg0) {			
			try {
				return (List<Playlist>) LastfmComm.getComm().getUserPlaylists();
			} catch (ServiceCommException e) {
				err = e.getMessage();
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(List<Playlist> data) {
			if(err != null){
				Toast.makeText(CreatePlaylistLastfmActivity.this, err, Toast.LENGTH_SHORT).show();
				return;
			}
			
			adapter.setPlaylists(data);
		}
		
	}
	
	private class CreatePlaylistTask extends AsyncTask<HashMap<String, String>, Integer, Void>{
		private SongNamesParcel sn = getIntent().getExtras().getParcelable("songNames");
		private ArtistsParcel ar = getIntent().getExtras().getParcelable("songArtists");
		private String err;
		
		@Override
		protected void onPreExecute() {
			showDialog(CREATE_PLAYLIST_DIAG);			
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			if(progress[0] >= 0)
				addTracksDiag.setProgress(progress[0]);
			else{
				removeDialog(CREATE_PLAYLIST_DIAG);
				showDialog(ADD_TRACKS_DIAG);
				addTracksDiag.setMax(sn.getSongNames().size());
			}
		}
		
		@Override
		protected Void doInBackground(HashMap<String, String>... params) {
			String plName = params[0].get("name");
			int plId = 0;			
			
			if(!LastfmComm.getComm().isAuthorized()){
				ServiceCommException e = new ServiceCommException(ServiceID.LASTFM, ServiceErr.NOT_AUTH);
				err = e.getMessage();
				return null;
			}
			
			try{		
				//check if the task was cancelled by the user
				if(Thread.interrupted()){
					return null;
				}
				
				if(plName != null){
					Playlist pl = LastfmComm.getComm().createPlaylist(plName, settings);
					plId = pl.getId();
				}else{
					plId = Integer.parseInt(params[0].get("id"));
				}
				
				publishProgress(-1);
				
				int i;
				ArrayList<String> songNames = sn.getSongNames();
				ArrayList<String> songArtists = ar.getArtistList();
				
				for(i=0; i<songNames.size() && i<songArtists.size(); i++){
					if(Thread.interrupted()){
						return null;
					}
					
					LastfmComm.getComm().addToPlaylist(plId, songArtists.get(i), songNames.get(i), settings);
					publishProgress(i+1);
				}				
			}catch (ServiceCommException e){
				err = e.getMessage();
				return null;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			removeDialog(ADD_TRACKS_DIAG);
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(getApplicationContext(), 
						getResources().getText(R.string.pl_created_str), Toast.LENGTH_SHORT).show();
			}
			
			CreatePlaylistLastfmActivity.this.finish();
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
		return adapter.pls;
	}
}
