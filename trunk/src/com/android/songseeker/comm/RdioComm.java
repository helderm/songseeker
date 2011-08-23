package com.android.songseeker.comm;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
	private static final String PREF_ACCESSTOKEN = "prefs.rdio.accesstoken";
	private static final String PREF_ACCESSTOKENSECRET = "prefs.rdio.accesstokensecret";	
	
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
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_AUTHORISE_APP:
			if (resultCode == Rdio.RESULT_AUTHORISATION_ACCEPTED) {
				Log.i(Util.APP, "User authorised our app.");
				rdio.setTokenAndSecret(data);
			} else if (resultCode == Rdio.RESULT_AUTHORISATION_REJECTED) {
				Log.i(Util.APP, "User rejected our app.");
			}
			break;
		default:
			break;
		}
	}

	/*
	* Dispatched by the Rdio object once the setTokenAndSecret call has finished, and the credentials are
	* ready to be used to make API calls. The token & token secret are passed in so that you can
	* save/cache them for future re-use.
	* @see com.rdio.android.api.RdioListener#onRdioAuthorised(java.lang.String, java.lang.String)
	*/
	@Override
	public void onRdioAuthorised(String accessToken, String accessTokenSecret) {
		Log.i(Util.APP, "Application authorised, saving access token & secret.");
		Log.d(Util.APP, "Access token: " + accessToken);
		Log.d(Util.APP, "Access token secret: " + accessTokenSecret);

		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		Editor editor = settings.edit();
		editor.putString(PREF_ACCESSTOKEN, accessToken);
		editor.putString(PREF_ACCESSTOKENSECRET, accessTokenSecret);
		editor.commit();

		//create playlist
	}
	
	public void onRdioUserPlayingElsewhere() {
		Log.d(Util.APP, "onRdioUserPlayingElsewhere()");		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		//rdio.cleanup();
		//rdio = null;
	}
}
