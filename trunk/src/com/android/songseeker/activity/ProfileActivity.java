package com.android.songseeker.activity;


import java.util.ArrayList;

import com.android.songseeker.R;
import com.android.songseeker.data.ArtistInfo;
import com.android.songseeker.util.ImageLoader;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ProfileActivity extends Activity {
	
	private ListAdapter adapter;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	}

	private class ListAdapter extends BaseAdapter {

		private ArrayList<ArtistInfo> artists;	 
		private LayoutInflater inflater;

		public ListAdapter() {    
			artists = null;
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public int getCount() {
			if(artists == null)
				return 0;

			return artists.size();
		}

		public ArtistInfo getItem(int position) {
			return artists.get(position);
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

			final ArtistInfo artist = getItem(position);
			if(artist != null) {

				holder.playPause.setVisibility(View.GONE);
				holder.topText.setText(artist.name);

				ImageLoader.getLoader(getCacheDir()).DisplayImage(artist.image, holder.coverArt, R.drawable.ic_menu_disc);	
			}

			return convertView;
		}

		public void setAdapter(ArrayList<ArtistInfo> vf){
			this.artists = vf;
			notifyDataSetChanged();
		}

		private class ViewHolder{
			public TextView topText;
			public TextView botText;
			public ImageView coverArt;
			public ImageView playPause;
		}
	}
}
