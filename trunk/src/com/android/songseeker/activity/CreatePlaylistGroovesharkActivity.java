package com.android.songseeker.activity;

import com.android.songseeker.comm.GroovesharkComm;
import com.android.songseeker.comm.ServiceCommException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

public class CreatePlaylistGroovesharkActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    try {
	    	SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
			GroovesharkComm.getComm(settings).requestAuthorize("malakias23", "hema*poa5", settings);
			GroovesharkComm.getComm().getUserPlaylists();
		} catch (ServiceCommException e) {
			
		} catch (Exception e){}
	    
	}

}
