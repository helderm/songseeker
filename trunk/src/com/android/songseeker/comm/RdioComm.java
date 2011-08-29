package com.android.songseeker.comm;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.songseeker.R;
import com.android.songseeker.data.SongList;
import com.android.songseeker.util.Util;
import com.echonest.api.v4.Song;
import com.echonest.api.v4.SongParams;

public class RdioComm extends Activity{
	
	private static final String CONSUMER_KEY = "e6axyuwxza2fxdhkbqbwdb2f";
	private static final String CONSUMER_SECRET = "5PcRraQyRk";		
	private static final String REQUEST_TOKEN = "http://api.rdio.com/oauth/request_token";
	private static final String ACCESS_TOKEN = "http://api.rdio.com/oauth/access_token";
	private static final String AUTHORIZE = "https://www.rdio.com/oauth/authorize";
	private static final String ENDPOINT = "http://api.rdio.com/1/";
	OAuthConsumer consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
    OAuthProvider provider = new CommonsHttpOAuthProvider(REQUEST_TOKEN, ACCESS_TOKEN, AUTHORIZE);
	
	private static String accessToken = null;
	private static String accessTokenSecret = null;	
	private static final String PREF_ACCESSTOKEN = "prefs.rdio.accesstoken";
	private static final String PREF_ACCESSTOKENSECRET = "prefs.rdio.accesstokensecret";	
	
	private static final int REQUEST_AUTH_DIAG = 1;
	private static final int CREATE_PLAYLIST = 2;
	private ProgressDialog createPlaylist_pd;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
		accessToken = settings.getString(PREF_ACCESSTOKEN, null);
		accessTokenSecret = settings.getString(PREF_ACCESSTOKENSECRET, null);

		if (accessToken == null || accessTokenSecret == null) {
			//If either one is null, reset both of them
			accessToken = accessTokenSecret = null;
			
			//requestAuthorize();
			
			new RequestAuthorizeTask().execute(null, null, null);
			
		} else {
			Log.d(Util.APP, "Found cached credentials:");
			Log.d(Util.APP, "Access token: " + accessToken);
			Log.d(Util.APP, "Access token secret: " + accessTokenSecret);
			new CreatePlaylistTask().execute(null, null, null);
		}	
		
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch(id){
		case REQUEST_AUTH_DIAG:
			ProgressDialog pd = new ProgressDialog(this);
			pd.setMessage("Requesting authorization from Rdio...");
			pd.setIndeterminate(true);
			pd.setCancelable(true);
			return pd;
		case CREATE_PLAYLIST:
			createPlaylist_pd = new ProgressDialog(this);
			createPlaylist_pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			createPlaylist_pd.setMessage("Retrieving song IDs...");
			createPlaylist_pd.setCancelable(false);			
			return createPlaylist_pd;
		default:
			return null;
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	private class RequestAuthorizeTask extends AsyncTask<Void, Void, Boolean>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			showDialog(REQUEST_AUTH_DIAG);
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {

			String authUrl = null;

			Log.d(Util.APP,"Fetching request token from Rdio...");

			// we do not support callbacks, thus pass OOB
			try {
				authUrl = provider.retrieveRequestToken(consumer, "oauth://checkin4me");
			} catch (Exception e) {
				Log.e(Util.APP, "Unable to request access to Rdio!", e);
				err = "Unable to request access to Rdio!";
				return false;
			} 

			Log.d(Util.APP, "Request token: " + consumer.getToken());
			Log.d(Util.APP, "Token secret: " + consumer.getTokenSecret());
			Log.d(Util.APP, "AuthURL: " + authUrl);

			//TODO: Check how can we start this activity without adding it to the call stack
			Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
			i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			startActivity(i);		

			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if(err != null){
				removeDialog(REQUEST_AUTH_DIAG);
				Toast.makeText(getApplicationContext(), err , Toast.LENGTH_SHORT).show();
				RdioComm.this.finish();
			}
			
		}
		
	}

	private class CreatePlaylistTask extends AsyncTask<Void, Integer, Void>{
		
		SongList sl = getIntent().getExtras().getParcelable("songList");
		
		
		@Override
		protected void onPreExecute() {
			showDialog(CREATE_PLAYLIST);
			createPlaylist_pd.setMax(sl.getSongIDs().size());
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			createPlaylist_pd.setProgress(progress[0]);
			
		}
		
		
		@Override
		protected Void doInBackground(Void... params) {
		
			List<String> songIDs = new ArrayList<String>();
					
			int count = 0;
			for(String id : sl.getSongIDs()){
				
				Log.d(Util.APP, "SongID = [" + id + "]");
				
				Song song = null;
				SongParams sp = new SongParams();
				sp.setID(id);
				sp.addIDSpace(EchoNestComm.RDIO);
				
				try {
					song = EchoNestComm.getComm().identifySongs(sp);
					
					String rdioID = song.getString("foreign_ids[0].foreign_id");
					
					String[] split = rdioID.split(":");
					songIDs.add(split[2]);
					Log.d(Util.APP, "RdioID = ["+split[2]+"]");
				} catch(ServiceCommException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch(IndexOutOfBoundsException e){
					//TODO not found in EchoNest, call Rdio
					if(song != null){
						Log.w(Util.APP, "Song ["+ song.getReleaseName()+" - " +song.getArtistName()+"] not found!");
					}					
				}
				
				publishProgress(++count);
			}
			
			Log.i(Util.APP, "SongIDs fetched! Creating playlist...");
			//createPlaylist_pd.setTitle("Creating playlist...");
			
			createPlaylist(songIDs);
			
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			removeDialog(CREATE_PLAYLIST);
		}
		
	}
	
