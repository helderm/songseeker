package com.seekermob.songseeker.activity;


import java.util.ArrayList;
import java.util.Collection;

import com.google.android.apps.analytics.easytracking.TrackedListActivity;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.LastfmComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.UserProfile;
import com.seekermob.songseeker.data.UserProfile.ArtistProfile;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;
import com.seekermob.songseeker.util.Util;

import de.umass.lastfm.Artist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ProfileActivity extends TrackedListActivity implements OnCancelListener{
	
	private ListAdapter adapter;
	private ImportProfileLastfmTask importLastfmTask = null;
	private ImportProfileDeviceTask importDeviceTask = null;
	
	private static final int ADD_ARTIST_DIAG = 0;
	private static final int IMPORT_FROM_DIAG = 1;
	private static final int LASTFM_USERNAME_DIAG = 2;
	private static final int IMPORTING_DIAG = 3;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
		// Use a custom layout file
		setContentView(R.layout.profile);

		// Tell the list view which view to display when the list is empty
		getListView().setEmptyView(findViewById(R.id.empty));

		// Set up our adapter
		adapter = new ListAdapter();
		setListAdapter(adapter);
		
		registerForContextMenu(getListView());
		
		// set 'add' button
		Button add = (Button)findViewById(R.id.profile_add);
		add.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(ADD_ARTIST_DIAG);
			}
		});
		
		// set 'import' button
		Button importb = (Button)findViewById(R.id.profile_import);
		importb.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(IMPORT_FROM_DIAG);
			}
		});
	}

	public class ListAdapter extends BaseAdapter {

		private UserProfile prof;	 
		private LayoutInflater inflater;

		public ListAdapter() {    
			prof = UserProfile.getInstance(getCacheDir());
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public int getCount() {
			if(prof == null || prof.getProfile() == null)
				return 0;

			return prof.getProfile().artists.size();
		}

		public ArtistProfile getItem(int position) {
			return prof.getProfile().artists.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if(convertView == null) {
				convertView = inflater.inflate(R.layout.artist_row, null);

				holder = new ViewHolder();
				holder.line = (TextView) convertView.findViewById(R.id.line);
				holder.coverArt = (ImageView) convertView.findViewById(R.id.coverart);

				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}

			final ArtistProfile artist = getItem(position);
			if(artist != null) {

				holder.line.setText(artist.name);

				ImageLoader.getLoader(getCacheDir()).DisplayImage(artist.image, holder.coverArt, R.drawable.ic_disc_stub, ImageSize.SMALL);	
			}

			return convertView;
		}

		private class ViewHolder{
			public TextView line;
			public ImageView coverArt;
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {

		switch(id){
		case ADD_ARTIST_DIAG:
			Dialog addDialog = new Dialog(this);
			addDialog.setContentView(R.layout.new_playlist_diag);
			addDialog.setTitle("Artist Name:");
			
			Button create_but = (Button)addDialog.findViewById(R.id.create_pl_but);
			create_but.setText("Add");
			
			create_but.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
	               	View p = (View)v.getParent();	            	
	            	View parent = (View)p.getParent();	            	
	            	EditText textInput = (EditText) parent.findViewById(R.id.pl_name_input); 
	            	
	                //check if the edit text is empty
	            	if(textInput.getText().toString().compareTo("") == 0){
	            		    		
	            		Toast.makeText(ProfileActivity.this, 
	            						getResources().getText(R.string.invalid_args_str), Toast.LENGTH_SHORT).show();
	            		
	            		removeDialog(ADD_ARTIST_DIAG);
	            		return;
	            	}
	            	
	            	//remove the soft input window from view
	            	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
	            	imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0); 
	            		            	
	            	removeDialog(ADD_ARTIST_DIAG);
	            	
	            	ArrayList<String> names = new ArrayList<String>();
	            	names.add(textInput.getText().toString()); 	            	
	            	UserProfile.getInstance(getCacheDir()).addToProfile(names, ProfileActivity.this, adapter);	            	
	            }
	        }); 
			
			return addDialog;
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
			
			Button ok = (Button)dialog.findViewById(R.id.create_pl_but);			
			ok.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
	               	View p = (View)v.getParent();	            	
	            	View parent = (View)p.getParent();	            	
	            	EditText textInput = (EditText) parent.findViewById(R.id.pl_name_input); 
	            	
	                //check if the edit text is empty
	            	if(textInput.getText().toString().compareTo("") == 0){
	            		    		
	            		Toast toast = Toast.makeText(ProfileActivity.this, 
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
				Toast.makeText(ProfileActivity.this, err, Toast.LENGTH_SHORT).show();
				return;
			}
			
			ArrayList<String> artists = new ArrayList<String>();
			
			for(Artist topArtist : topArtists){
				artists.add(topArtist.getName());				
			}
			
			ProgressDialog pd = new ProgressDialog(ProfileActivity.this);
			pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pd.setMessage("Adding artists to profile...");
			pd.setCancelable(true);
			
			UserProfile.getInstance(getCacheDir()).addToProfile(artists, ProfileActivity.this, adapter, pd);
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
				topArtists = Util.getArtistsFromDevice(ProfileActivity.this);
			} catch (Exception e) {
				err = "Failed to get artists from device!";
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
				Toast.makeText(ProfileActivity.this, err, Toast.LENGTH_SHORT).show();
				return;
			}
			
			ProgressDialog pd = new ProgressDialog(ProfileActivity.this);
			pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pd.setMessage("Adding artists to profile...");
			pd.setCancelable(true);
			
			UserProfile.getInstance(getCacheDir()).addToProfile(topArtists, ProfileActivity.this, adapter, pd);
		}
	}	
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		ArtistProfile artistProf = adapter.prof.getProfile().artists.get(position);
		
		ArtistInfo artistInfo = new ArtistInfo();
		artistInfo.id = artistProf.id;
		artistInfo.name = artistProf.name;
		artistInfo.image = artistProf.image;
		artistInfo.buyUrl = artistProf.buyUrl;
		
		Intent i = new Intent(ProfileActivity.this, MusicInfoTab.class);
		i.putExtra("artistParcel", artistInfo);
		startActivity(i);		
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.setHeaderTitle("Options");
		
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.profile_contextmenu, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		if(item.getItemId() == R.id.remove_artist){
			UserProfile.getInstance(getCacheDir()).removeArtistFromProfile(info.position, (BaseAdapter)getListAdapter());
			return true;
		}
		
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCancel(DialogInterface arg0) {
		if(importDeviceTask != null){
			importDeviceTask.cancel(true);
			importDeviceTask = null;
			removeDialog(IMPORTING_DIAG);
		}else if(importLastfmTask != null){
			importLastfmTask.cancel(true);
			importLastfmTask = null;
			removeDialog(IMPORTING_DIAG);
		}			
		
		//adapter.notifyDataSetChanged();
		//Toast.makeText(getApplicationContext(), getString(R.string.op_cancel_str), Toast.LENGTH_SHORT).show();		
	}
}
