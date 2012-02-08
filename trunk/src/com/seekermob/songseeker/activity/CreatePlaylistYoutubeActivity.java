package com.seekermob.songseeker.activity;

import java.util.ArrayList;
import java.util.HashMap;

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.YouTubeComm;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceID;
import com.seekermob.songseeker.comm.YouTubeComm.VideoFeed;
import com.seekermob.songseeker.data.ArtistsParcel;
import com.seekermob.songseeker.data.SongNamesParcel;
import com.seekermob.songseeker.data.UserPlaylistsData;
import com.seekermob.songseeker.util.Util;
import com.google.android.apps.analytics.easytracking.TrackedListActivity;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
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

public class CreatePlaylistYoutubeActivity extends TrackedListActivity implements AccountManagerCallback<Bundle>, OnCancelListener {
	
	private static final int ACCOUNTS_DIAG = 0;
	private static final int REQUEST_AUTH_DIAG = 1;
	private static final int CREATE_PLAYLIST_DIAG = 2;
	private static final int FETCH_VIDEO_IDS_DIAG = 3;
	private static final int NEW_PLAYLIST_DIAG = 4;
	private static final int ADD_VIDEOS_PLAYLIST_DIAG = 5;
	
	private ProgressDialog progressDiag;
	private CreatePlaylistTask createPlaylistTask = null;
	
	private YouTubePlaylistsAdapter adapter;
	private SharedPreferences settings;
	
