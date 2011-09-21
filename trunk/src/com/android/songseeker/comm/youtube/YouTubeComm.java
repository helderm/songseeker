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
import com.android.songseeker.comm.youtube.Urls.YouTubePlaylistUrl;
import com.android.songseeker.comm.youtube.Urls.YouTubeVideoUrl;
import com.android.songseeker.data.UserPlaylistsData;
import com.android.songseeker.util.Util;
import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.GoogleUrl;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.googleapis.json.JsonCContent;
import com.google.api.client.googleapis.json.JsonCParser;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.Key;

public class YouTubeComm {
	
	protected static final String DEVKEY = "AI39si6yAoZ7f6EjzMCtIex34tdlMped4mK3ZL9vg-8pn8ZXTrvhKd6_5VfR-J-GwJyFQC4lPm6YAnH0V-zSvsgAcJ-xlT0ZUg"; 
	
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
			return executeGetFeed(url, VideoFeed.class);
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