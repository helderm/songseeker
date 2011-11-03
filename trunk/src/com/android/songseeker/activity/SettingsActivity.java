package com.android.songseeker.activity;

import java.util.ArrayList;
import java.util.Collection;

import com.android.songseeker.R;
import com.android.songseeker.comm.GroovesharkComm;
import com.android.songseeker.comm.LastfmComm;
import com.android.songseeker.comm.RdioComm;
import com.android.songseeker.comm.YouTubeComm;
import com.android.songseeker.data.UserProfile;
import com.android.songseeker.util.Util;

import de.umass.lastfm.Artist;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {

	private static final int NEW_PLAYLIST_DIAG = 0;

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
		
		/* TODO
		Preference importProfile = (Preference) findPreference("import_prof");		 
		importProfile.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	showDialog(NEW_PLAYLIST_DIAG);
            	return true;
            }
        });*/		

	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch(id){
		case NEW_PLAYLIST_DIAG:
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
	            		removeDialog(NEW_PLAYLIST_DIAG);
	            		return;
	            	}
	            	
	            	//remove the soft input window from view
	            	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
	            	imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0); 
	            		            	
	            	removeDialog(NEW_PLAYLIST_DIAG);	            	
	            	new ImportProfileTask().execute(textInput.getText().toString());	            	
	            }
	        }); 
			
			return dialog;		
		default:
			return null;
		}
	}	
	
	private class ImportProfileTask extends AsyncTask<String, Void, Void>{

		@Override
		protected void onPreExecute() {
			Toast.makeText(SettingsActivity.this, "Importing profile, please wait...", Toast.LENGTH_LONG).show();
		}
		
		@Override
		protected Void doInBackground(String... params) {
			Collection<Artist> topArtists = LastfmComm.getComm().getTopArtists(params[0]);
			ArrayList<String> artists = new ArrayList<String>();
			
			for(Artist topArtist : topArtists){
				artists.add(topArtist.getName());				
			}
			
			UserProfile.getInstance(getCacheDir()).addToProfile(artists, SettingsActivity.this, null);
			
			return null;
		}		
	}
}
