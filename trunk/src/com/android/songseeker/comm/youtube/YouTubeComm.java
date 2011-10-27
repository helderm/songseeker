package com.android.songseeker.comm.youtube;


import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import android.accounts.Account;
import android.accounts.AccountManagerCallback;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.comm.ServiceCommException.ServiceErr;
import com.android.songseeker.comm.ServiceCommException.ServiceID;
import com.android.songseeker.comm.youtube.Urls.YouTubePlaylistUrl;
import com.android.songseeker.comm.youtube.Urls.YouTubeVideoUrl;
import com.android.songseeker.data.UserPlaylistsData;
import com.android.songseeker.util.Util;
import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.GoogleUrl;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.googleapis.json.JsonCParser;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

public class YouTubeComm {

	protected static final String DEVKEY = "AI39si6yAoZ7f6EjzMCtIex34tdlMped4mK3ZL9vg-8pn8ZXTrvhKd6_5VfR-J-GwJyFQC4lPm6YAnH0V-zSvsgAcJ-xlT0ZUg"; 
	private static YouTubeComm comm = new YouTubeComm();

	private static final String LOGIN_ENDPOINT = "https://www.google.com/youtube/accounts/ClientLogin";
	//private static final String LOGIN_ENDPOINT = "https://www.google.com/accounts/AuthSubRequest?next=oauth://checkin4me" +
	//												"&scope=http://gdata.youtube.com&session=1&secure=0";
	private static final String GET_PLAYLISTS_ENDPOINT = "https://gdata.youtube.com/feeds/api/users/default/playlists?v=2&alt=jsonc";
	private static final String CREATE_PLAYLIST_ENDPOINT = "https://gdata.youtube.com/feeds/api/users/default/playlists?alt=jsonc";
	private static final String ADD_VIDEO_ENDPOINT = "https://gdata.youtube.com/feeds/api/playlists/";
	private static final String GET_VIDEOS_ENDPOINT = "https://gdata.youtube.com/feeds/api/videos?alt=jsonc&v=2";

	private static String session = null;

	private YouTubeComm(){}

	public static YouTubeComm getComm(){
		return comm;
	}

	public void requestAuthorize(String username, String password) throws ServiceCommException{		
		
		HttpPost request = new HttpPost(LOGIN_ENDPOINT);
		HttpResponse response;		
		List<NameValuePair> request_args = new ArrayList<NameValuePair>();		

		request_args.add(new BasicNameValuePair("Email", username));
		request_args.add(new BasicNameValuePair("Passwd", password));
		request_args.add(new BasicNameValuePair("service", "youtube"));	
		request_args.add(new BasicNameValuePair("source", Util.APP));				

		try{		
			StringEntity body = new StringEntity(URLEncodedUtils.format(request_args, "UTF-8"));
			body.setContentType("application/x-www-form-urlencoded");
			request.setEntity(body);

			HttpClient httpClient = new DefaultHttpClient();
			response = httpClient.execute(request);

			if(response.getStatusLine().getStatusCode() != 200) {

				//unauthorized
				if(response.getStatusLine().getStatusCode() == 403){
					//cleanAuthTokens(settings);
					throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.NOT_AUTH);
				}

				Log.e(Util.APP, "HTTP client returned code different from 200! code: "+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
				throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.REQ_FAILED);
			}   

			String r_string = EntityUtils.toString(response.getEntity());

			//seach for the auth session key
			String[] tokens = r_string.split("[=\n]");
			if(tokens.length < 2)
				throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.NOT_AUTH);