	@Override
    protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Uri uri = intent.getData();

		removeDialog(REQUEST_AUTH_DIAG);

		//Verificando se a chamada vem realmente do callback esperado
		if (uri != null && uri.toString().contains("oauth")) {
			String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
			try {
				// Definir os tokens para obter o Access Token
				provider.retrieveAccessToken(consumer, verifier);

				accessToken = consumer.getToken();
				accessTokenSecret = consumer.getTokenSecret();

				Log.d(Util.APP, "AcessToken: "+consumer.getToken());
				Log.d(Util.APP, "AcessTokenSecret: "+consumer.getTokenSecret());
				
				SharedPreferences settings = getPreferences(MODE_PRIVATE);
				Editor editor = settings.edit();
				editor.putString(PREF_ACCESSTOKEN, accessToken);
				editor.putString(PREF_ACCESSTOKENSECRET, accessTokenSecret);
				editor.commit();

				Toast.makeText(this, getResources().getText(R.string.auth_str) , Toast.LENGTH_SHORT).show();

				
			} catch (OAuthCommunicationException e) {
				//TODO: Check how the err will be handled
				Toast.makeText(getApplicationContext(), getResources().getText(R.string.not_auth_str), Toast.LENGTH_SHORT).show();
				SharedPreferences settings = getPreferences(MODE_PRIVATE);
				Editor editor = settings.edit();
				editor.putString(PREF_ACCESSTOKEN, null);
				editor.putString(PREF_ACCESSTOKENSECRET, null);
				editor.commit();
				//RdioComm.this.finish();
			} catch(Exception e){
				Toast.makeText(getApplicationContext(), "Error while trying to authorize user!", Toast.LENGTH_SHORT).show();
				Log.e(Util.APP, "Error while trying to authorize user!", e);
				SharedPreferences settings = getPreferences(MODE_PRIVATE);
				Editor editor = settings.edit();
				editor.putString(PREF_ACCESSTOKEN, null);
				editor.putString(PREF_ACCESSTOKENSECRET, null);
				editor.commit();
				//RdioComm.this.finish();
			}
		}
	}
	
	
	private void createPlaylist(List<String> songIDs){

		StringBuilder sb = new StringBuilder();
		
		HttpPost request = new HttpPost(ENDPOINT);
		HttpResponse response;

		List<NameValuePair> request_args = new ArrayList<NameValuePair>();
		request_args.add(new BasicNameValuePair("method", "createPlaylist"));
		request_args.add(new BasicNameValuePair("name", "teste3"));
		request_args.add(new BasicNameValuePair("description", "teste_3"));
		
			
		for(String rdioID : songIDs){
			sb.append(rdioID+",");
		}
		sb.deleteCharAt(sb.length()-1);
		
		
		request_args.add(new BasicNameValuePair("tracks", sb.toString()));
		
		try{
			StringEntity body = new StringEntity(URLEncodedUtils.format(request_args, "UTF-8"));
			body.setContentType("application/x-www-form-urlencoded");
			request.setEntity(body);

			consumer.setTokenWithSecret(accessToken, accessTokenSecret);
			
			Log.d(Util.APP, "AcessToken: "+consumer.getToken());
			Log.d(Util.APP, "AcessTokenSecret: "+consumer.getTokenSecret());			
			consumer.sign(request);

			Log.d(Util.APP,"sending currentUser request to Rdio");

			HttpClient httpClient = new DefaultHttpClient();
			response = httpClient.execute(request);
		}catch(Exception e){
			Log.e(Util.APP, "Err while creating playlist!", e);
			return;
		}
        
        Log.d(Util.APP,"Response: " + response.getStatusLine().getStatusCode() + " "
                + response.getStatusLine().getReasonPhrase());

        if (response.getStatusLine().getStatusCode() == 200) {
        	/*InputStreamReader reader = null;
			try {
				reader = new InputStreamReader(response.getEntity().getContent());
			} catch (IllegalStateException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	while(true) {
        		try {
                	char[] buf = new char[64*1024];
                	if (reader.read(buf) < 0) break;
                	Log.d(Util.APP, new String(buf));
        		} catch(EOFException ex) {
        			break;
        		} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}*/
        }
        Log.d(Util.APP,"");
        
	}
}
