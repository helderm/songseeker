package com.android.songseeker.comm.youtube;


import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManagerCallback;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.comm.ServiceCommException.ServiceErr;
import com.android.songseeker.comm.ServiceCommException.ServiceID;
import com.android.songseeker.data.UserPlaylistsData;
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
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.Key;

public class YouTubeComm {
	
	private static YouTubeComm comm = new YouTubeComm();
	private static YouTubeClient client = comm.new YouTubeClient();
	private static GoogleAccountManager accountManager = null;
	private static GoogleAccessProtectedResource accessProtectedResource = null;
	
	private static String accountName = null;
	
	private static final String AUTH_TOKEN_TYPE = "Youtube";
	private static final String PREF_ACCESSTOKEN = "prefs.google.accesstoken";
	private static final String PREF_ACCOUNTNAME = "prefs.google.accountname";
	
	private YouTubeComm(){}
	
	public static YouTubeComm getComm(Context c, SharedPreferences settings){
		if(accountManager == null)
			accountManager = new GoogleAccountManager(c);
		
		if(accessProtectedResource == null){
			accessProtectedResource = new GoogleAccessProtectedResource(settings.getString(PREF_ACCESSTOKEN, null));
		}
		
		accountName = settings.getString(PREF_ACCOUNTNAME, null);
		
		return comm;
	}
	
	public static YouTubeComm getComm(){
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
		
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREF_ACCESSTOKEN, token);
		editor.commit();
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
		YouTubeUrl url = YouTubeUrl.forVideosFeed();
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
		YouTubeUrl url = YouTubeUrl.forPlaylistsFeed();		
	   
		// execute GData request for the feed
	    try {
			feed = client.executeGetPlaylistFeed(url);
		} catch (IOException e) {
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.IO);
		}
	    
	    UserPlaylistsData data = new UserPlaylistsData();	    
	    for(Playlist pl : feed.items){
	    	data.addPlaylist(pl.title, pl.id);
	    }
	    
	    return data;		
	}
	
	private static class YouTubeUrl extends GoogleUrl {

		/** Whether to pretty print HTTP requests and responses. */
		private static final boolean PRETTY_PRINT = true;

		static final String ROOT_URL = "https://gdata.youtube.com/feeds/api";

		//@Key("q")
		//String query;

		//@Key("max-results")
		//Integer maxResults = 1;

		YouTubeUrl(String encodedUrl) {
			super(encodedUrl);
			this.alt = "jsonc";
			this.prettyprint = PRETTY_PRINT;
		}

		private static YouTubeUrl root() {
			return new YouTubeUrl(ROOT_URL);
		}

		static YouTubeUrl forVideosFeed() {
			YouTubeUrl result = root();
			result.getPathParts().add("videos");			
			return result;
		}
		
		static YouTubeUrl forPlaylistsFeed(){
			YouTubeUrl result = root();
			result.getPathParts().add("users");
			result.getPathParts().add("default");
			result.getPathParts().add("playlists");
			return result;
		}
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
					headers.setApplicationName("Google-YouTubeSample/1.0");
					headers.gdataVersion = "2";
					
					//set access token
					headers.setGoogleLogin(accessProtectedResource.getAccessToken());
					request.setHeaders(headers);
					request.addParser(parser);
				}
			});
		}

		public VideoFeed executeGetVideoFeed(YouTubeUrl url) throws IOException {
			return executeGetFeed(url, VideoFeed.class);
		}
		
		public PlaylistFeed executeGetPlaylistFeed(YouTubeUrl url) throws IOException{
			return executeGetFeed(url, PlaylistFeed.class);
		}

		private <F extends Feed<? extends Item>> F executeGetFeed(YouTubeUrl url, Class<F> feedClass)
				throws IOException {
			HttpRequest request = requestFactory.buildGetRequest(url);
			return request.execute().parseAs(feedClass);
		}
		
		
	}
	


	
	
}
