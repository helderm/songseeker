package com.android.songseeker.activity;

import java.io.InputStreamReader;
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
import com.android.songseeker.comm.EchoNestComm;
import com.android.songseeker.comm.RdioComm;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.data.SongList;
import com.android.songseeker.util.Util;
import com.echonest.api.v4.Song;
import com.echonest.api.v4.SongParams;

public class CreatePlaylistRdioActivity extends Activity{
	
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
	private static final int FETCH_SONG_IDS_DIAG = 2;
	private static final int CREATE_PLAYLIST_DIAG = 3;
	private ProgressDialog fetchSongIdsDiag;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
		//accessToken = settings.getString(PREF_ACCESSTOKEN, null);
		//accessTokenSecret = settings.getString(PREF_ACCESSTOKENSECRET, null);

		if(!RdioComm.getComm(settings).isAuthorized()) {
			//If either one is null, reset both of them
			//accessToken = accessTokenSecret = null;
					
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

			//String authUrl = null;

			//Log.d(Util.APP,"Fetching request token from Rdio...");

			try {
				//authUrl = provider.retrieveRequestToken(consumer, "oauth://checkin4me");
				RdioComm.getComm().requestAuthorize(CreatePlaylistRdioActivity.this);
			} catch (ServiceCommException e) {
				Log.e(Util.APP, "Unable to request access to Rdio!", e);
				err = e.getMessage();
				return false;
			} 

			//Log.d(Util.APP, "Request token: " + consumer.getToken());
			//Log.d(Util.APP, "Token secret: " + consumer.getTokenSecret());
			//Log.d(Util.APP, "AuthURL: " + authUrl);

			//TODO: Check how can we start this activity without adding it to the call stack
			//Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
			//i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			//i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			//Log.d(Util.APP, "Requesting permission to Rdio... ");
			//startActivity(i);		

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

	private class CreatePlaylistTask extends AsyncTask<Void, Integer, Void>{
		
		private SongList sl = getIntent().getExtras().getParcelable("songList");
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
					song = EchoNestComm.getComm().getSongs(sp);
					
					String rdioID = song.getString("foreign_ids[0].foreign_id");
					
					String[] split = rdioID.split(":");
					songIDs.add(split[2]);
					Log.d(Util.APP, "RdioID = ["+split[2]+"]");
				} catch(ServiceCommException e) {
					err = e.getMessage();
					return null;
				} catch(IndexOutOfBoundsException e){
					//TODO not found in EchoNest, call Rdio
					if(song != null){
						Log.w(Util.APP, "Song ["+ song.getReleaseName()+" - " +song.getArtistName()+"] not found!");
						
						try{
							songIDs.add(queryTrackID(song.getReleaseName(), song.getArtistName()));							
						}catch(Exception ex){
							Log.e(Util.APP, "Err while fetching track data from Rdio!", ex);
						}
					}					
				}
				
				publishProgress(++count);
			}
			
			Log.i(Util.APP, "SongIDs fetched! Creating playlist...");
			
			//show cratePlaylist diag
			publishProgress(-1);
			
