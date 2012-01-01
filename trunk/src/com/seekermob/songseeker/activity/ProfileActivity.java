package com.seekermob.songseeker.activity;


import java.util.ArrayList;

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.UserProfile;
import com.seekermob.songseeker.data.UserProfile.ArtistProfile;
import com.seekermob.songseeker.util.ImageLoader;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
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

public class ProfileActivity extends ListActivity {
	
	private ListAdapter adapter;
	
	private static final int NEW_PLAYLIST_DIAG = 0;
	
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
				showDialog(NEW_PLAYLIST_DIAG);
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

				ImageLoader.getLoader(getCacheDir()).DisplayImage(artist.image, holder.coverArt, R.drawable.ic_disc_stub);	
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
		case NEW_PLAYLIST_DIAG:
			Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.new_playlist_diag);
			dialog.setTitle("Artist Name:");
			
			Button create_but = (Button)dialog.findViewById(R.id.create_pl_but);
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
	            		
	            		removeDialog(NEW_PLAYLIST_DIAG);
	            		return;
	            	}
	            	
	            	//remove the soft input window from view
	            	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
	            	imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0); 
	            		            	
	            	removeDialog(NEW_PLAYLIST_DIAG);
	            	
	            	ArrayList<String> names = new ArrayList<String>();
	            	names.add(textInput.getText().toString()); 	            	
	            	UserProfile.getInstance(getCacheDir()).addToProfile(names, ProfileActivity.this, adapter);	            	
	            }
	        }); 
			
			return dialog;
		default:
			return null;
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
}
