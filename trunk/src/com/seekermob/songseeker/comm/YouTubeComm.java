package com.seekermob.songseeker.comm;


import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
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

import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceID;
import com.seekermob.songseeker.data.UserPlaylistsData;
import com.seekermob.songseeker.util.Util;


public class YouTubeComm {

	protected static final String DEVKEY = "AI39si6yAoZ7f6EjzMCtIex34tdlMped4mK3ZL9vg-8pn8ZXTrvhKd6_5VfR-J-GwJyFQC4lPm6YAnH0V-zSvsgAcJ-xlT0ZUg"; 
	private static YouTubeComm comm = new YouTubeComm();

	//WS URLs
	private static final String GET_PLAYLISTS_ENDPOINT = "https://gdata.youtube.com/feeds/api/users/default/playlists?v=2&alt=jsonc";
	private static final String CREATE_PLAYLIST_ENDPOINT = "https://gdata.youtube.com/feeds/api/users/default/playlists?alt=jsonc";
	private static final String ADD_VIDEO_ENDPOINT = "https://gdata.youtube.com/feeds/api/playlists/";
	private static final String GET_VIDEOS_ENDPOINT = "http://gdata.youtube.com/feeds/api/videos?alt=jsonc&v=2";

	//login
	private static GoogleAccountManager accountManager = null;
	private static GoogleAccessProtectedResource accessProtectedResource = null;
	
	//auth token
	private static final String PREF_ACCESSTOKEN = "prefs.google.accesstoken";
	private static final String AUTH_TOKEN_TYPE = "Youtube";
	
	private YouTubeComm(){}

	public static YouTubeComm getComm(Context c, SharedPreferences settings){
		if(accountManager == null)
			accountManager = new GoogleAccountManager(c);

		if(accessProtectedResource == null){
			accessProtectedResource = new GoogleAccessProtectedResource(settings.getString(PREF_ACCESSTOKEN, null));
		}

		return comm;
	}

	public boolean isAuthorized(){
		if(accessProtectedResource == null)
			return false;
		
		if(accessProtectedResource.getAccessToken() != null)
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

		//TODO Debug
		//accountManager.invalidateAuthToken(accessProtectedResource.getAccessToken());
		//accessProtectedResource.setAccessToken(null);

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
		editor.putString(PREF_ACCESSTOKEN, null);
		editor.commit();
	}
	
	public static YouTubeComm getComm(){
		return comm;
	}

	public UserPlaylistsData getUserPlaylists(SharedPreferences settings) throws ServiceCommException{

		UserPlaylistsData data = new UserPlaylistsData();		

		try{
			HttpGet request = new HttpGet(GET_PLAYLISTS_ENDPOINT);
			request.setHeader("Authorization", "GoogleLogin auth="+accessProtectedResource.getAccessToken());
			request.setHeader("X-GData-Key", "key="+DEVKEY);
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(request);

			//check for errors
			treatHttpError(response, settings);
			
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
			Log.w(Util.APP, "Unknown error while trying to get the user's playlist on YouTube!", e);
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.UNKNOWN);
		}	
	}

	public String createPlaylist(String name, SharedPreferences settings) throws ServiceCommException{

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
			
			//check for errors
			treatHttpError(response, settings);

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
			Log.w(Util.APP, "Unknown error while trying to create a YouTube playlist!", e);
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.UNKNOWN);
		}	
	}

	public ArrayList<VideoFeed> searchVideo(String songName, String artistName, int maxResults) throws ServiceCommException{
		ArrayList<VideoFeed> videos = new ArrayList<VideoFeed>();
		
		try{
			String query = songName + "+" + artistName;			
			HttpGet request = new HttpGet(GET_VIDEOS_ENDPOINT+ "&q="+ query.replace(' ', '+')+ "&max-results="+maxResults);
	
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(request);

			if(response.getStatusLine().getStatusCode() != 200) {

				Log.w(Util.APP, "HTTP client returned code different from 200! code: "+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
				throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.REQ_FAILED);
			}   

			String jsonString = EntityUtils.toString(response.getEntity());	
			JSONParser parser = new JSONParser();

			//parse the response
			JSONObject result = (JSONObject)((JSONObject)parser.parse(jsonString)).get("data");
			Long totalItems = (Long)result.get("totalItems");
			if(totalItems <= 0)
				return videos;
			
			JSONArray array = (JSONArray) result.get("items");	
			VideoFeed video = new VideoFeed();
			
			for(int i=0; i<array.size(); i++){
				JSONObject pl = (JSONObject) array.get(i);

				video = new VideoFeed();				
				video.id = (String) pl.get("id");
				video.title = (String) pl.get("title");
				video.description = (String) pl.get("description");
				video.image = (String)((JSONObject) pl.get("thumbnail")).get("sqDefault");	
				videos.add(video);
			} 			

			return videos;

		}catch (IOException ex){
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.IO);
		}catch (ServiceCommException e){
			throw e;
		}catch (Exception e){
			Log.w(Util.APP, "Unknown error while trying to search for videos on YouTube!", e);
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.UNKNOWN);
		}	
	}		
	
	public void addVideosToPlaylist(String playlistID, String videoID, SharedPreferences settings) throws ServiceCommException{
		
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
			
			StringEntity body = new StringEntity(JSONValue.toJSONString(args));
			body.setContentType("application/json");
			request.setEntity(body);			
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(request);
			
			//check for errors
			treatHttpError(response, settings);

		}catch (IOException ex){
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.IO);
		}catch (ServiceCommException e){
			throw e;
		}catch (Exception e){
			Log.w(Util.APP, "Unknown error while trying to add videos to YouTube playlist!", e);
			throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.UNKNOWN);
		}	
		
	}
	
	private void treatHttpError(HttpResponse response, SharedPreferences settings) throws ServiceCommException{
		int code = response.getStatusLine().getStatusCode();
		
		if(code != 200 && code != 201 ) {

			switch(code){
			
			case 401:
			case 403:
				//unauthorized
				unauthorizeUser(settings);
				throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.NOT_AUTH);
				
			default:
				Log.w(Util.APP, "HTTP client returned code different from 200 or 201! code: "+ code+ " - "+ response.getStatusLine().getReasonPhrase());
				throw new ServiceCommException(ServiceID.YOUTUBE, ServiceErr.REQ_FAILED);
			}
		} 
	}
	
	public class VideoFeed{
		public String id;
		public String title;
		public String description;
		public String image;	
	}	

}