			try{
				createPlaylist(songIDs);
			}catch(Exception e){
				Log.e(Util.APP, "Error while creating playlist!", e);
				err = "Error while creating playlist!";				
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
	
	@Override
    protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Uri uri = intent.getData();

		removeDialog(REQUEST_AUTH_DIAG);

		Log.d(Util.APP, "OAuth callback started!");
		
		//Verificando se a chamada vem realmente do callback esperado
		if (uri != null && uri.toString().contains("oauth")) {
			
			//TODO Check! If the verifier comes null in a 'Deny' operation, we can use that to return an err msg to the user!
			String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
			try {
				
				Log.d(Util.APP, "Verifier: "+verifier);
				
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
	
	
	private void createPlaylist(List<String> songIDs) throws Exception{

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
		
		StringEntity body = new StringEntity(URLEncodedUtils.format(request_args, "UTF-8"));
		body.setContentType("application/x-www-form-urlencoded");
		request.setEntity(body);

		consumer.setTokenWithSecret(accessToken, accessTokenSecret);
		
		Log.d(Util.APP, "AcessToken: "+consumer.getToken());
		Log.d(Util.APP, "AcessTokenSecret: "+consumer.getTokenSecret());			
		consumer.sign(request);

		Log.d(Util.APP,"sending createPlaylist request to Rdio");

		HttpClient httpClient = new DefaultHttpClient();
		response = httpClient.execute(request);	
        
        Log.d(Util.APP,"Response: " + response.getStatusLine().getStatusCode() + " "
                + response.getStatusLine().getReasonPhrase());

        if (response.getStatusLine().getStatusCode() != 200) {
        	Exception e = new Exception("HTTP client returned code different from 200! code: "+response.getStatusLine().getStatusCode()+" - "+
        																						response.getStatusLine().getReasonPhrase());
        	throw e;
        }               
        
        //TODO check 'status'
        
        Log.i(Util.APP, "Playlist created with success!");
	}

	
	private String queryTrackID(String songName, String songArtist) throws Exception{
		HttpPost request = new HttpPost(ENDPOINT);
		HttpResponse response;
		int start_index, end_index;
				
		List<NameValuePair> request_args = new ArrayList<NameValuePair>();
		request_args.add(new BasicNameValuePair("method", "search"));
		request_args.add(new BasicNameValuePair("types", "Track"));
		//request_args.add(new BasicNameValuePair("never_or", "false"));
		request_args.add(new BasicNameValuePair("count", "1"));
		
		request_args.add(new BasicNameValuePair("query", songName+" "+songArtist));	
		
		StringEntity body = new StringEntity(URLEncodedUtils.format(request_args, "UTF-8"));
		body.setContentType("application/x-www-form-urlencoded");
		request.setEntity(body);

		consumer.setTokenWithSecret(accessToken, accessTokenSecret);	
		consumer.sign(request);

		Log.i(Util.APP,"sending search request to Rdio...");
		Log.d(Util.APP, "songName=["+songName+"], songArtist=["+songArtist+"]");

		HttpClient httpClient = new DefaultHttpClient();
		response = httpClient.execute(request);	
        
        Log.d(Util.APP,"Response: " + response.getStatusLine().getStatusCode() + " "
                + response.getStatusLine().getReasonPhrase());

        if (response.getStatusLine().getStatusCode() != 200) {
        	Exception e = new Exception(response.getStatusLine().getReasonPhrase());
        	throw e;
        }               
		
    	InputStreamReader reader = null;
		reader = new InputStreamReader(response.getEntity().getContent());
		
       	char[] buf = new char[2*1024];
    	if (reader.read(buf) < 0) return null;
    	
    	String str = new String(buf);
    	buf = null;
    	
    	//parse for status OK
    	start_index = str.indexOf("\"status\": ");
    	start_index += 11; 
    	end_index = str.indexOf('\"', start_index);    	
    	char[] bufOk = new char[end_index-start_index];  
    	str.getChars(start_index, end_index, bufOk, 0);
    	String bufStrOK = new String(bufOk);    	    	
    	if(!bufStrOK.equalsIgnoreCase("ok")){
    		throw new Exception("Rdio search req returned status NOT OK! status:" +bufOk.toString());
    	}
    	bufOk = null;
    	bufStrOK = null;		
    	
    	//parse for track_count > 0
    	start_index = str.indexOf("\"track_count\": ");
    	start_index += 15; 
    	end_index = str.indexOf(',', start_index);    	
    	char[] bufCount = new char[end_index-start_index];  
    	str.getChars(start_index, end_index, bufCount, 0);
    	String bufStrCount = new String(bufCount);
    	if(Integer.parseInt(bufStrCount) <= 0){
    		throw new Exception("Rdio search req returned no tracks!");
    	}    	
    	bufStrCount = null;
    	bufCount = null;
    	
    	//parse the track id
    	start_index = str.indexOf("\"key\": ");
    	start_index += 8;    	
    	end_index = str.indexOf('\"', start_index);    	
    	char[] buf2 = new char[end_index-start_index];  
    	str.getChars(start_index, end_index, buf2, 0);
    	
    	String retString = new String(buf2);
    	buf2 = null;
    	Log.d(Util.APP, "RdioID: " + retString);
    	return retString;
		
	}
	
}
