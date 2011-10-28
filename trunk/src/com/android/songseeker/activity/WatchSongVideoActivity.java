package com.android.songseeker.activity;

import java.util.ArrayList;

import com.android.songseeker.R;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.comm.YouTubeComm;
import com.android.songseeker.comm.YouTubeComm.VideoFeed;
import com.android.songseeker.data.ArtistInfo;
import com.android.songseeker.data.SongInfo;
import com.android.songseeker.util.ImageLoader;
import com.android.songseeker.util.Util;

import android.app.Dialog;
import android.app.ListActivity;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class WatchSongVideoActivity extends ListActivity {

	private ListAdapter adapter;
	
	private static final int FETCH_VIDEOS_DIAG = 0;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
		
	    // Use a custom layout file
		setContentView(R.layout.listview);

		// Tell the list view which view to display when the list is empty
		getListView().setEmptyView(findViewById(R.id.empty));
		
		// Set up our adapter
		adapter = new ListAdapter();
		setListAdapter(adapter);
		
		new GetVideosTask().execute();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case FETCH_VIDEOS_DIAG:
			ProgressDialog pd = new ProgressDialog(this);
			pd.setMessage("Fetching videos from YouTube...");
			pd.setIndeterminate(true);
			pd.setCancelable(true);
			return pd;
		default:
			return null;		    	
		}
	}

	private class GetVideosTask extends AsyncTask<Void, Void, ArrayList<VideoFeed>>{
		String err = null;

		@Override
		protected void onPreExecute() {
			showDialog(FETCH_VIDEOS_DIAG);
		}

		@Override
		protected ArrayList<VideoFeed> doInBackground(Void... arg0) {
			ArrayList<VideoFeed> videos;

			try {
				ArtistInfo artistInfo = getIntent().getExtras().getParcelable("artistParcel");
				SongInfo songInfo = getIntent().getExtras().getParcelable("songParcel");
				
				if(songInfo != null){
					videos = YouTubeComm.getComm().searchVideo(songInfo.name, songInfo.artist.name, 10);
				}else{
					videos = YouTubeComm.getComm().searchVideo("", artistInfo.name, 20);
				}
				
				return videos;

			} catch (ServiceCommException e) {
				err = e.getMessage();
				return null;
			}
		
		}

		@Override
		protected void onPostExecute(ArrayList<VideoFeed> result) {
			removeDialog(FETCH_VIDEOS_DIAG);

			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				WatchSongVideoActivity.this.finish();
				return;
			}

			adapter.setAdapter(result);
		}		
	}		
	
	private class ListAdapter extends BaseAdapter {

		private ArrayList<VideoFeed> videos;	 
		private LayoutInflater inflater;

		public ListAdapter() {    
			videos = null;
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public int getCount() {
			if(videos == null)
				return 0;

			return videos.size();
		}

		public VideoFeed getItem(int position) {
			return videos.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if(convertView == null) {
				convertView = inflater.inflate(R.layout.list_row, null);

				holder = new ViewHolder();
				holder.topText = (TextView) convertView.findViewById(R.id.firstLine);
				holder.botText = (TextView) convertView.findViewById(R.id.secondLine);
				holder.coverArt = (ImageView) convertView.findViewById(R.id.coverart);
				holder.playPause = (ImageView) convertView.findViewById(R.id.playpause);	

				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}

			final VideoFeed video = getItem(position);
			if(video != null) {

				holder.playPause.setVisibility(View.GONE);
				holder.topText.setText(video.title);
				holder.botText.setText(video.description);

				try{
					ImageLoader.getLoader(getCacheDir()).DisplayImage(video.image, holder.coverArt, R.drawable.ic_menu_disc);
				}catch(IndexOutOfBoundsException e){
					Log.w(Util.APP, "Unable to fetch the release image from Echo Nest!");
				}
			}

			return convertView;
		}

		public void setAdapter(ArrayList<VideoFeed> vf){
			this.videos = vf;
			notifyDataSetChanged();
		}

		private class ViewHolder{
			public TextView topText;
			public TextView botText;
			public ImageView coverArt;
			public ImageView playPause;
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String videoId = adapter.videos.get(position).id;
		
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:"+videoId));
		startActivity(i);
	}
}
