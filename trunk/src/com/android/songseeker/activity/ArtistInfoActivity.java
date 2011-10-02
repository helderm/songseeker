package com.android.songseeker.activity;

import java.util.ArrayList;

import com.android.songseeker.R;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.comm.SevenDigitalComm;
import com.android.songseeker.data.ArtistInfo;
import com.android.songseeker.data.IdsParcel;
import com.android.songseeker.data.ReleaseInfo;
import com.android.songseeker.util.ImageLoader;

import android.app.Activity;
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
import android.widget.TextView;
import android.widget.Toast;

public class ArtistInfoActivity extends ListActivity {

	private ArtistInfo artist;
	
	private ArtistReleasesAdapter adapter = new ArtistReleasesAdapter();
	private static final int RELEASE_DETAILS_DIAG = 0;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    new GetArtistDetails().execute();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case RELEASE_DETAILS_DIAG:
			ProgressDialog pd = new ProgressDialog(this);
			pd.setMessage("Fetching song details...");
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
				//if(artist == null){
				//	IdsParcel releaseIdParcel = getIntent().getExtras().getParcelable("releaseId");
				//	artist = SevenDigitalComm.getComm().queryArtistDetails(releaseIdParcel.getIds().get(0));
				//}				
				
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

			//set image
			//ImageView coverart = (ImageView) header.findViewById(R.id.releaseinfo_coverArt);
			//ImageLoader.getLoader(getCacheDir()).DisplayImage(release.image, coverart, R.drawable.blankdisc);
			
			getListView().addHeaderView(header);

			//set adapter
			ArtistInfoActivity.this.setListAdapter(adapter); 	
		}		
	}	
	
	private class ArtistReleasesAdapter extends BaseAdapter {

		private ArrayList<ReleaseInfo> releases;    

		public ArtistReleasesAdapter() {    
			releases = null;
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
			View v = convertView;

			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.rec_song_row, null);
			}

			ReleaseInfo release = getItem(position);
			if (release != null) {
				TextView tt = (TextView) v.findViewById(R.id.recsong_firstLine);
				TextView bt = (TextView) v.findViewById(R.id.recsong_secondLine);
				ImageView coverart = (ImageView) v.findViewById(R.id.recsong_coverart);

				bt.setText(release.artist.name);
				tt.setText(release.name);

				ImageLoader.getLoader(getCacheDir()).DisplayImage(release.image, coverart, R.drawable.blankdisc);
			}

			return v;
		}

		public void setArtistReleases(ArrayList<ReleaseInfo> tp){
			this.releases = tp;
		}
	}

}
