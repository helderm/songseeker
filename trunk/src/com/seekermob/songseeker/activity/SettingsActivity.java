package com.seekermob.songseeker.activity;

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.GroovesharkComm;
import com.seekermob.songseeker.comm.LastfmComm;
import com.seekermob.songseeker.comm.RdioComm;
import com.seekermob.songseeker.comm.YouTubeComm;
import com.seekermob.songseeker.data.UserProfile;
import com.seekermob.songseeker.util.FileCache;
import com.seekermob.songseeker.util.TrackedPreferenceActivity;
import com.seekermob.songseeker.util.Util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

public class SettingsActivity extends TrackedPreferenceActivity{

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.layout.settings);

		Preference clearAuth = (Preference) findPreference("clear_auth");
		clearAuth.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				SharedPreferences settings = getApplicationContext().getSharedPreferences(Util.APP, Context.MODE_PRIVATE);

				GroovesharkComm.getComm().cleanAuth(settings);
				RdioComm.getComm().cleanAuthTokens(settings);
				YouTubeComm.getComm().unauthorizeUser(settings);
				LastfmComm.getComm().cleanAuth(settings);

				Toast.makeText(getApplicationContext(), "Authentications cleared!", Toast.LENGTH_SHORT).show();
				return true;
			}
		});		

		Preference clearProfile = (Preference) findPreference("clear_prof");
		clearProfile.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				UserProfile.getInstance(SettingsActivity.this).clearProfile(SettingsActivity.this);
				Toast.makeText(getApplicationContext(), "Profile cleared!", Toast.LENGTH_SHORT).show();
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

	private class ClearCacheTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected void onPreExecute() {
			Toast.makeText(SettingsActivity.this, "Clearing cache, please wait...", Toast.LENGTH_SHORT).show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			FileCache.getCache(getApplicationContext()).clear();        	
			return null;
		}		

		@Override
		protected void onPostExecute(Void result) {
			Toast.makeText(getApplicationContext(), "Cache cleared!", Toast.LENGTH_SHORT).show();
		}
	}
}
