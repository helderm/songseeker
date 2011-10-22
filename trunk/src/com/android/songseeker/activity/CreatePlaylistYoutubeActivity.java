package com.android.songseeker.activity;

import java.io.IOException;
import java.util.ArrayList;

import com.android.songseeker.R;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.comm.youtube.VideoFeed;
import com.android.songseeker.comm.youtube.YouTubeComm;
import com.android.songseeker.data.ArtistsParcel;
import com.android.songseeker.data.SongNamesParcel;
import com.android.songseeker.data.UserPlaylistsData;
import com.android.songseeker.util.Util;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class CreatePlaylistYoutubeActivity extends ListActivity implements AccountManagerCallback<Bundle> {
	
	private static final int ACCOUNTS_DIAG = 0;
	private static final int REQUEST_AUTH_DIAG = 1;
	private static final int CREATE_PLAYLIST_DIAG = 2;
	private static final int FETCH_SONG_IDS_DIAG = 3;
	private ProgressDialog fetchSongIdsDiag;
	
	private YouTubePlaylistsAdapter adapter;
	
	public static final int REQUEST_AUTHENTICATE = 0;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.listview);

        getListView().setEmptyView(findViewById(R.id.empty));
		
        adapter = new YouTubePlaylistsAdapter();
        setListAdapter(adapter);
        
		SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
		if(!YouTubeComm.getComm(this, settings).isAuthorized())
			showDialog(ACCOUNTS_DIAG);
		else{
			//new CreatePlaylistTask().execute();	
			new GetUserPlaylistsTask().execute();
		}
				
		//accountManager = new GoogleAccountManager(this);
		//gotAccount(false);
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
			cpd.setCancelable(false);
			return cpd;	
		case FETCH_SONG_IDS_DIAG:
			fetchSongIdsDiag = new ProgressDialog(this);
			fetchSongIdsDiag.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			fetchSongIdsDiag.setMessage("Fetching video data...");
			fetchSongIdsDiag.setCancelable(false);			
			return fetchSongIdsDiag;
		}
		
		return null;
	}

	/*void gotAccount(boolean tokenExpired) {
		SharedPreferences settings = getSharedPreferences(PREF, 0);
		String accountName = settings.getString("accountName", null);
		Account account = accountManager.getAccountByName(accountName);
		if (account != null) {
			if (tokenExpired) {
				accountManager.invalidateAuthToken(accessProtectedResource.getAccessToken());
				accessProtectedResource.setAccessToken(null);
			}
			gotAccount(account);
			return;
		}
		showDialog(ACCOUNTS_DIAG);
	}

	void gotAccount(final Account account) {
		SharedPreferences settings = getSharedPreferences(PREF, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("accountName", account.name);
		editor.commit();
		accountManager.manager.getAuthToken(
				account, AUTH_TOKEN_TYPE, true, new AccountManagerCallback<Bundle>() {

					public void run(AccountManagerFuture<Bundle> future) {
						try {
							Bundle bundle = future.getResult();
							if (bundle.containsKey(AccountManager.KEY_INTENT)) {
								Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
								intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivityForResult(intent, REQUEST_AUTHENTICATE);
							} else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
								accessProtectedResource.setAccessToken(
										bundle.getString(AccountManager.KEY_AUTHTOKEN));
								onAuthToken();
							}
						} catch (Exception e) {
							handleException(e);
						}
					}
				}, null);
	}*/

	void onAuthToken() {
		Log.i(Util.APP, "onAuthToken()");
	}

	void handleException(Exception e){
		if (e instanceof HttpResponseException) {
			HttpResponse response = ((HttpResponseException) e).getResponse();
			int statusCode = response.getStatusCode();
			try {
				response.ignore();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			if (statusCode == 401 || statusCode == 403) {	          
				SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
				YouTubeComm.getComm().unauthorizeUser(settings);
				return;
			}
			try {
				Log.e(Util.APP, response.parseAsString());
			} catch (IOException parseException) {
				parseException.printStackTrace();
			}
		}
		Log.e(Util.APP, e.getMessage(), e);

	}

	@Override
	public void run(AccountManagerFuture<Bundle> future) {
		try {
			Bundle bundle = future.getResult();
			if (bundle.containsKey(AccountManager.KEY_INTENT)) {
				Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
				intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivityForResult(intent, REQUEST_AUTHENTICATE);
			} else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
				SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
				YouTubeComm.getComm().setAccessToken(bundle.getString(AccountManager.KEY_AUTHTOKEN), settings);
				
				onAuthToken();
			}
		} catch (Exception e) {
			handleException(e);
		}
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.i(Util.APP, "onActivityResult()");

		//TODO Check!
		switch (requestCode) {
		case REQUEST_AUTHENTICATE:
			if (resultCode == RESULT_OK) {
				//gotAccount(false);
			} else {
				//showDialog(ACCOUNTS_DIAG);				
				CreatePlaylistYoutubeActivity.this.finish();
			}
			break;
		}
	}

	private class YouTubePlaylistsAdapter extends BaseAdapter {
		UserPlaylistsData data;
		
		public YouTubePlaylistsAdapter() {
			data = null;
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
			    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			    v = vi.inflate(R.layout.list_row, null);
			}			 

			TextView tt = (TextView) v.findViewById(R.id.firstLine);
		    TextView bt = (TextView) v.findViewById(R.id.secondLine);
		    
		    ImageView img = (ImageView) v.findViewById(R.id.coverart);
		    img.setImageResource(R.drawable.ic_menu_database);	
		    
		    ImageView playPause = (ImageView) v.findViewById(R.id.playpause);
		    playPause.setVisibility(View.GONE);
		    
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
			
			SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
			
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
				YouTubeComm.getComm().createPlaylist();
				
				data = YouTubeComm.getComm().getPlaylistFeed();
			} catch(ServiceCommException e){
				err = e.getMessage();
				return null;
			}
			
			return data;
		}
		
		@Override
		protected void onPostExecute(UserPlaylistsData data) {
			if(err != null){
				Toast.makeText(CreatePlaylistYoutubeActivity.this, "Unable to fetch the user playlists...", Toast.LENGTH_SHORT).show();
				return;
			}
			
			adapter.setUserData(data);
		}
		
	}
	
	private class CreatePlaylistTask extends AsyncTask<Void, Integer, Void>{
		private SongNamesParcel sn = getIntent().getExtras().getParcelable("songNames");
		private ArtistsParcel ar = getIntent().getExtras().getParcelable("songArtists");
		private String err;
		
		@Override
		protected void onPreExecute() {
			showDialog(FETCH_SONG_IDS_DIAG);
			fetchSongIdsDiag.setMax(sn.getSongNames().size());		
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			if(progress[0] >= 0)
				fetchSongIdsDiag.setProgress(progress[0]);
			else{
				removeDialog(FETCH_SONG_IDS_DIAG);
				//showDialog(CREATE_PLAYLIST_DIAG);
			}
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			//String plName = params[0].get("name");
			//int plId = 0;			
			
			//SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
			try{				
				//if(plName != null){
				//	Playlist pl = LastfmComm.getComm().createPlaylist(plName, settings);
				//	plId = pl.getId();
				//}else{
				//	plId = Integer.parseInt(params[0].get("id"));
				//}
				
				//publishProgress(-1);
				
				YouTubeComm.getComm().getPlaylistFeed();
				
				int i;
				ArrayList<String> songNames = sn.getSongNames();
				ArrayList<String> songArtists = ar.getArtistList();
				
				for(i=0; i<songNames.size() && i<songArtists.size(); i++){
					VideoFeed video = YouTubeComm.getComm().getVideoFeed(songNames.get(i) + "-" + songArtists.get(i));
					publishProgress(i+1);
					
					if(video.totalItems <= 0){
						Log.w(Util.APP, "Song ["+songNames.get(i) + "-" + songArtists.get(i)+"] not found on YouTube!");
						continue;
					}
					
					Log.i(Util.APP, "Song ["+video.items.get(0).title+" ("+ video.items.get(0).player.defaultUrl +")]");
				}
				publishProgress(-1);
			}catch (ServiceCommException e){
				err = e.getMessage();
				return null;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			//removeDialog(ADD_TRACKS_DIAG);
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(getApplicationContext(), 
						getResources().getText(R.string.pl_created_str), Toast.LENGTH_SHORT).show();
			}
			
			CreatePlaylistYoutubeActivity.this.finish();
		}
		
	}
	

}
