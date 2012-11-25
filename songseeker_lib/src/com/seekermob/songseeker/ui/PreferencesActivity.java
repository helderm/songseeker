package com.seekermob.songseeker.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.GroovesharkComm;
import com.seekermob.songseeker.comm.LastfmComm;
import com.seekermob.songseeker.comm.RdioComm;
import com.seekermob.songseeker.comm.YouTubeComm;
import com.seekermob.songseeker.util.FileCache;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class PreferencesActivity extends SherlockPreferenceActivity {

	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    final PreferenceActivity activity = this;
	    
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.layout.preferences);
		
		final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.settings);        

		Preference clearAuth = (Preference) findPreference("clear_auth");
		clearAuth.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				GroovesharkComm.getComm().cleanAuth(activity);
				RdioComm.getComm().cleanAuthTokens(activity);
				YouTubeComm.getComm().unauthorizeUser(activity);
				LastfmComm.getComm().cleanAuth(activity);

				Toast.makeText(getApplicationContext(), R.string.auth_cleared, Toast.LENGTH_SHORT).show();
				return true;
			}
		});	
		
		//clear_cache
		Preference clearCache = (Preference) findPreference("clear_cache");		 
		clearCache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				new ClearCacheTask().execute();
				return true;
			}
		});
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
	
	private class ClearCacheTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected void onPreExecute() {
			Toast.makeText(PreferencesActivity.this, R.string.clearing_cache, Toast.LENGTH_SHORT).show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			FileCache.getCache().clear(getApplicationContext());        	
			return null;
		}		

		@Override
		protected void onPostExecute(Void result) {
			Toast.makeText(getApplicationContext(), R.string.cache_cleared, Toast.LENGTH_SHORT).show();
		}
	}

}
