package com.seekermob.songseeker.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.ui.AuthDialogFragment.OnUserPassEnteredListener;
import com.seekermob.songseeker.ui.InputDialogFragment.OnTextEnteredListener;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class ExportPlaylistGroovesharkActivity extends SherlockFragmentActivity implements OnUserPassEnteredListener, OnTextEnteredListener{

	private Fragment mFragment;	
	private static final String FRAGMENT_TAG = "fragment";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_singlepane_empty);

		//set action bar
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.export_grooveshark_title);	
		
        if(savedInstanceState == null) {
            mFragment = new ExportPlaylistGroovesharkFragment();
            mFragment.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction().replace(R.id.root_container, mFragment, FRAGMENT_TAG)
                    .commit();
        }
	}

	public boolean onOptionsItemSelected(MenuItem item)	{

		switch (item.getItemId()){
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onUserPassEntered(String user, String pass){
		ExportPlaylistGroovesharkFragment fragment = (ExportPlaylistGroovesharkFragment) getSupportFragmentManager()
				.findFragmentByTag(FRAGMENT_TAG);
		
		fragment.authorize(user, pass);
	}

	@Override
	public void onDialogTextEntered(String text, String tag) {
		ExportPlaylistGroovesharkFragment fragment = (ExportPlaylistGroovesharkFragment) getSupportFragmentManager()
				.findFragmentByTag(FRAGMENT_TAG);
		fragment.exportToNewPlaylist(text);
	}
}
