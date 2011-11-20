package com.android.songseeker.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import com.android.songseeker.R;
import com.android.songseeker.comm.GroovesharkComm;
import com.android.songseeker.comm.LastfmComm;
import com.android.songseeker.comm.RdioComm;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.comm.YouTubeComm;
import com.android.songseeker.data.UserProfile;
import com.android.songseeker.util.ImageLoader;
import com.android.songseeker.util.Util;

import de.umass.lastfm.Artist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {

	private static final int LASTFM_USERNAME_DIAG = 0;
	private static final int IMPORT_DIAG = 1;

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
            	UserProfile.getInstance(getCacheDir()).clearProfile();
            	Toast.makeText(getApplicationContext(), "Profile cleared!", Toast.LENGTH_SHORT).show();
            	return true;
            }
        });
		
		Preference importProfile = (Preference) findPreference("import_prof");		 
		importProfile.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	showDialog(IMPORT_DIAG);
            	return true;
            }
        });	
		
		//clear_cache
		Preference clearCache = (Preference) findPreference("clear_cache");		 
		clearCache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	File cacheDir = getCacheDir();
            	ImageLoader.getLoader(cacheDir).clearCache(cacheDir);
            	Toast.makeText(getApplicationContext(), "Cache cleared!", Toast.LENGTH_SHORT).show();
            	return true;
            }
        });
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch(id){
		case IMPORT_DIAG:		  	
			final CharSequence[] items = {"Device top artists", "Last.fm top artists"};

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Import from...");
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {			        
					
			    	removeDialog(IMPORT_DIAG);
					
					switch(item){
					case 0:
						new ImportProfileDeviceTask().execute();
						break;
					case 1:
						showDialog(LASTFM_USERNAME_DIAG);
						break;
					default:
						return;							
					}		    	
  	
			    }
			});
			AlertDialog alert = builder.create();
			return alert;	
		
		case LASTFM_USERNAME_DIAG:
			Dialog dialog = new Dialog(this);

			dialog.setContentView(R.layout.new_playlist_diag);
			dialog.setTitle("Last.fm username:");
			
			Button create_but = (Button)dialog.findViewById(R.id.create_pl_but);			
			create_but.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
	               	View p = (View)v.getParent();	            	
	            	View parent = (View)p.getParent();	            	
	            	EditText textInput = (EditText) parent.findViewById(R.id.pl_name_input); 
	            	
	                //check if the edit text is empty
	            	if(textInput.getText().toString().compareTo("") == 0){
	            		    		
	            		Toast toast = Toast.makeText(SettingsActivity.this, 
	            						getResources().getText(R.string.invalid_args_str), Toast.LENGTH_SHORT);
	            		toast.show();
	            		removeDialog(LASTFM_USERNAME_DIAG);
	            		return;
	            	}
	            	
	            	//remove the soft input window from view
	            	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
	            	imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0); 
	            		            	
	            	removeDialog(LASTFM_USERNAME_DIAG);	            	
	            	new ImportProfileLastfmTask().execute(textInput.getText().toString());	            	
	            }
	        }); 
			
			return dialog;		
		default:
			return null;
		}
	}	
	
	private class ImportProfileLastfmTask extends AsyncTask<String, Void, Collection<Artist>>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			Toast.makeText(SettingsActivity.this, "Importing profile, please wait...", Toast.LENGTH_LONG).show();
		}
		
		@Override
		protected Collection<Artist> doInBackground(String... params) {
			Collection<Artist> topArtists;
			try {
				topArtists = LastfmComm.getComm().getTopArtists(params[0]);
			} catch (ServiceCommException e) {
				err = e.getMessage();
				return null;
			}
			
			return topArtists;
		}
		
		@Override
		protected void onPostExecute(Collection<Artist> topArtists) {
			if(err != null){
				Toast.makeText(SettingsActivity.this, err, Toast.LENGTH_SHORT).show();
				return;
			}
			
			ArrayList<String> artists = new ArrayList<String>();
			
			for(Artist topArtist : topArtists){
				artists.add(topArtist.getName());				
			}
			
			UserProfile.getInstance(getCacheDir()).addToProfile(artists, SettingsActivity.this, null);
		}
	}
	
	private class ImportProfileDeviceTask extends AsyncTask<Void, Void, ArrayList<String>>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			Toast.makeText(SettingsActivity.this, "Importing profile, please wait...", Toast.LENGTH_LONG).show();
		}
		
		@Override
		protected ArrayList<String> doInBackground(Void... params) {
			ArrayList<String> topArtists;
			try {
				topArtists = Util.getArtistsFromDevice(SettingsActivity.this);
			} catch (Exception e) {
				err = "Failed to get artists from device!";
				Log.e(Util.APP, e.getMessage(), e);
				return null;
			}
			
			if(topArtists.isEmpty()){
				err = "No artist found in your device!";
				return null;
			}
				
			
			return topArtists;
		}
		
		@Override
		protected void onPostExecute(ArrayList<String> topArtists) {
			if(err != null){
				Toast.makeText(SettingsActivity.this, err, Toast.LENGTH_SHORT).show();
				return;
			}
			
			UserProfile.getInstance(getCacheDir()).addToProfile(topArtists, SettingsActivity.this, null);
		}
	}	
}
