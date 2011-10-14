package com.android.songseeker.activity;

import com.android.songseeker.comm.GroovesharkComm;
import com.android.songseeker.comm.ServiceCommException;

import android.app.Activity;
import android.os.Bundle;

public class CreatePlaylistGroovesharkActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    try {
			GroovesharkComm.getComm().authorizeUser("heldergaray", "teste");
		} catch (ServiceCommException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	}

}
