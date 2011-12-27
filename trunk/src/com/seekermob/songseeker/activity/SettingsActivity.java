package com.seekermob.songseeker.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.GroovesharkComm;
import com.seekermob.songseeker.comm.LastfmComm;
import com.seekermob.songseeker.comm.RdioComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.YouTubeComm;
import com.seekermob.songseeker.data.UserProfile;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.Util;

import de.umass.lastfm.Artist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
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

public class SettingsActivity extends PreferenceActivity implements OnCancelListener{

	private static final int LASTFM_USERNAME_DIAG = 0;
	private static final int IMPORT_FROM_DIAG = 1;
	private static final int IMPORTING_DIAG = 2;

	private ImportProfileLastfmTask importLastfmTask = null;
	private ImportProfileDeviceTask importDeviceTask = null;
	
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
            	showDialog(IMPORT_FROM_DIAG);
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

	@Override
	protected Dialog onCreateDialog(int id) {

		switch(id){
		case IMPORT_FROM_DIAG:		  	
			final CharSequence[] items = {"Device top artists", "Last.fm top artists"};

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Import from...");
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {			        
					
			    	removeDialog(IMPORT_FROM_DIAG);
					
					switch(item){
					case 0:
						importDeviceTask = (ImportProfileDeviceTask) new ImportProfileDeviceTask().execute();
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
	            	importLastfmTask = (ImportProfileLastfmTask) new ImportProfileLastfmTask().execute(textInput.getText().toString());	            	
	            }
	        }); 
			
			return dialog;		
		
		case IMPORTING_DIAG:			
			ProgressDialog imd = new ProgressDialog(this);
			imd.setMessage("Importing profile...");
			imd.setIndeterminate(true);
			imd.setCancelable(true);	
			imd.setOnCancelListener(this);
			return imd;
			
		default:
			return null;
		}
	}	
	
	private class ImportProfileLastfmTask extends AsyncTask<String, Void, Collection<Artist>>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			showDialog(IMPORTING_DIAG);
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
			removeDialog(IMPORTING_DIAG);
			
			if(err != null){
				Toast.makeText(SettingsActivity.this, err, Toast.LENGTH_SHORT).show();
				return;
			}
			
			ArrayList<String> artists = new ArrayList<String>();
			
			for(Artist topArtist : topArtists){
				artists.add(topArtist.getName());				
			}
			
			ProgressDialog pd = new ProgressDialog(SettingsActivity.this);
			pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pd.setMessage("Adding artists to profile...");
			pd.setCancelable(true);
			
			UserProfile.getInstance(getCacheDir()).addToProfile(artists, SettingsActivity.this, pd);
		}
	}
	
	private class ImportProfileDeviceTask extends AsyncTask<Void, Void, ArrayList<String>>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {
			showDialog(IMPORTING_DIAG);
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
			removeDialog(IMPORTING_DIAG);
			
			if(err != null){
				Toast.makeText(SettingsActivity.this, err, Toast.LENGTH_SHORT).show();
				return;
			}
			
			ProgressDialog pd = new ProgressDialog(SettingsActivity.this);
			pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pd.setMessage("Adding artists to profile...");
			pd.setCancelable(true);
			
			UserProfile.getInstance(getCacheDir()).addToProfile(topArtists, SettingsActivity.this, pd);
		}
	}	
	
	private class ClearCacheTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected void onPreExecute() {
			Toast.makeText(SettingsActivity.this, "Clearing cache, please wait...", Toast.LENGTH_SHORT).show();
		}

		@Override
		protected Void doInBackground(Void... params) {
        	File cacheDir = getCacheDir();
        	ImageLoader.getLoader(cacheDir).clearCache(cacheDir);        	
        	return null;
		}		
		
		@Override
		protected void onPostExecute(Void result) {
			Toast.makeText(getApplicationContext(), "Cache cleared!", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		if(importDeviceTask != null){
			importDeviceTask.cancel(true);
			importDeviceTask = null;
		}
		
		if(importLastfmTask != null){
			importLastfmTask.cancel(true);
			importLastfmTask = null;
		}	
		
		removeDialog(IMPORTING_DIAG);
		
		Toast.makeText(getApplicationContext(), getString(R.string.op_cancel_str), Toast.LENGTH_SHORT).show();
	}
}
