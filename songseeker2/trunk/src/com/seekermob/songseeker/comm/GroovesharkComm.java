package com.seekermob.songseeker.comm;

import java.io.IOException;
import java.math.BigInteger;

import java.security.MessageDigest;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceID;
//import com.seekermob.songseeker.data.UserPlaylistsData;
import com.seekermob.songseeker.util.Util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

public class GroovesharkComm {
	private static GroovesharkComm comm = new GroovesharkComm();

	private static final String KEY = "heldermartins";
	private static final String SECRET = "b0518075945c057909da9829fe1639df";
	
	private static final String ENDPOINT = "api.grooveshark.com/ws/3.0/?sig=";
	private static final String HTTP = "http://";
	private static final String HTTPS = "https://";
	
	private static final String PREF_SESSIONID = "prefs.grooveshark.sessionid";	
	private static final String CRYPT_ALG = "HmacMD5";	
	
	public static final int RATE_LIMIT = 126; //actually 128, but I'm leaving a margin here
	
	private static String sessionID = null;

	public static GroovesharkComm getComm(Context c){
		if(sessionID != null)
			return comm;
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);		
		sessionID = settings.getString(PREF_SESSIONID, null);		
		return comm;
	}
	
	public static GroovesharkComm getComm(){
		return comm;
	}
	
	public boolean isAuthorized(){
		if(sessionID != null)
			return true;
		
		return false;
	}
	
	public void requestAuthorize(String username, String password, SharedPreferences settings) throws ServiceCommException{
		String unauthSession = null;
		
		try{
		
			unauthSession = startSession();	
			
			//get MD5 token from password
			String token = getPasswordToken(password);
			
			//build params
			LinkedHashMap<String, Object> args = new LinkedHashMap<String, Object>();
			args.put("method", "authenticate");
	
			//add key and session
			LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();
			header.put("wsKey", KEY);
			header.put("sessionID", unauthSession);
			args.put("header", header);
			
			//add login and token
			LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
			parameters.put("login", username);
			parameters.put("password", token);
			args.put("parameters", parameters);
			
			//call authenticate
			String signature = getSignature(args);	
			HttpPost request = new HttpPost(HTTPS+ENDPOINT+signature);
			StringEntity body = new StringEntity(JSONValue.toJSONString(args));				
			request.setEntity(body);			
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(request);
			
			//parse the response
			HttpEntity r_entity = response.getEntity();
			String jsonString = EntityUtils.toString(r_entity);		
			JSONParser parser = new JSONParser();
			
			//check for errors
			parseError(jsonString, parser);
			
			JSONObject result = (JSONObject)((JSONObject)parser.parse(jsonString)).get("result");
			
			//if we have a UserID, the our session is autheticated
			String userID = Long.toString((Long)result.get("UserID"));
			if(userID == null || userID.equals("0")){
				throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.NOT_AUTH);
			}
		
		} catch(ServiceCommException e){
 			if(e.getErr() == ServiceErr.NOT_AUTH)
 				cleanAuth(settings);
 			
			throw e;		
		} catch (IOException e){
			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
		} catch(Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.UNKNOWN);
		}
		
		sessionID = unauthSession;
		
		//save the session id
		Editor editor = settings.edit();
		editor.putString(PREF_SESSIONID, sessionID);
		editor.commit();
	}

	private String startSession() throws ServiceCommException{		
		HttpResponse response;

		try{
			LinkedHashMap<String, Object> args = new LinkedHashMap<String, Object>();
			args.put("method", "startSession");

			LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();
			header.put("wsKey", KEY);

			args.put("header", header);			
			args.put("parameters", "");

			//call start session	
			String signature = getSignature(args);	
			HttpPost request = new HttpPost(HTTPS+ENDPOINT+signature);
			StringEntity body = new StringEntity(JSONValue.toJSONString(args));				
			request.setEntity(body);			
			HttpClient httpClient = new DefaultHttpClient();
			response = httpClient.execute(request);

			if (response.getStatusLine().getStatusCode() != 200){
				Log.w(Util.APP, "HTTP client returned code different from 200! code: "
						+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
				throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
			} 			

			HttpEntity r_entity = response.getEntity();
			String jsonString = EntityUtils.toString(r_entity);
			JSONParser parser = new JSONParser();			
			
			//check for errors
			parseError(jsonString, parser);			

			//parse the response
			JSONObject result = (JSONObject)((JSONObject)parser.parse(jsonString)).get("result");

			//get result status
			Boolean success = (Boolean)result.get("success");
			if(success == null || success != true){
				throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.REQ_FAILED);
			}	
			
			//get unauthenticated session id
			String unauthSessionID = (String)result.get("sessionID");
			if(unauthSessionID == null){
				throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.NOT_AUTH);
			}			
			
			return unauthSessionID;

		} catch(ServiceCommException e){
			throw e;		
		} catch (IOException e){
			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
		} catch(Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.UNKNOWN);
		}
	}

 	/*public UserPlaylistsData getUserPlaylists(SharedPreferences settings) throws ServiceCommException{
 		UserPlaylistsData data = new UserPlaylistsData();
 		HttpResponse response;
 		
 		try{	
 			LinkedHashMap<String, Object> args = new LinkedHashMap<String, Object>();
 			args.put("method", "getUserPlaylists");
 
 			LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();
 			header.put("wsKey", KEY);
 			header.put("sessionID", sessionID);
 
 			args.put("header", header);	
 
 			String signature = getSignature(args);			
 
 			HttpPost request = new HttpPost(HTTP+ENDPOINT+signature);
 			StringEntity body = new StringEntity(JSONValue.toJSONString(args));
 
 			request.setEntity(body);			
 			HttpClient httpClient = new DefaultHttpClient();
 			response = httpClient.execute(request);
 			 			 
 	 		if (response.getStatusLine().getStatusCode() != 200){
 	 			Log.w(Util.APP, "HTTP client returned code different from 200! code: "
 	 					+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
 	 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 	 		} 
 			
			HttpEntity r_entity = response.getEntity();
			String jsonString = EntityUtils.toString(r_entity);			
			JSONParser parser = new JSONParser();			
			
			//check for errors
			parseError(jsonString, parser);
			
			//parse the response
			JSONObject result = (JSONObject)((JSONObject)parser.parse(jsonString)).get("result");
			JSONArray array = (JSONArray) result.get("playlists");			
			
			for(int i=0; i<array.size(); i++){
				JSONObject pl = (JSONObject) array.get(i);
				
				String id = Long.toString((Long) pl.get("PlaylistID"));
				String name = (String) pl.get("PlaylistName");
				
				data.addPlaylist(name, -1, id);
				
			} 
 		} catch(ServiceCommException e){
 			if(e.getErr() == ServiceErr.NOT_AUTH)
 				cleanAuth(settings); 			
 			
 			throw e;		
 		} catch (IOException ex){
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 		} catch(Exception e){
 			Log.w(Util.APP, e.getMessage(), e);
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.UNKNOWN);
 		}
 		
 		return data;
 	}*/
 	
 	public String getSongID(String songName, String artistName, SharedPreferences settings) throws ServiceCommException{		
 		
 		try{	
 			LinkedHashMap<String, Object> args = new LinkedHashMap<String, Object>();
 			args.put("method", "getSongSearchResults");
 
 			LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();
 			header.put("wsKey", KEY);
 			args.put("header", header);	
 
 			LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
 			parameters.put("query", songName + " " + artistName);
 			parameters.put("country", "");
 			parameters.put("limit", "20");
 			args.put("parameters", parameters);
 			
 			String signature = getSignature(args);			
 
 			HttpPost request = new HttpPost(HTTP+ENDPOINT+signature);
 			StringEntity body = new StringEntity(JSONValue.toJSONString(args));
 
 			request.setEntity(body);			
 			HttpClient httpClient = new DefaultHttpClient();
 			HttpResponse response = httpClient.execute(request); 			
 			 
 	 		if (response.getStatusLine().getStatusCode() != 200){
 	 			Log.w(Util.APP, "HTTP client returned code different from 200! code: "
 	 						+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
 	 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 	 		} 		
 	 		
 			HttpEntity r_entity = response.getEntity();
 			String jsonString = EntityUtils.toString(r_entity);			
 			JSONParser parser = new JSONParser();			
 			
 			//check for errors
 			parseError(jsonString, parser);
 			
 			//parse the response
 			JSONObject result = (JSONObject)((JSONObject)parser.parse(jsonString)).get("result");
 			JSONArray array = (JSONArray) result.get("songs");			
 			
 			//search for the song
 			for(int i=0; i<array.size(); i++){
 				JSONObject song = (JSONObject) array.get(i);
 				
 				String id = Long.toString((Long) song.get("SongID"));
 				
 				String name = (String) song.get("SongName");
 				String artist = (String) song.get("ArtistName"); 				
 				
 				if((songName.equalsIgnoreCase(name) || name.toLowerCase().contains(songName.toLowerCase()) || songName.toLowerCase().contains(name.toLowerCase())) && 
 					(artistName.equalsIgnoreCase(artist) || artist.toLowerCase().contains(artistName.toLowerCase()) || artistName.toLowerCase().contains(artist.toLowerCase()))){
 					
 					return id;
 				} 							
 			} 
 			
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.SONG_NOT_FOUND);
 
 		} catch(ServiceCommException e){ 			
 			if(e.getErr() == ServiceErr.NOT_AUTH)
 				cleanAuth(settings);
 			
 			throw e;		
 		} catch (IOException ex){
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 		} catch(Exception e){
 			Log.w(Util.APP, e.getMessage(), e);
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.UNKNOWN);
 		}
 	}
 	
 	public void createPlaylist(String name, ArrayList<String> songIDs, SharedPreferences settings) throws ServiceCommException{
 		
 		try{	
 			LinkedHashMap<String, Object> args = new LinkedHashMap<String, Object>();
 			args.put("method", "createPlaylist");
 
 			LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();
 			header.put("wsKey", KEY);
 			header.put("sessionID", sessionID);
 			args.put("header", header);	
 
 			LinkedHashMap<String, Object> parameters = new LinkedHashMap<String, Object>();
 			parameters.put("name", name);
			parameters.put("songIDs", songIDs); 			
 			args.put("parameters", parameters);
 			
 			String signature = getSignature(args);			
 
 			HttpPost request = new HttpPost(HTTP+ENDPOINT+signature);
 			StringEntity body = new StringEntity(JSONValue.toJSONString(args));
 
 			request.setEntity(body);			
 			HttpClient httpClient = new DefaultHttpClient();
 			HttpResponse response = httpClient.execute(request);
 			
	 		if (response.getStatusLine().getStatusCode() != 200){
 	 			Log.w(Util.APP, "HTTP client returned code different from 200! code: "
 	 						+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
 	 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 	 		} 		
 	 		
			HttpEntity r_entity = response.getEntity();
			String jsonString = EntityUtils.toString(r_entity);
			JSONParser parser = new JSONParser();			
			
			//check for errors
			parseError(jsonString, parser);			

			//parse the response
			JSONObject result = (JSONObject)((JSONObject)parser.parse(jsonString)).get("result");

			//get result status
			Boolean success = (Boolean)result.get("success");
			if(success == null || success != true){
				throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.REQ_FAILED);
			}	
 
 		} catch(ServiceCommException e){
 			if(e.getErr() == ServiceErr.NOT_AUTH)
 				cleanAuth(settings); 	
 			
 			throw e;		
 		} catch (IOException ex){
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 		} catch(Exception e){
 			Log.w(Util.APP, e.getMessage(), e);
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.UNKNOWN);
 		}
 	}
 	
 	public ArrayList<String> getPlaylistSongs(String playlistID, SharedPreferences settings) throws ServiceCommException{
 		ArrayList<String> plSongs = new ArrayList<String>();
 		
 		try{	
 			LinkedHashMap<String, Object> args = new LinkedHashMap<String, Object>();
 			args.put("method", "getPlaylistSongs");
 
 			LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();
 			header.put("wsKey", KEY);
 			header.put("sessionID", sessionID);
 			args.put("header", header);	
 
 			LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
 			parameters.put("playlistID", playlistID);
 			args.put("parameters", parameters);
 			
 			String signature = getSignature(args);			
 
 			HttpPost request = new HttpPost(HTTP+ENDPOINT+signature);
 			StringEntity body = new StringEntity(JSONValue.toJSONString(args));
 
 			request.setEntity(body);			
 			HttpClient httpClient = new DefaultHttpClient();
 			HttpResponse response = httpClient.execute(request); 			
 			 
 	 		if (response.getStatusLine().getStatusCode() != 200){
 	 			Log.w(Util.APP, "HTTP client returned code different from 200! code: "
 	 						+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
 	 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 	 		} 		
 	 		
 			HttpEntity r_entity = response.getEntity();
 			String jsonString = EntityUtils.toString(r_entity);			
 			JSONParser parser = new JSONParser();			
 			
 			//check for errors
 			parseError(jsonString, parser);
 			
 			//parse the response
 			JSONObject result = (JSONObject)((JSONObject)parser.parse(jsonString)).get("result");
 			JSONArray array = (JSONArray) result.get("songs"); 			
 	 		
 			//search for songs
 			for(int i=0; i<array.size(); i++){
 				JSONObject song = (JSONObject) array.get(i);
 				
 				String id = Long.toString((Long) song.get("SongID"));
 				plSongs.add(id);					
 			} 
 			
 	 		return plSongs;
 	 		
 		} catch(ServiceCommException e){ 			
 			if(e.getErr() == ServiceErr.NOT_AUTH)
 				cleanAuth(settings);
 			
 			throw e;		
 		} catch (IOException ex){
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 		} catch(Exception e){
 			Log.w(Util.APP, e.getMessage(), e);
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.UNKNOWN);
 		} 		
 	}
 	
 	public void setPlaylistSongs(String playlistID, ArrayList<String> songIDs, SharedPreferences settings) throws ServiceCommException{
 		
 		try{	
 			LinkedHashMap<String, Object> args = new LinkedHashMap<String, Object>();
 			args.put("method", "setPlaylistSongs");
 
 			LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();
 			header.put("wsKey", KEY);
 			header.put("sessionID", sessionID);
 			args.put("header", header);	
 
 			LinkedHashMap<String, Object> parameters = new LinkedHashMap<String, Object>();
 			parameters.put("playlistID", playlistID);
			parameters.put("songIDs", songIDs); 			
 			args.put("parameters", parameters);
 			
 			String signature = getSignature(args);			
 
 			HttpPost request = new HttpPost(HTTP+ENDPOINT+signature);
 			StringEntity body = new StringEntity(JSONValue.toJSONString(args));
 
 			request.setEntity(body);			
 			HttpClient httpClient = new DefaultHttpClient();
 			HttpResponse response = httpClient.execute(request);
 			
	 		if (response.getStatusLine().getStatusCode() != 200){
 	 			Log.w(Util.APP, "HTTP client returned code different from 200! code: "
 	 						+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
 	 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 	 		} 		
 	 		
			HttpEntity r_entity = response.getEntity();
			String jsonString = EntityUtils.toString(r_entity);
			JSONParser parser = new JSONParser();			
			
			//check for errors
			parseError(jsonString, parser);			

			//parse the response
			JSONObject result = (JSONObject)((JSONObject)parser.parse(jsonString)).get("result");

			//get result status
			Long success = (Long)result.get("success");
			if(success == null || success != 1){
				throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.REQ_FAILED);
			}	
 
 		} catch(ServiceCommException e){
 			if(e.getErr() == ServiceErr.NOT_AUTH)
 				cleanAuth(settings); 	
 			
 			throw e;		
 		} catch (IOException ex){
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 		} catch(Exception e){
 			Log.w(Util.APP, e.getMessage(), e);
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.UNKNOWN);
 		}
 	} 	
 	
	private String getSignature(LinkedHashMap<String, Object> args) throws Exception{

		Mac mac = Mac.getInstance(CRYPT_ALG);
		SecretKeySpec secret = new SecretKeySpec(SECRET.getBytes(), CRYPT_ALG);
		mac.init(secret);
		String value = JSONValue.toJSONString(args);
		byte[] digest = mac.doFinal(value.getBytes());
		BigInteger hash = new BigInteger(1, digest);
		String hmac = hash.toString(16);

		if (hmac.length() % 2 != 0) {
			hmac = "0" + hmac;
		}

		return hmac;
	}

	private String getPasswordToken(String password) throws Exception{
		//get MD5 token from password
		byte[] bytes = password.getBytes("UTF-8");
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] digest = md.digest(bytes);
		BigInteger hash = new BigInteger(1, digest);
		String token = hash.toString(16);
		if (token.length() % 2 != 0) {
			token = "0" + token;
		}
		
		return token;
	}
	
	private void parseError(String jsonString, JSONParser parser) throws ServiceCommException{		
		
		try{
		
			JSONArray errors = (JSONArray)((JSONObject)parser.parse(jsonString)).get("errors");
			
			if(errors == null)
				return;
			
			Long code = (Long)((JSONObject)errors.get(0)).get("code");
			if(code == null)
				throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.REQ_FAILED);
			
			switch(code.intValue()){
			case 11: //rate limit exceded
				throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.TRY_LATER);
			case 100:
			case 101:
			case 300:	
				throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.NOT_AUTH);			
			default:
				throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.REQ_FAILED);
			}
		
		} catch(ServiceCommException e){
			throw e;
		} catch(NullPointerException e){
			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.REQ_FAILED);
		} catch(Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.UNKNOWN);
		}		
	}
	
	public void cleanAuth(SharedPreferences settings){
		Editor editor = settings.edit();
		editor.putString(PREF_SESSIONID, null);
		editor.commit();
		sessionID = null;
	}
}