			for(int i=0; i<tokens.length-1; i++){
				String token = tokens[i];
				if(token.equalsIgnoreCase("auth") && tokens[i+1] != null){
					session = tokens[i+1];

					return;
				}
			}

			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.NOT_AUTH);
		}catch (IOException ex){
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.IO);
		}catch (ServiceCommException e){
			throw e;
		}catch (Exception e){
			Log.e(Util.APP, "Unknown error while trying to login into YouTube!", e);
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.UNKNOWN);
		}		
	}

	public UserPlaylistsData getUserPlaylists(SharedPreferences settings) throws ServiceCommException{

		UserPlaylistsData data = new UserPlaylistsData();		

		try{
			HttpGet request = new HttpGet(GET_PLAYLISTS_ENDPOINT);
			request.setHeader("Authorization", "GoogleLogin auth="+accessProtectedResource.getAccessToken());
			request.setHeader("X-GData-Key", "key="+DEVKEY);
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(request);

			if(response.getStatusLine().getStatusCode() != 200) {

				switch(response.getStatusLine().getStatusCode()){
				
				case 401:
				case 403:
					//unauthorized
					unauthorizeUser(settings);
					throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.NOT_AUTH);
					
				default:
					Log.e(Util.APP, "HTTP client returned code different from 200! code: "+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
					throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.REQ_FAILED);
				}
			}   

			String jsonString = EntityUtils.toString(response.getEntity());	
			JSONParser parser = new JSONParser();

			//parse the response
			JSONObject result = (JSONObject)((JSONObject)parser.parse(jsonString)).get("data");
			Long totalItems = (Long)result.get("totalItems");
			if(totalItems <= 0)
				return data;
			
			JSONArray array = (JSONArray) result.get("items");		

			for(int i=0; i<array.size(); i++){
				JSONObject pl = (JSONObject) array.get(i);

				String id = (String) pl.get("id");
				String name = (String) pl.get("title");
				Long numSongs = (Long) pl.get("size");

				data.addPlaylist(name, numSongs.intValue(), id);				
			} 			

			return data;

		}catch (IOException ex){
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.IO);
		}catch (ServiceCommException e){
			throw e;
		}catch (Exception e){
			Log.e(Util.APP, "Unknown error while trying to login into YouTube!", e);
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.UNKNOWN);
		}	
	}

	public String createPlaylist(String name) throws ServiceCommException{

		try{
			LinkedHashMap<String, Object> args = new LinkedHashMap<String, Object>();

			LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
			data.put("title", name);
			data.put("description", "Playlist created by Song Seeker app for Android");
			args.put("data", data);	

			HttpPost request = new HttpPost(CREATE_PLAYLIST_ENDPOINT);
			request.setHeader("Authorization", "GoogleLogin auth="+accessProtectedResource.getAccessToken());
			request.setHeader("X-GData-Key", "key="+DEVKEY);
			request.setHeader("GData-Version", "2");			
			
			StringEntity body = new StringEntity(JSONValue.toJSONString(args));
			body.setContentType("application/json");
			request.setEntity(body);			
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(request);
			
			if(response.getStatusLine().getStatusCode() != 201) {

				//unauthorized
				if(response.getStatusLine().getStatusCode() == 403){
					//cleanAuthTokens(settings);
					throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.NOT_AUTH);
				}

				Log.e(Util.APP, "HTTP client returned code different from 200! code: "+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
				throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.REQ_FAILED);
			}   

			String jsonString = EntityUtils.toString(response.getEntity());	
			JSONParser parser = new JSONParser();

			//parse the response
			JSONObject result = (JSONObject)((JSONObject)parser.parse(jsonString)).get("data");
			String id = (String) result.get("id");
			if(id == null)
				throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.REQ_FAILED);
			
			return id;			
		}catch (IOException ex){
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.IO);
		}catch (ServiceCommException e){
			throw e;
		}catch (Exception e){
			Log.e(Util.APP, "Unknown error while trying to login into YouTube!", e);
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.UNKNOWN);
		}	
	}

	public VideoFeed searchVideo(String songName, String artistName) throws ServiceCommException{
		try{
			String query = songName + "+" + artistName;
			
			HttpGet request = new HttpGet(GET_VIDEOS_ENDPOINT+ "&q="+ query.replace(' ', '+')+ "&max-results=1");
			//request.setHeader("Authorization", "GoogleLogin auth="+session);
			//request.setHeader("X-GData-Key", "key="+DEVKEY);
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(request);

			if(response.getStatusLine().getStatusCode() != 200) {

				//unauthorized
				//if(response.getStatusLine().getStatusCode() == 403){
					//cleanAuthTokens(settings);
				//	throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.NOT_AUTH);
				//}

				Log.e(Util.APP, "HTTP client returned code different from 200! code: "+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
				throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.REQ_FAILED);
			}   

			String jsonString = EntityUtils.toString(response.getEntity());	
			JSONParser parser = new JSONParser();

			//parse the response
			JSONObject result = (JSONObject)((JSONObject)parser.parse(jsonString)).get("data");
			Long totalItems = (Long)result.get("totalItems");
			if(totalItems <= 0)
				throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.ID_NOT_FOUND);
			
			JSONArray array = (JSONArray) result.get("items");	
			VideoFeed video = new VideoFeed();
			
			for(int i=0; i<array.size(); i++){
				JSONObject pl = (JSONObject) array.get(i);

				video = new VideoFeed();				
				video.id = (String) pl.get("id");
				video.title = (String) pl.get("title");
				video.description = (String) pl.get("description");
				video.image = (String)((JSONObject) pl.get("thumbnail")).get("sqDefault");				
			} 			

			return video;

		}catch (IOException ex){
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.IO);
		}catch (ServiceCommException e){
			throw e;
		}catch (Exception e){
			Log.e(Util.APP, "Unknown error while trying to login into YouTube!", e);
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.UNKNOWN);
		}	
	}			
	
	
	public void addVideosToPlaylist(String playlistID, String videoID) throws ServiceCommException{
		
		try{
			LinkedHashMap<String, Object> args = new LinkedHashMap<String, Object>();
			LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
			
			LinkedHashMap<String, String> video = new LinkedHashMap<String, String>();
			video.put("id", videoID);			
			data.put("video", video);
			args.put("data", data);	

			HttpPost request = new HttpPost(ADD_VIDEO_ENDPOINT+playlistID+"?alt=jsonc");
			request.setHeader("Authorization", "GoogleLogin auth="+accessProtectedResource.getAccessToken());
			request.setHeader("X-GData-Key", "key="+DEVKEY);
			request.setHeader("GData-Version", "2");			
			
			String bla = JSONValue.toJSONString(args);
			StringEntity body = new StringEntity(bla);
			body.setContentType("application/json");
			request.setEntity(body);			
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(request);
			
			if(response.getStatusLine().getStatusCode() != 201) {

				//unauthorized
				if(response.getStatusLine().getStatusCode() == 403){
					//cleanAuthTokens(settings);
					throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.NOT_AUTH);
				}

				Log.e(Util.APP, "HTTP client returned code different from 200! code: "+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
				throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.REQ_FAILED);
			}   
		}catch (IOException ex){
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.IO);
		}catch (ServiceCommException e){
			throw e;
		}catch (Exception e){
			Log.e(Util.APP, "Unknown error while trying to login into YouTube!", e);
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.UNKNOWN);
		}	
		
	}
	
	public class VideoFeed{
		public String id;
		public String title;
		public String description;
		public String image;	
	}
	
	
	/**************************************************************************************/



	private static YouTubeClient client = comm.new YouTubeClient();
	private static GoogleAccountManager accountManager = null;
	private static GoogleAccessProtectedResource accessProtectedResource = null;

	private static String accountName = null;

	private static final String AUTH_TOKEN_TYPE = "Youtube";
	private static final String PREF_ACCESSTOKEN = "prefs.google.accesstoken";
	private static final String PREF_ACCOUNTNAME = "prefs.google.accountname";



	public static YouTubeComm getComm(Context c, SharedPreferences settings){
		if(accountManager == null)
			accountManager = new GoogleAccountManager(c);

		if(accessProtectedResource == null){
			accessProtectedResource = new GoogleAccessProtectedResource(settings.getString(PREF_ACCESSTOKEN, null));
		}

		accountName = settings.getString(PREF_ACCOUNTNAME, null);

		return comm;
	}



	public boolean isAuthorized(){
		if(accessProtectedResource.getAccessToken() != null && accountName != null)
			return true;

		return false;		 
	}

	public String[] getAccountsNames(){

		final Account[] accounts = accountManager.getAccounts();
		final int size = accounts.length;

		String[] names = new String[size];
		for (int i = 0; i < size; i++) {
			names[i] = accounts[i].name;
		}

		return names;
	}

	public void requestAuthorize(String accountName, AccountManagerCallback<Bundle> callback, 
			SharedPreferences settings){

		Account account = accountManager.getAccountByName(accountName);		
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREF_ACCOUNTNAME, account.name);
		editor.commit();

		//TODO Debug
		accountManager.invalidateAuthToken(accessProtectedResource.getAccessToken());
		accessProtectedResource.setAccessToken(null);

		accountManager.manager.getAuthToken(account, AUTH_TOKEN_TYPE, true, callback, null);
	}

	public void setAccessToken(String token, SharedPreferences settings){
		accessProtectedResource.setAccessToken(token);

		//SharedPreferences.Editor editor = settings.edit();
		//editor.putString(PREF_ACCESSTOKEN, token);
		//editor.commit();
	}

	public void unauthorizeUser(SharedPreferences settings){
		if(!isAuthorized())
			return;

		accountManager.invalidateAuthToken(accessProtectedResource.getAccessToken());
		accessProtectedResource.setAccessToken(null);

		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREF_ACCOUNTNAME, null);
		editor.putString(PREF_ACCESSTOKEN, null);
		editor.commit();
	}

	public VideoFeed getVideoFeed(String query) throws ServiceCommException{
		VideoFeed feed;
		YouTubeVideoUrl url = YouTubeVideoUrl.forVideosFeed();
		//url.query = query;

		// execute GData request for the feed
		try {
			feed = client.executeGetVideoFeed(url);
		} catch (IOException e) {
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.IO);
		}

		return feed;
	}

	public UserPlaylistsData getPlaylistFeed() throws ServiceCommException{
		PlaylistFeed feed;
		YouTubePlaylistUrl url = YouTubePlaylistUrl.forPlaylistsFeed();		

		// execute GData request for the feed
		try {
			feed = client.executeGetPlaylistFeed(url);
		} catch (IOException e) {
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.IO);
		}

		UserPlaylistsData data = new UserPlaylistsData();	    
		for(Playlist pl : feed.items){
			data.addPlaylist(pl.title, pl.size, pl.id);
		}

		return data;		
	}

	public Playlist createPlaylist() throws ServiceCommException{
		Playlist pl;
		YouTubePlaylistUrl url = YouTubePlaylistUrl.forPlaylistsFeed();	

		pl = new Playlist();
		pl.title = "Teste";
		pl.description = "Playlist created by Song Seeker for Android";
		//pl.summary = "Playlist created by Song Seeker for Android";

		try {
			pl = client.executePostPlaylistFeed(pl, url);
		} catch (IOException e) {
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.IO);
		}

		return pl;
	}

	private class YouTubeClient {

		private final JsonFactory jsonFactory = new JacksonFactory();
		private final HttpTransport transport = new NetHttpTransport();
		private final HttpRequestFactory requestFactory;

		public YouTubeClient() {
			final JsonCParser parser = new JsonCParser(jsonFactory);
			requestFactory = transport.createRequestFactory(new HttpRequestInitializer() {

				@Override
				public void initialize(HttpRequest request) {
					// headers
					GoogleHeaders headers = new GoogleHeaders();
					//headers.setApplicationName(Util.APP);
					headers.gdataVersion = "2";
					headers.setDeveloperId(DEVKEY);

					//set access token
					headers.setGoogleLogin(accessProtectedResource.getAccessToken());
					request.setHeaders(headers);
					request.addParser(parser);
				}
			});
		}

		public VideoFeed executeGetVideoFeed(GoogleUrl url) throws IOException {
			return null;
			//return executeGetFeed(url, VideoFeed.class);
		}

		public PlaylistFeed executeGetPlaylistFeed(GoogleUrl url) throws IOException{
			return executeGetFeed(url, PlaylistFeed.class);
		}		

		private <F extends Feed<? extends Item>> F executeGetFeed(GoogleUrl url, Class<F> feedClass)
				throws IOException {
			HttpRequest request = requestFactory.buildGetRequest(url);
			return request.execute().parseAs(feedClass);
		}

		public Playlist executePostPlaylistFeed(Playlist pl, GoogleUrl url) throws IOException{
			return executePostRequest(url, pl, Playlist.class);
		}

		private <F extends Item> F executePostRequest(GoogleUrl url, F data, Class<F> feedClass) throws IOException{
			JsonHttpContent result = new JsonHttpContent(jsonFactory, data.toContent(jsonFactory));

			HttpRequest request = requestFactory.buildPostRequest(url, result);

			return request.execute().parseAs(feedClass);
		}

	}	
}
