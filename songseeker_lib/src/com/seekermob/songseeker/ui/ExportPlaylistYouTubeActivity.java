package com.seekermob.songseeker.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.ui.InputDialogFragment.OnTextEnteredListener;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class ExportPlaylistYouTubeActivity extends SherlockFragmentActivity implements OnTextEnteredListener{

	private Fragment mFragment;	
	public static final String FRAGMENT_TAG = "fragment";	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
		setContentView(R.layout.activity_singlepane_empty);

		//set action bar
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.export_youtube_title);	
		
        if(savedInstanceState == null) {
            mFragment = new ExportPlaylistYouTubeFragment();
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
	public void onDialogTextEntered(String text, String tag) {
		ExportPlaylistYouTubeFragment fragment = (ExportPlaylistYouTubeFragment) getSupportFragmentManager()
				.findFragmentByTag(FRAGMENT_TAG);
		fragment.exportToNewPlaylist(text);		
	}
}
