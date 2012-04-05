package com.seekermob.songseeker.ui;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.seekermob.songseeker.R;

import android.os.Bundle;

public class ProfileFragment extends SherlockListFragment {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.profile_menu, menu);		
		super.onCreateOptionsMenu(menu, inflater);
	}

}
