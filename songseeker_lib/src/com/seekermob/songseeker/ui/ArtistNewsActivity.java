package com.seekermob.songseeker.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.seekermob.songseeker.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class ArtistNewsActivity extends SherlockFragmentActivity {
	
	private Fragment mFragment;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_singlepane_empty);

		//set action bar
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.artist_news_title);	
		
        if(savedInstanceState == null) {
            mFragment = new ArtistNewsFragment();
            mFragment.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction().replace(R.id.root_container, mFragment)
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

}
