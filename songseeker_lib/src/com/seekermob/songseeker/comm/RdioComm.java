package com.seekermob.songseeker.comm;

import java.io.BufferedReader;
import java.io.IOException;
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

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceID;
import com.seekermob.songseeker.data.PlaylistInfo;
import com.seekermob.songseeker.util.Util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;


public class RdioComm {
	private static RdioComm comm = new RdioComm();
	
	private static String consumer_key = "e6axyuwxza2fxdhkbqbwdb2f";
	private static String consumer_secret = "5PcRraQyRk";		
	private static final String REQUEST_TOKEN = "http://api.rdio.com/oauth/request_token";
	private static final String ACCESS_TOKEN = "http://api.rdio.com/oauth/access_token";
	private static final String AUTHORIZE = "https://www.rdio.com/oauth/authorize";
	private static final String ENDPOINT = "http://api.rdio.com/1/";
	
	private static OAuthConsumer consumer;
	private OAuthProvider provider = new CommonsHttpOAuthProvider(REQUEST_TOKEN, ACCESS_TOKEN, AUTHORIZE);
	
	private static String accessToken = null;
	private static String accessTokenSecret = null;	
	private static final String PREF_ACCESSTOKEN = "prefs.rdio.accesstoken";
	private static final String PREF_ACCESSTOKENSECRET = "prefs.rdio.accesstokensecret";	
		
	private RdioComm(){}
	
	public static void initialize(Context c){
		consumer_key = c.getString(R.string.rdio_key);
		consumer_secret = c.getString(R.string.rdio_secret);
		consumer = new CommonsHttpOAuthConsumer(consumer_key, consumer_secret);	
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		accessToken = settings.getString(PREF_ACCESSTOKEN, null);
		accessTokenSecret = settings.getString(PREF_ACCESSTOKENSECRET, null);
	}
	
	public static RdioComm getComm(){
		return comm;
	}
	
	public boolean isAuthorized(){
		if(accessToken == null || accessTokenSecret == null)
			return false;
		
		return true;
	}
	
