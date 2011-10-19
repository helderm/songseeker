package com.android.songseeker.activity;

import java.util.HashMap;

import com.android.songseeker.R;
import com.android.songseeker.comm.GroovesharkComm;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.data.UserPlaylistsData;


import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
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
import android.widget.TextView;
import android.widget.Toast;

public class CreatePlaylistGroovesharkActivity extends ListActivity {

	private PlaylistsAdapter adapter;
	
	private static final int REQUEST_AUTH_DIAG = 1;
	private static final int FETCH_SONG_IDS_DIAG = 2;
	private static final int CREATE_PLAYLIST_DIAG = 3;
	private static final int NEW_PLAYLIST_DIAG = 4;
	private static final int USER_AUTH_DIAG = 5;
	
	private ProgressDialog fetchSongIdsDiag;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);	    
	    
	    setContentView(R.layout.playlists_list);		
		getListView().setEmptyView(findViewById(R.id.empty));
		
        adapter = new PlaylistsAdapter();
        setListAdapter(adapter);
				
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
	    
        if(!GroovesharkComm.getComm(settings).isAuthorized()){
        	showDialog(USER_AUTH_DIAG);
        }else
        	new GetUserPlaylistsTask().execute();
        
	    /*try {
	    	//SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
			GroovesharkComm.getComm(settings).requestAuthorize("malakias23", "hema*poa5", settings);
			//GroovesharkComm.getComm().getUserPlaylists();
			//GroovesharkComm.getComm().getSongID("Down in the flood", "The Derek Trucks Band");
			String song1 = GroovesharkComm.getComm().getSongID("strange brew", "cream");
			String song2 = GroovesharkComm.getComm().getSongID("one", "u2");
			
			ArrayList<String> songs = new ArrayList<String>();
			songs.add(song1);
			songs.add(song2);
			
			GroovesharkComm.getComm().createPlaylist("teste1", songs);
		} catch (ServiceCommException e) {
			
		} catch (Exception e){}*/
	    
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
			   	convertView = inflater.inflate(R.layout.playlist_row, null);
				
				holder = new ViewHolder();
				holder.topText = (TextView) convertView.findViewById(R.id.pl_firstLine);
			    holder.botText = (TextView) convertView.findViewById(R.id.pl_secondLine);
			    holder.image = (ImageView) convertView.findViewById(R.id.pl_art);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}				
			
			holder.image.setImageResource(R.drawable.plus2);
		    
		    if(position == 0){
		    	holder.topText.setText("New");		    	
		    	holder.botText.setText("Playlist...");
		    }else{			    
		    	holder.topText.setText(playlists.getPlaylistName(position-1));			    
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
			SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
			
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
			SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
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
			fetchSongIdsDiag.setCancelable(false);			
			return fetchSongIdsDiag;
		case CREATE_PLAYLIST_DIAG:
			ProgressDialog cpd = new ProgressDialog(this);
			cpd.setMessage("Creating playlist on Grooveshark...");
			cpd.setIndeterminate(true);
			cpd.setCancelable(false);
			return cpd;	
		case NEW_PLAYLIST_DIAG:
			Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.new_playlist_diag);
			dialog.setTitle("Name the playlist!");
			
			Button create_but = (Button)dialog.findViewById(R.id.create_pl_but);		
			
			create_but.setOnClickListener(new View.OnClickListener() {
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
	            	
	            	//new CreatePlaylistTask().execute(plName, null, null);	            	
	            }
	        }); 
			
			return dialog;
		
		case USER_AUTH_DIAG:
			Dialog uad = new Dialog(this);
			uad.setContentView(R.layout.user_auth_diag);
			uad.setTitle("Login into Grooveshark");
			
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
	
}
