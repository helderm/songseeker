package com.android.songseeker.activity;

import java.util.ArrayList;

import com.android.songseeker.R;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.comm.SevenDigitalComm;
import com.android.songseeker.data.ArtistInfo;
import com.android.songseeker.data.IdsParcel;
import com.android.songseeker.data.ReleaseInfo;
import com.android.songseeker.data.SongInfo;
import com.android.songseeker.util.ImageLoader;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ArtistInfoActivity extends ListActivity {

	private ArtistInfo artist;
	
	private ArtistReleasesAdapter adapter;
	private static final int RELEASE_DETAILS_DIAG = 0;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    adapter = new ArtistReleasesAdapter();
	    new GetArtistDetails().execute();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case RELEASE_DETAILS_DIAG:
			ProgressDialog pd = new ProgressDialog(this);
			pd.setMessage("Fetching artist details...");
			pd.setIndeterminate(true);
			pd.setCancelable(true);
			return pd;
		default:
			return null;		    	
		}
	}
	
	private class GetArtistDetails extends AsyncTask<Void, Void, Void>{
		String err = null;		

		@Override
		protected void onPreExecute() {
			showDialog(RELEASE_DETAILS_DIAG);
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			ArrayList<ReleaseInfo> releases;

			try{
				artist = getIntent().getExtras().getParcelable("artistParcel");
				
				if(artist == null){
					IdsParcel songIdParcel = getIntent().getExtras().getParcelable("songId");	
					SongInfo song = SevenDigitalComm.getComm().querySongDetails(songIdParcel.getIds().get(0));
					artist = song.artist;
				}
				
				//will need to fetch artist details, since we dont have the artist image url 
				if(artist.image == null){				
					artist = SevenDigitalComm.getComm().queryArtistDetails(artist.id);
				}				
				
				releases = SevenDigitalComm.getComm().getArtistReleases(artist.id);
			}catch(ServiceCommException e){
				err = e.getMessage();		
				return null;
			}			
			
			adapter.setArtistReleases(releases);

			return null;
		}

		@Override
		protected void onPostExecute(Void s) {

			removeDialog(RELEASE_DETAILS_DIAG);

			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				ArtistInfoActivity.this.finish();
				return;
			}

			//set content for main screen
			setContentView(R.layout.listview);	
			
			//set album info header
			LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			LinearLayout header = (LinearLayout)inflater.inflate(R.layout.artist_info, null);
			
			TextView artistName = (TextView) header.findViewById(R.id.artistinfo_artistName);
			artistName.setText(artist.name);

			//set buy button
			Button buy = (Button)header.findViewById(R.id.artistinfo_buy);
			buy.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(artist.buyUrl));
					intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
			});
			
			//set share button
			Button share = (Button)header.findViewById(R.id.artistinfo_share);
			share.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					final Intent intent = new Intent(Intent.ACTION_SEND);					 
					intent.setType("text/plain");
					intent.putExtra(Intent.EXTRA_SUBJECT, "New artist!");
					intent.putExtra(Intent.EXTRA_TEXT, "I discovered the artist '"+ artist.name   
							+ "' using the Song Seeker app for Android! Check it out! "+ artist.buyUrl);
					startActivity(Intent.createChooser(intent, "Share using..."));
				}				
			});
			
			TextView tvBio = (TextView)header.findViewById(R.id.artistinfo_biography);
			tvBio.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
				
					Intent i = new Intent(ArtistInfoActivity.this, ArtistBioActivity.class);
					i.putExtra("artistParcel", artist);
					startActivity(i);
				}
			});

			TextView tvNews = (TextView)header.findViewById(R.id.artistinfo_news);
			tvNews.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
				
					Intent i = new Intent(ArtistInfoActivity.this, ArtistNewsActivity.class);
					i.putExtra("artistParcel", artist);
					startActivity(i);
				}
			});	
			
			TextView tvSimilar = (TextView)header.findViewById(R.id.artistinfo_similar);
			tvSimilar.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
				
					Intent i = new Intent(ArtistInfoActivity.this, SimilarArtistsActivity.class);
					i.putExtra("artistParcel", artist);
					startActivity(i);
				}
			});	
			
			//set image
			ImageView coverart = (ImageView) header.findViewById(R.id.artistinfo_image);
			ImageLoader.getLoader(getCacheDir()).DisplayImage(artist.image, coverart, R.drawable.blankdisc);
			
			getListView().addHeaderView(header);

			//set adapter
			ArtistInfoActivity.this.setListAdapter(adapter); 	
		}		
	}	
	
	private class ArtistReleasesAdapter extends BaseAdapter {

		private ArrayList<ReleaseInfo> releases;    
		private LayoutInflater inflater;
		
		public ArtistReleasesAdapter() {    
			releases = null;
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public int getCount() {
			if(releases == null)
				return 0;

			return releases.size();
		}

		public ReleaseInfo getItem(int position) {
			return releases.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		/*public class ViewHolder{
	    	public TextView username;
	    	public TextView message;
	    	public ImageView image;
	    }*/

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
	    	
			if (convertView == null) {			    
				convertView = inflater.inflate(R.layout.rec_song_row, null);
				
				holder = new ViewHolder();
				holder.topText = (TextView) convertView.findViewById(R.id.recsong_firstLine);
			    holder.botText = (TextView) convertView.findViewById(R.id.recsong_secondLine);
			    holder.coverArt = (ImageView) convertView.findViewById(R.id.recsong_coverart);
			    holder.playPause = (ImageView) convertView.findViewById(R.id.recsong_playpause);	
			    
			    convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}

			ReleaseInfo release = getItem(position);
			if (release != null) {				
				holder.botText.setText(release.artist.name);
				holder.topText.setText(release.name);
				holder.playPause.setVisibility(View.GONE);
				
				ImageLoader.getLoader(getCacheDir()).DisplayImage(release.image, holder.coverArt, R.drawable.blankdisc);
			}

			return convertView;
		}

		public void setArtistReleases(ArrayList<ReleaseInfo> tp){
			this.releases = tp;
		}
		
	    public class ViewHolder{
	    	public TextView topText;
	    	public TextView botText;
	    	public ImageView coverArt;
	    	public ImageView playPause;
	    }
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		ReleaseInfo ri = adapter.getItem(position-1);
		
		//set the image in the parcel to avoid calling the ws again
		if(artist.image != null && artist.id.equalsIgnoreCase(ri.artist.id)){
			ri.artist.image = artist.image;
		}		
		
		Intent i = new Intent(ArtistInfoActivity.this, MusicInfoTab.class);
		i.putExtra("releaseParcel", ri);
		startActivity(i);
	}
}
