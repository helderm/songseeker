package com.android.songseeker.comm;

import com.android.songseeker.comm.ServiceCommException.ServiceErr;
import com.android.songseeker.comm.ServiceCommException.ServiceID;
import com.android.songseeker.util.Util;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;

public class RdioComm {
	private static RdioComm comm = new RdioComm();
	
	private static final String CONSUMER_KEY = "e6axyuwxza2fxdhkbqbwdb2f";
	private static final String CONSUMER_SECRET = "5PcRraQyRk";		
	private static final String REQUEST_TOKEN = "http://api.rdio.com/oauth/request_token";
	private static final String ACCESS_TOKEN = "http://api.rdio.com/oauth/access_token";
	private static final String AUTHORIZE = "https://www.rdio.com/oauth/authorize";
	private static final String ENDPOINT = "http://api.rdio.com/1/";
	
	private OAuthConsumer consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
	private OAuthProvider provider = new CommonsHttpOAuthProvider(REQUEST_TOKEN, ACCESS_TOKEN, AUTHORIZE);
	
	private static String accessToken = null;
	private static String accessTokenSecret = null;	
	private static final String PREF_ACCESSTOKEN = "prefs.rdio.accesstoken";
	private static final String PREF_ACCESSTOKENSECRET = "prefs.rdio.accesstokensecret";	
		
	private RdioComm(){}
	
	public static RdioComm getComm(SharedPreferences settings){
				
		if(accessToken != null && accessTokenSecret != null)
			return comm;
				
		accessToken = settings.getString(PREF_ACCESSTOKEN, null);
		accessTokenSecret = settings.getString(PREF_ACCESSTOKENSECRET, null);
		
		return comm;
	}
	
	public static RdioComm getComm(){
		return comm;
	}
	
	public boolean isAuthorized(){
		if(accessToken == null || accessTokenSecret == null)
			return false;
		
		return true;
	}
	
	public void requestAuthorize(Activity a) throws ServiceCommException{
		String authUrl = null;
		
		Log.d(Util.APP,"Fetching request token from Rdio...");

		try {
			authUrl = provider.retrieveRequestToken(consumer, "oauth://checkin4me");
		} catch (OAuthCommunicationException e) {
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.IO);
		} catch (Exception e){
			Log.e(Util.APP, "Error while fetching request token to Rdio!", e);
			throw new ServiceCommException(ServiceID.RDIO, ServiceErr.UNKNOWN);
		}

		Log.d(Util.APP, "Request token: " + consumer.getToken());
		Log.d(Util.APP, "Token secret: " + consumer.getTokenSecret());
		Log.d(Util.APP, "AuthURL: " + authUrl);

		//TODO: Check how can we start this activity without adding it to the call stack
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
		i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		Log.d(Util.APP, "Requesting permission to Rdio... ");
		a.startActivity(i);		
	}
	
	
}
