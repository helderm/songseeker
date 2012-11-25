package com.seekermob.songseeker.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.ui.InputDialogFragment.OnTextEnteredListener;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class ExportPlaylistRdioActivity extends SherlockFragmentActivity implements OnTextEnteredListener{
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
		actionBar.setTitle(R.string.export_rdio_title);	
		
        if(savedInstanceState == null) {
            mFragment = new ExportPlaylistRdioFragment();
            mFragment.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction().replace(R.id.root_container, mFragment, FRAGMENT_TAG)
                    .commit();
        }
	}
	
	public boolean onOptionsItemSelected(MenuItem item)	{

		switch (item.getItemId()){
		case android.R.id.home:
            final Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onDialogTextEntered(String text, String tag) {
		ExportPlaylistRdioFragment fragment = (ExportPlaylistRdioFragment) getSupportFragmentManager()
				.findFragmentByTag(FRAGMENT_TAG);
		fragment.exportToNewPlaylist(text);		
	}
	
	@Override
    protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Uri uri = intent.getData();
		
		//Verificando se a chamada vem realmente do callback esperado
		if (uri == null || !uri.toString().contains("oauth")) {
			return;
		}
		
		String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);

		ExportPlaylistRdioFragment fragment = (ExportPlaylistRdioFragment) getSupportFragmentManager()
				.findFragmentByTag(FRAGMENT_TAG);
		fragment.authorize(verifier);
	}

}
