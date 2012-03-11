package com.seekermob.songseeker.activity;

import java.util.ArrayList;

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.EchoNestComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;
import com.seekermob.songseeker.util.Util;
import com.echonest.api.v4.Artist;
import com.echonest.api.v4.EchoNestException;
import com.google.android.apps.analytics.easytracking.TrackedListActivity;

import android.app.Dialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SimilarArtistsActivity extends TrackedListActivity {

	private static final int SIMILAR_ARTISTS_DIAG = 0;
	private SimilarArtistsAdapter adapter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Use a custom layout file
		setContentView(R.layout.listview);

		// Tell the list view which view to display when the list is empty
		getListView().setEmptyView(findViewById(R.id.empty));

		//set EN logo at the end of the listview
		setLogoFooter();
				
		// Set up our adapter
		adapter = new SimilarArtistsAdapter();
		setListAdapter(adapter);

		//check orientation change
		@SuppressWarnings("unchecked")
		ArrayList<ArtistInfo> savedArtists = (ArrayList<ArtistInfo>) getLastNonConfigurationInstance();
		
		if(savedArtists == null)
			new GetSimilarArtists().execute();
		else
			adapter.setSimilarArtists(savedArtists);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case SIMILAR_ARTISTS_DIAG:
			ProgressDialog pd = new ProgressDialog(this);
			pd.setMessage("Fetching similar artists...");
			pd.setIndeterminate(true);
			pd.setCancelable(true);
			return pd;
		default:
			return null;		    	
		}
	}

	private class GetSimilarArtists extends AsyncTask<Void, Void, ArrayList<ArtistInfo>>{
		String err = null;

		@Override
		protected void onPreExecute() {
			showDialog(SIMILAR_ARTISTS_DIAG);
		}

		@Override
		protected ArrayList<ArtistInfo> doInBackground(Void... arg0) {
			ArrayList<Artist> similar;
			ArrayList<ArtistInfo> artists = new ArrayList<ArtistInfo>();
			ArtistInfo artist;

			try {
				ArtistInfo artistInfo = getIntent().getExtras().getParcelable("artistParcel");
				similar = EchoNestComm.getComm().getSimilarArtistsFromBucket(artistInfo.id, 10);

				for(Artist a : similar){
					String foreignId = a.getForeignID(EchoNestComm.SEVEN_DIGITAL);

					try{
						artist = SevenDigitalComm.getComm().queryArtistDetails(foreignId.split(":")[2]);
					}catch (ServiceCommException e) {
						Log.i(Util.APP, "Artist ["+a.getForeignID(EchoNestComm.SEVEN_DIGITAL)+"] not found in 7digital, skipping...");
						continue;
					}

					artists.add(artist);
				}

			} catch (ServiceCommException e) {
				err = e.getMessage();
				return null;
			} catch (EchoNestException e) {
				err = e.getMessage();
				return null;
			}			

			return artists;
		}

		@Override
		protected void onPostExecute(ArrayList<ArtistInfo> result) {
			removeDialog(SIMILAR_ARTISTS_DIAG);

			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				SimilarArtistsActivity.this.finish();
				return;
			}

			adapter.setSimilarArtists(result);
		}		
	}

	private class SimilarArtistsAdapter extends BaseAdapter {

		private ArrayList<ArtistInfo> similarArtists;	 
		private LayoutInflater inflater;

		public SimilarArtistsAdapter() {    
			similarArtists = null;
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public int getCount() {
			if(similarArtists == null)
				return 0;

			return similarArtists.size();
		}

		public ArtistInfo getItem(int position) {
			return similarArtists.get(position);
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

			final ArtistInfo artist = getItem(position);
			if(artist != null) {

				holder.line.setText(artist.name);			

				try{
					ImageLoader.getLoader().DisplayImage(artist.image, holder.coverArt, R.drawable.ic_disc_stub, ImageSize.SMALL);
				}catch(IndexOutOfBoundsException e){
					Log.i(Util.APP, "Unable to fetch the release image from Echo Nest!");
				}
			}

			return convertView;
		}

		public void setSimilarArtists(ArrayList<ArtistInfo> sa){
			this.similarArtists = sa;
			notifyDataSetChanged();
		}

		private class ViewHolder{
			public TextView line;
			public ImageView coverArt;
		}
	}

	private void setLogoFooter() {		
		
		//set logo footer
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout footer = (LinearLayout)inflater.inflate(R.layout.logo, null);
		
		ImageView logo = (ImageView)footer.findViewById(R.id.logo);
		logo.setImageResource(R.drawable.echonest_logo_pwd);
		
		logo.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Util.ECHONEST_URL));
				intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		});
		
		getListView().addFooterView(footer);		
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		ArtistInfo artist = adapter.getItem(position);
		
		Intent i = new Intent(SimilarArtistsActivity.this, MusicInfoTab.class);
		i.putExtra("artistParcel", artist);
		startActivity(i);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return adapter.similarArtists;
	}
}