	public void retrieveRequestToken(Activity a) throws ServiceCommException{
		String authUrl = null;

		try {
			authUrl = provider.retrieveRequestToken(consumer, "oauth://checkin4me");
		} catch (OAuthCommunicationException e) {
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.IO);
		} catch (Exception e){
			Log.w(Util.APP, "Error while fetching request token to Rdio!", e);
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.UNKNOWN);
		}

		//TODO: Check how can we start this activity without adding it to the call stack
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
		i.setFlags(i.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
		//i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		//i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		a.startActivity(i);		
	}
	
	public void retrieveAccessTokens(String verifier, Context c) throws ServiceCommException{
		
		//String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
		if(verifier == null){
			cleanAuthTokens(c);			
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.NOT_AUTH);
		}
		
		try {
			provider.retrieveAccessToken(consumer, verifier);
		} catch (OAuthCommunicationException e) {
			cleanAuthTokens(c);			
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.IO);
		} catch (Exception e){
			Log.w(Util.APP, "Error while trying to retrieve access tokens from Rdio!", e);
			cleanAuthTokens(c);			
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.UNKNOWN);
		}

		accessToken = consumer.getToken();
		accessTokenSecret = consumer.getTokenSecret();
		
		Editor editor = PreferenceManager.getDefaultSharedPreferences(c).edit();
		editor.putString(PREF_ACCESSTOKEN, accessToken);
		editor.putString(PREF_ACCESSTOKENSECRET, accessTokenSecret);
		editor.commit();
	}
	
	public void cleanAuthTokens(Context c){
		Editor editor = PreferenceManager.getDefaultSharedPreferences(c).edit();
		editor.putString(PREF_ACCESSTOKEN, null);
		editor.putString(PREF_ACCESSTOKENSECRET, null);
		editor.commit();
		
		accessToken = null;
		accessTokenSecret = null;
	}
	
	public void createPlaylist(String playlistName, List<String> songIDs, Context c) throws ServiceCommException{

		StringBuilder sb = new StringBuilder();
		
		HttpPost request = new HttpPost(ENDPOINT);
		HttpResponse response;

		List<NameValuePair> request_args = new ArrayList<NameValuePair>();
		request_args.add(new BasicNameValuePair("method", "createPlaylist"));
		request_args.add(new BasicNameValuePair("name", playlistName));
		request_args.add(new BasicNameValuePair("description", ""));		
			
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
			consumer.sign(request);
	
			HttpClient httpClient = new DefaultHttpClient();
			response = httpClient.execute(request);
        
		}catch (OAuthCommunicationException ex){
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.IO);
		}catch (IOException ex){
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.IO);
		}catch (Exception ex){
			Log.w(Util.APP, "Unknown error while trying to create playlist on Rdio!", ex);
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.UNKNOWN);
		}

		if (response.getStatusLine().getStatusCode() != 200) {
        	
        	//unauthorized
        	if(response.getStatusLine().getStatusCode() == 401){
        		cleanAuthTokens(c);
        		throw new ServiceCommException(ServiceID.RDIO, ServiceErr.NOT_AUTH);
        	}
        	
        	Log.w(Util.APP, "HTTP client returned code different from 200! code: "+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
        	throw new ServiceCommException(ServiceID.RDIO, ServiceErr.REQ_FAILED);
        }               
        
        //TODO check 'status'
	}
	
	public void addToPlaylist(String playlistID, List<String> songIDs, Context c) throws ServiceCommException{

		StringBuilder sb = new StringBuilder();
		
		HttpPost request = new HttpPost(ENDPOINT);
		HttpResponse response;

		List<NameValuePair> request_args = new ArrayList<NameValuePair>();
		request_args.add(new BasicNameValuePair("method", "addToPlaylist"));
		request_args.add(new BasicNameValuePair("playlist", playlistID));
					
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

			consumer.sign(request);
	
			HttpClient httpClient = new DefaultHttpClient();
			response = httpClient.execute(request);
        
		}catch (OAuthCommunicationException ex){
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.IO);
		}catch (IOException ex){
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.IO);
		}catch (Exception ex){
			Log.w(Util.APP, "Unknown error while trying to add to playlist on Rdio!", ex);
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.UNKNOWN);
		}

		if (response.getStatusLine().getStatusCode() != 200) {
        	
        	//unauthorized
        	if(response.getStatusLine().getStatusCode() == 401){
        		cleanAuthTokens(c);
        		throw new ServiceCommException(ServiceID.RDIO, ServiceErr.NOT_AUTH);
        	}
        	
        	Log.w(Util.APP, "HTTP client returned code different from 200! code: "+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
        	throw new ServiceCommException(ServiceID.RDIO, ServiceErr.REQ_FAILED);
        }               
        
        //TODO check 'status'
	}

	
	public String queryTrackID(String songName, String songArtist) throws ServiceCommException{
		HttpPost request = new HttpPost(ENDPOINT);
		HttpResponse response;
		int start_index, end_index;
				
		List<NameValuePair> request_args = new ArrayList<NameValuePair>();
		request_args.add(new BasicNameValuePair("method", "search"));
		request_args.add(new BasicNameValuePair("types", "Track"));
		//request_args.add(new BasicNameValuePair("never_or", "false"));
		request_args.add(new BasicNameValuePair("count", "1"));
		
		request_args.add(new BasicNameValuePair("query", songName+" "+songArtist));	
		
		try{
			StringEntity body = new StringEntity(URLEncodedUtils.format(request_args, "UTF-8"));
			body.setContentType("application/x-www-form-urlencoded");
			request.setEntity(body);
	
			consumer.setTokenWithSecret(accessToken, accessTokenSecret);	
			consumer.sign(request);
	
			HttpClient httpClient = new DefaultHttpClient();
			response = httpClient.execute(request);	
		}catch (OAuthCommunicationException ex){
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.IO);
		}catch (IOException ex){
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.IO);
		}catch (Exception ex){
			Log.w(Util.APP, "Unknown error while trying to create playlist on Rdio!", ex);
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.UNKNOWN);
		}

        if (response.getStatusLine().getStatusCode() != 200) {
        	
        	//unauthorized
        	if(response.getStatusLine().getStatusCode() == 401){
        		//cleanAuthTokens(settings);
        		throw new ServiceCommException(ServiceID.RDIO, ServiceErr.NOT_AUTH);
        	}
        	
        	Log.w(Util.APP, "HTTP client returned code different from 200! code: "+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
        	throw new ServiceCommException(ServiceID.RDIO, ServiceErr.REQ_FAILED);
        }               
		
    	try{
        
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
	        	Log.w(Util.APP, "Rdio returned status different from OK! Status: "+bufStrOK);
	        	throw new ServiceCommException(ServiceID.RDIO, ServiceErr.REQ_FAILED);
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
	        	Log.w(Util.APP, "Rdio returned no tracks! Status: "+bufStrOK);
	        	throw new ServiceCommException(ServiceID.RDIO, ServiceErr.REQ_FAILED);
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
	    	return retString;
    	} catch (ServiceCommException e){
    		throw e;
    	} catch (IOException e){
    		throw new ServiceCommException(ServiceID.RDIO, ServiceErr.IO);
    	} catch (Exception e){
			Log.w(Util.APP, "Unknown error while trying to parse response from Rdio!", e);
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.UNKNOWN);
		}
	}
	
	public ArrayList<PlaylistInfo> getUserPlaylists() throws ServiceCommException{
		ArrayList<PlaylistInfo> playlists = new ArrayList<PlaylistInfo>();
	
		HttpPost request = new HttpPost(ENDPOINT);
		HttpResponse response;
		int start_index, end_index;
				
		List<NameValuePair> request_args = new ArrayList<NameValuePair>();
		request_args.add(new BasicNameValuePair("method", "getPlaylists"));		
		
		try{
			StringEntity body = new StringEntity(URLEncodedUtils.format(request_args, "UTF-8"));
			body.setContentType("application/x-www-form-urlencoded");
			request.setEntity(body);
	
			consumer.setTokenWithSecret(accessToken, accessTokenSecret);	
			consumer.sign(request);
				
			HttpClient httpClient = new DefaultHttpClient();
			response = httpClient.execute(request);	
		}catch (OAuthCommunicationException ex){
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.IO);
		}catch (IOException ex){
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.IO);
		}catch (Exception ex){
			Log.w(Util.APP, "Unknown error while trying to create playlist on Rdio!", ex);
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.UNKNOWN);
		}
		
        try{            
        	BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
	        			
	        String line;	        
	        while ((line = rd.readLine()) != null) {
	        		        	
	        	start_index = line.indexOf("\"status\": ");
		    	start_index += 11; 
		    	end_index = line.indexOf('\"', start_index);    	
		    	String bufStrOK = line.substring(start_index, end_index);    	    	
		    	if(!bufStrOK.equalsIgnoreCase("ok")){
		        	Log.w(Util.APP, "Rdio returned status different from OK! Status: "+bufStrOK);
		        	throw new ServiceCommException(ServiceID.RDIO, ServiceErr.REQ_FAILED);
		    	}
		    	bufStrOK = null;		
		    	
		    	//parse for owned
		    	start_index = line.indexOf("\"owned\": [");
		    	start_index += 10; 
		    	end_index = line.indexOf(']', start_index);
		    	if(end_index == start_index){
		    		return playlists;
		    	}
	    	
		    	String bufStrPls = line.substring(start_index, end_index);
		    	
		    	line = null;		    	
		    	int aux_ind = 0;
		    	while(true){
		    		
		    		//parse name
		    		start_index = bufStrPls.indexOf("\"name\": \"", aux_ind);
		    		start_index += 9;
		    		end_index = bufStrPls.indexOf('\"', start_index);
		    		String bufName = bufStrPls.substring(start_index, end_index);
		    		
		    		//parse lenght
		    		start_index = bufStrPls.indexOf("\"length\": ", aux_ind);
		    		start_index += 10;
		    		end_index = bufStrPls.indexOf(',', start_index);
		    		String bufLenght = bufStrPls.substring(start_index, end_index);
		    			    		
		    		//parse id 
		    		start_index = bufStrPls.indexOf("\"key\": \"", aux_ind);
		    		start_index += 8;
		    		end_index = bufStrPls.indexOf('\"', start_index);
		    		String bufId = bufStrPls.substring(start_index, end_index);		    		
		    		
		    		//parse image
		    		start_index = bufStrPls.indexOf("\"icon\": \"", aux_ind);
		    		start_index += 9;
		    		end_index = bufStrPls.indexOf('\"', start_index);
		    		String bufImage = bufStrPls.substring(start_index, end_index);		    	
		    		
		    		//add playlist
		    		PlaylistInfo playlist = new PlaylistInfo();
		    		playlist.id = bufId;
		    		playlist.name = bufName;
		    		playlist.numSongs = bufLenght;
		    		playlist.imageUrl = bufImage;
		    		playlists.add(playlist);
		    		
		    		start_index = aux_ind + 1;
		    		aux_ind = bufStrPls.indexOf("{", start_index);
		    		
		    		if(aux_ind == -1){
		    			break;
		    		}
		    		
		    		bufName=bufLenght=bufImage=bufId=null; 		    		
		    	}
		    	
	        }	        
	        rd.close();			
	    	
        } catch (IOException e){
    		throw new ServiceCommException(ServiceID.RDIO, ServiceErr.IO);
    	} catch (Exception e){
			Log.w(Util.APP, "Unknown error while trying to parse response from Rdio!", e);
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.UNKNOWN);
		}
				
		return playlists;
		
	}
	
	
}
