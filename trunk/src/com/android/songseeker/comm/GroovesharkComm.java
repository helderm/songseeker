package com.android.songseeker.comm;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.android.songseeker.comm.ServiceCommException.ServiceErr;
import com.android.songseeker.comm.ServiceCommException.ServiceID;
import com.android.songseeker.data.UserPlaylistsData;
import com.android.songseeker.util.Util;
import com.google.api.client.util.Base64;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class GroovesharkComm {
	private static GroovesharkComm comm = new GroovesharkComm();

	private static final String KEY = "heldermartins";
	private static final String SECRET = "b0518075945c057909da9829fe1639df";
	private static final String ENDPOINT = "https://api.grooveshark.com/ws/3.0/?sig=";
	private static final String PREF_SESSIONID = "prefs.grooveshark.sessionid";	
	private static final String CRYPT_ALG = "HmacMD5";

	private static String sessionID = null;

	public static GroovesharkComm getComm(SharedPreferences settings){
		if(sessionID != null)
			return comm;
		
		sessionID = settings.getString(PREF_SESSIONID, null);		
		return comm;
	}
	
	public static GroovesharkComm getComm(){
		return comm;
	}
	
	public void requestAuthorize(String username, String password, SharedPreferences settings) throws ServiceCommException{
		if(sessionID != null)
			return;
		
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
			HttpPost request = new HttpPost(ENDPOINT+signature);
			StringEntity body = new StringEntity(JSONValue.toJSONString(args));				
			request.setEntity(body);			
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(request);
			
			//parse the response
			HttpEntity r_entity = response.getEntity();
			String jsonString = EntityUtils.toString(r_entity);		
			JSONParser parser = new JSONParser();
			
			JSONObject result = (JSONObject)((JSONObject)parser.parse(jsonString)).get("result");
			
			//if we have a UserID, the our session is autheticated
			String userID = Long.toString((Long)result.get("UserID"));
			if(userID == null){
				throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.NOT_AUTH);
			}
		
		} catch(ServiceCommException e){
			throw e;		
		} catch (IOException e){
			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
		} catch(Exception e){
			Log.e(Util.APP, e.getMessage(), e);
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
			HttpPost request = new HttpPost(ENDPOINT+signature);
			StringEntity body = new StringEntity(JSONValue.toJSONString(args));				
			request.setEntity(body);			
			HttpClient httpClient = new DefaultHttpClient();
			response = httpClient.execute(request);

			if (response.getStatusLine().getStatusCode() != 200){
				Log.e(Util.APP, "HTTP client returned code different from 200! code: "
						+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
				throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
			} 			

			//parse the response
			HttpEntity r_entity = response.getEntity();
			String jsonString = EntityUtils.toString(r_entity);
			JSONParser parser=new JSONParser();			
			JSONObject result = (JSONObject)((JSONObject)parser.parse(jsonString)).get("result");

			//get result status
			Boolean success = (Boolean)result.get("success");
			if(success == null || success != true){
				throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.REQ_FAILED);
			}	
			
			//get unauthenticated session id
			String unauthSessionID = (String)result.get("sessionID");
			if(unauthSessionID == null){
				throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.REQ_FAILED);
			}			
			
			return unauthSessionID;

		} catch(ServiceCommException e){
			throw e;		
		} catch (IOException e){
			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
		} catch(Exception e){
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.UNKNOWN);
		}
	}

 	public UserPlaylistsData getUserPlaylists() throws ServiceCommException{
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
 
 			HttpPost request = new HttpPost(ENDPOINT+signature);
 			StringEntity body = new StringEntity(JSONValue.toJSONString(args));
 
 			request.setEntity(body);			
 			HttpClient httpClient = new DefaultHttpClient();
 			response = httpClient.execute(request);
 			 			 
 	 		if (response.getStatusLine().getStatusCode() != 200){
 	 			Log.e(Util.APP, "HTTP client returned code different from 200! code: "
 	 					+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
 	 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 	 		} 
 			
			//parse the response
			HttpEntity r_entity = response.getEntity();
			String jsonString = EntityUtils.toString(r_entity);
			
			JSONParser parser = new JSONParser();			
			JSONArray result = (JSONArray)((JSONObject)parser.parse(jsonString)).get("playlists");

			for(int i=0; i<result.size(); i++){
				JSONObject pl = (JSONObject) result.get(i);
				
				String id = Long.toString((Long) pl.get("PlaylistID"));
				String name = (String) pl.get("PlaylistName");				
				
			}
 
 		} catch(ServiceCommException e){
 			throw e;		
 		} catch (IOException ex){
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 		} catch(Exception e){
 			Log.e(Util.APP, e.getMessage(), e);
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.UNKNOWN);
 		}

 		
 		//...
 		
 		return data;
 	}
 	
 	public String getSongID(String songName, String artistName) throws ServiceCommException{		
 		HttpResponse response;
 		
 		try{	
 			LinkedHashMap<String, Object> args = new LinkedHashMap<String, Object>();
 			args.put("method", "getSongSearchResults");
 
 			LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();
 			header.put("wsKey", KEY);
 			header.put("sessionID", sessionID);
 			args.put("header", header);	
 
 			LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
 			parameters.put("query", songName + " " + artistName);
 			parameters.put("limit", "5");
 			args.put("parameters", parameters);
 			
 			String signature = getSignature(args);			
 
 			HttpPost request = new HttpPost(ENDPOINT+signature);
 			StringEntity body = new StringEntity(JSONValue.toJSONString(args));
 
 			request.setEntity(body);			
 			HttpClient httpClient = new DefaultHttpClient();
 			response = httpClient.execute(request);
 
 		} catch(ServiceCommException e){
 			throw e;		
 		} catch (IOException ex){
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 		} catch(Exception e){
 			Log.e(Util.APP, e.getMessage(), e);
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.UNKNOWN);
 		}
 
 		if (response.getStatusLine().getStatusCode() != 200){
 			Log.e(Util.APP, "HTTP client returned code different from 200! code: "
 								+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 		} 		
 		
 		//...
 		
 		return null;
 	}
 	
 	public void createPlaylist(String name, ArrayList<String> songIDs) throws ServiceCommException{
 		HttpResponse response;
 		
 		try{	
 			LinkedHashMap<String, Object> args = new LinkedHashMap<String, Object>();
 			args.put("method", "createPlaylist");
 
 			LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();
 			header.put("wsKey", KEY);
 			header.put("sessionID", sessionID);
 			args.put("header", header);	
 
 			LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
 			parameters.put("name", name);
 			
 			StringBuilder sb = new StringBuilder();
 			for(String id : songIDs){
 				sb.append(id+",");
 			}
 			sb.deleteCharAt(sb.length()-1);		
 			parameters.put("songIDs", sb.toString());
 			
 			args.put("parameters", parameters);
 			
 			String signature = getSignature(args);			
 
 			HttpPost request = new HttpPost(ENDPOINT+signature);
 			StringEntity body = new StringEntity(JSONValue.toJSONString(args));
 
 			request.setEntity(body);			
 			HttpClient httpClient = new DefaultHttpClient();
 			response = httpClient.execute(request);
 
 		} catch(ServiceCommException e){
 			throw e;		
 		} catch (IOException ex){
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 		} catch(Exception e){
 			Log.e(Util.APP, e.getMessage(), e);
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.UNKNOWN);
 		}
 
 		if (response.getStatusLine().getStatusCode() != 200){
 			Log.e(Util.APP, "HTTP client returned code different from 200! code: "
 								+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
 			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
 		} 		
 		
 		//...		
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
}