	public static final int REQUEST_AUTHENTICATE = 0;	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.listview);

        getListView().setEmptyView(findViewById(R.id.empty));
		
        adapter = new YouTubePlaylistsAdapter();
        setListAdapter(adapter);
        
		settings = getApplicationContext().getSharedPreferences(Util.APP, Context.MODE_PRIVATE);
		
		if(!YouTubeComm.getComm(this, settings).isAuthorized()){
		
			//check if the user has a Google account configured
			final String[] names = YouTubeComm.getComm().getAccountsNames();
			if(names.length <= 0){
				Toast.makeText(getApplicationContext(), "No Google account found in your device!", Toast.LENGTH_SHORT).show();
				
				startActivity(new Intent(Settings.ACTION_SYNC_SETTINGS));
				
				finish();
				return;
			}
			
			showDialog(ACCOUNTS_DIAG);
			
			return;
		}
		
		//check orientation change
		UserPlaylistsData savedPls = (UserPlaylistsData) getLastNonConfigurationInstance();
		if(savedPls == null)		
			new GetUserPlaylistsTask().execute();
		else
			adapter.setUserData(savedPls);		
	}

	private class YouTubePlaylistsAdapter extends BaseAdapter {
		private UserPlaylistsData data;
		private LayoutInflater inflater;
		
		public YouTubePlaylistsAdapter() {
			data = null;
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		@Override
		public int getCount() {
			if(data == null){
				return 1; //only the "New..." item
			}
			
			return data.getPlaylistsSize()+1;
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int position) {			
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			
			if (v == null) {
			    v = inflater.inflate(R.layout.list_row, null);
			}			 

			TextView tt = (TextView) v.findViewById(R.id.firstLine);
		    TextView bt = (TextView) v.findViewById(R.id.secondLine);
		    
		    ImageView img = (ImageView) v.findViewById(R.id.coverart);
		    img.setImageResource(R.drawable.ic_playlist_stub);	
		    
		    if(position == 0){
		    	tt.setText("New Playlist...");		    	
		    	bt.setText("");
		    }else{			    
		    	tt.setText(data.getPlaylistName(position-1));
		    	bt.setText(data.getPlaylistNumSongs(position-1)+" videos");			    			    
		    }		
						
			return v;
		}

		public void setUserData(UserPlaylistsData d) {
			data = d;
			notifyDataSetChanged();
		}
		
	}
	
	private class RequestAuthorizeTask extends AsyncTask<String, Void, Void>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			removeDialog(ACCOUNTS_DIAG);
			showDialog(REQUEST_AUTH_DIAG);
		}
		
		@Override
		protected Void doInBackground(String... name) {
			
			//try{			
				YouTubeComm.getComm().requestAuthorize(name[0], CreatePlaylistYoutubeActivity.this, settings);
			//}catch(ServiceCommException e){
			//	err = e.getMessage();
			//}
			
			return null;
		}
				
		@Override
		protected void onPostExecute(Void result) {
			
			removeDialog(REQUEST_AUTH_DIAG);
			
			if(err != null){				
			//	Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				CreatePlaylistYoutubeActivity.this.finish();
				return;
			}
			
			//new GetUserPlaylistsTask().execute(null, null, null);
		}		
	}
	
	//requestAuthorize callback function
	@Override
	public void run(AccountManagerFuture<Bundle> future) {
		try {
			Bundle bundle = future.getResult();
			if(bundle.containsKey(AccountManager.KEY_INTENT)) {
				//user didn't yet authorize our app within Google, sending an Intent to do so
				Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
				intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivityForResult(intent, REQUEST_AUTHENTICATE);
			} else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
				//user already authorized us with Google
				YouTubeComm.getComm().setAccessToken(bundle.getString(AccountManager.KEY_AUTHTOKEN), settings);				
				new GetUserPlaylistsTask().execute();
			}
		} catch (Exception e) {
			handleException(e);
		}		
	}
	
	//startActivityForResult(intent, REQUEST_AUTHENTICATE) callback
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		//TODO Check!
		switch (requestCode) {
		case REQUEST_AUTHENTICATE:
			if (resultCode == RESULT_OK) {
				//user authorized us within Google
				new GetUserPlaylistsTask().execute();
			} else {
				//user denied our app
				ServiceCommException e = new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.NOT_AUTH);
				Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();				
				CreatePlaylistYoutubeActivity.this.finish();
			}
			break;
		}
	}

	void handleException(Exception e){
		if (e instanceof HttpResponseException) {
			HttpResponse response = ((HttpResponseException) e).getResponse();
			int statusCode = response.getStatusCode();

			if (statusCode == 401 || statusCode == 403) {	          
				YouTubeComm.getComm().unauthorizeUser(settings);
				
				ServiceCommException ex = new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.NOT_AUTH);
				Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
				return;
			}
			
			ServiceCommException ex = new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.UNKNOWN);
			Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();			
		}
		
		Log.e(Util.APP, e.getMessage(), e);
	}
		
	
	private class GetUserPlaylistsTask extends AsyncTask<Void, Void, UserPlaylistsData>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			Toast.makeText(CreatePlaylistYoutubeActivity.this, "Fetching your YouTube playlists...", Toast.LENGTH_LONG).show();
		}
		
		@Override
		protected UserPlaylistsData doInBackground(Void... arg0) {
			UserPlaylistsData data;
			try{
				data = YouTubeComm.getComm().getUserPlaylists(settings);
			} catch(ServiceCommException e){
				err = e.getMessage();
				return null;
			}
			
			return data;
		}
		
		@Override
		protected void onPostExecute(UserPlaylistsData data) {
			if(err != null){
				Toast.makeText(CreatePlaylistYoutubeActivity.this, "Unable to fetch your playlists...", Toast.LENGTH_SHORT).show();
				return;
			}
			
			adapter.setUserData(data);
		}
		
	}
	
	private class CreatePlaylistTask extends AsyncTask<HashMap<String, String>, Integer, Void>{
		private SongNamesParcel sn = getIntent().getExtras().getParcelable("songNames");
		private ArtistsParcel ar = getIntent().getExtras().getParcelable("songArtists");
		private ArrayList<VideoFeed> videos = new ArrayList<VideoFeed>();
		
		private String err;
		
		@Override
		protected void onPreExecute() {
			showDialog(FETCH_VIDEO_IDS_DIAG);
			progressDiag.setMax(sn.getSongNames().size());		
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			if(progress[0] >= 0)
				progressDiag.setProgress(progress[0]);
			else{
				switch(progress[0]){
				case -1:
					removeDialog(FETCH_VIDEO_IDS_DIAG);					
					break;
				case -2:
					showDialog(CREATE_PLAYLIST_DIAG);
					break;
				case -3:
					removeDialog(CREATE_PLAYLIST_DIAG);
					break;
				case -4:
					showDialog(ADD_VIDEOS_PLAYLIST_DIAG);
					progressDiag.setMax(videos.size());
				}	
			}
		}
		
		@Override
		protected Void doInBackground(HashMap<String, String>... params) {

			if(!YouTubeComm.getComm().isAuthorized()){
				ServiceCommException e = new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.NOT_AUTH);
				err = e.getMessage();
				return null;
			}
			
			try{				
				ArrayList<String> songNames = sn.getSongNames();
				ArrayList<String> songArtists = ar.getArtistList();
				
				//fetch video ids		
				for(int i=0; i<songNames.size() && i<songArtists.size(); i++){
					//check if the task was cancelled by the user
					if(Thread.interrupted()){
						return null;
					}
					
					ArrayList<VideoFeed> vids = YouTubeComm.getComm().searchVideo(songNames.get(i), songArtists.get(i), 1);
					publishProgress(i+1);
					
					if(vids.size() == 0){
						Log.i(Util.APP, "Song ["+songNames.get(i) + "-" + songArtists.get(i)+"] not found on YouTube, ignoring...");
						continue;
					}
				
					videos.add(vids.get(0));
				}
				
				if(Thread.interrupted()){
					return null;
				}
				
				publishProgress(-1);

				String plName = params[0].get("name");
				String plID = params[0].get("id");
				
				//create a playlist
				if(plName != null){					
					publishProgress(-2);
					plID = YouTubeComm.getComm().createPlaylist(plName, settings);
					publishProgress(-3);
				}
				
				//add videos to the playlist
				int i=0;
				
				publishProgress(-4);
				for(VideoFeed video : videos){					
					
					if(Thread.interrupted()){
						return null;
					}
					
					publishProgress(++i);
					YouTubeComm.getComm().addVideosToPlaylist(plID, video.id, settings);
				}		
			}catch (ServiceCommException e){
				err = e.getMessage();
				return null;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(getApplicationContext(), 
						getResources().getText(R.string.pl_created_str), Toast.LENGTH_SHORT).show();
			}
			
			CreatePlaylistYoutubeActivity.this.finish();
		}		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if(position == 0){
			showDialog(NEW_PLAYLIST_DIAG);
		}else{		
			HashMap<String, String> plId = new HashMap<String, String>();
			plId.put("id", adapter.data.getPlaylistId(position-1));
			createPlaylistTask = (CreatePlaylistTask) new CreatePlaylistTask().execute(plId);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case REQUEST_AUTH_DIAG:
			ProgressDialog rad = new ProgressDialog(this);
			rad.setMessage("Requesting authorization from YouTube...");
			rad.setIndeterminate(true);
			rad.setCancelable(true);
			return rad;
		case ACCOUNTS_DIAG:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Select a Google account");

			final String[] names = YouTubeComm.getComm().getAccountsNames();
			builder.setItems(names, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {					
					new RequestAuthorizeTask().execute(names[which]);
				}
			});
			return builder.create();			
		case CREATE_PLAYLIST_DIAG:
			ProgressDialog cpd = new ProgressDialog(this);
			cpd.setMessage("Creating playlist on YouTube...");
			cpd.setIndeterminate(true);
			cpd.setCancelable(true);
			cpd.setOnCancelListener(this);		
			return cpd;	
		case FETCH_VIDEO_IDS_DIAG:
			progressDiag = new ProgressDialog(this);
			progressDiag.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDiag.setMessage("Fetching videos...");
			progressDiag.setCancelable(true);
			progressDiag.setOnCancelListener(this);				
			return progressDiag;
		case ADD_VIDEOS_PLAYLIST_DIAG:
			progressDiag = new ProgressDialog(this);
			progressDiag.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDiag.setMessage("Adding videos to playlist...");
			progressDiag.setCancelable(true);
			progressDiag.setOnCancelListener(this);				
			return progressDiag;			
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
	            		    		
	            		Toast.makeText(CreatePlaylistYoutubeActivity.this, 
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
	            	
	            	createPlaylistTask = (CreatePlaylistTask) new CreatePlaylistTask().execute(plName);	            	
	            }
	        }); 
			
			return dialog;			
			
		}	
		
		return null;
	}
	
	@Override
	public void onCancel(DialogInterface dialog) {
		if(createPlaylistTask  != null)
			createPlaylistTask.cancel(true);
		
		Toast.makeText(getApplicationContext(), getString(R.string.op_cancel_str), Toast.LENGTH_SHORT).show();
		finish();		
	}		

	@Override
	public Object onRetainNonConfigurationInstance() {
		return adapter.data;
	}
}
