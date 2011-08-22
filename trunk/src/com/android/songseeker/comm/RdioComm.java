package com.android.songseeker.comm;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.songseeker.util.Util;
import com.rdio.android.api.Rdio;
import com.rdio.android.api.RdioListener;
import com.rdio.android.api.RdioSubscriptionType;


public class RdioComm extends Activity implements RdioListener{
	private static Rdio rdio = null;
	
	private static final String appKey = "e6axyuwxza2fxdhkbqbwdb2f";
	private static final String appSecret = "5PcRraQyRk";	
	
	private static String accessToken = null;
	private static String accessTokenSecret = null;	
	private static final String PREF_ACCESSTOKEN = "prefs.accesstoken";
	private static final String PREF_ACCESSTOKENSECRET = "prefs.accesstokensecret";	
	
	private static final int REQUEST_AUTHORISE_APP = 100;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(rdio == null){
			SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
			accessToken = settings.getString(PREF_ACCESSTOKEN, null);
			accessTokenSecret = settings.getString(PREF_ACCESSTOKENSECRET, null);

			if (accessToken == null || accessTokenSecret == null) {
				// If either one is null, reset both of them
				accessToken = accessTokenSecret = null;
			} else {
				Log.d(Util.APP, "Found cached credentials:");
				Log.d(Util.APP, "Access token: " + accessToken);
				Log.d(Util.APP, "Access token secret: " + accessTokenSecret);
			}

			// Initialise our API object
			rdio = new Rdio(appKey, appSecret, accessToken, accessTokenSecret, this, this);				
		}else {
			RdioSubscriptionType t = rdio.getSubscriptionState();
			if(t == RdioSubscriptionType.ANONYMOUS){
		
				Toast toast = Toast.makeText(getApplicationContext(), "Rdio app not found!", Toast.LENGTH_LONG);
	    		toast.show();        		
	    		
	    		RdioComm.this.finish();
			}
		}
		
	}

	public void onRdioAuthorised(String arg0, String arg1) {
		Log.d(Util.APP, "onRdioAuthorised()");
		
	}

	public void onRdioReady() {
		Log.d(Util.APP, "onRdioReady()");		
	}

	public void onRdioUserAppApprovalNeeded(Intent authorisationIntent) {
		try {
			startActivityForResult(authorisationIntent, REQUEST_AUTHORISE_APP);
		} catch (ActivityNotFoundException e) {
			// Rdio app not found
			Toast toast = Toast.makeText(getApplicationContext(), "Rdio app not found!", Toast.LENGTH_LONG);
    		toast.show();        		
    		
    		RdioComm.this.finish();
		}				
	}

	public void onRdioUserPlayingElsewhere() {
		Log.d(Util.APP, "onRdioUserPlayingElsewhere()");		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		rdio.cleanup();
		//rdio = null;
	}
}
