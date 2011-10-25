package com.android.songseeker.activity;

import java.util.ArrayList;

import com.android.songseeker.R;
import com.android.songseeker.comm.ServiceCommException;
import com.android.songseeker.comm.SevenDigitalComm;
import com.android.songseeker.data.RecSongsPlaylist;
import com.android.songseeker.data.ReleaseInfo;
import com.android.songseeker.data.SongInfo;
import com.android.songseeker.util.ImageLoader;
import com.android.songseeker.util.MediaPlayerController;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ReleaseInfoActivity extends ListActivity {

	private ReleaseInfo release;
	
	private static final int RELEASE_DETAILS_DIAG = 0;
	private SongListAdapter adapter = new SongListAdapter();;

	private StartMediaPlayerTask mp_task = new StartMediaPlayerTask();
	private GetReleaseDetails rl_task = new GetReleaseDetails();
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		rl_task.execute();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case RELEASE_DETAILS_DIAG:
			ProgressDialog pd = new ProgressDialog(this);
			pd.setMessage("Fetching album details...");
			pd.setIndeterminate(true);
			pd.setCancelable(true);
			return pd;
		default:
			return null;		    	
		}
	}
	
	@Override
	protected void onPause() {
		MediaPlayerController.getCon().release();
		super.onPause();
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		rl_task.cancel(true);
	}
	
	private class GetReleaseDetails extends AsyncTask<Void, Void, Void>{
		String err = null;		

		@Override
		protected void onPreExecute() {
			showDialog(RELEASE_DETAILS_DIAG);
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			ArrayList<SongInfo> songList;

			try{
				release = getIntent().getExtras().getParcelable("releaseParcel");
				if(release == null){
					SongInfo song = getIntent().getExtras().getParcelable("songId");	
					song = SevenDigitalComm.getComm().querySongDetails(song.id, song.name, song.artist.name);	
					release = song.release;
				}				
				
				songList = SevenDigitalComm.getComm().queryReleaseSongList(release.id);
			}catch(ServiceCommException e){
				err = e.getMessage();		
				return null;
			}			
			
			adapter.setSongList(songList);

			return null;
		}

		@Override
		protected void onPostExecute(Void s) {

			removeDialog(RELEASE_DETAILS_DIAG);

			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				ReleaseInfoActivity.this.finish();
				return;
			}

			//set content for main screen
			setContentView(R.layout.listview);	
			
			//set album info header
			LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			LinearLayout header = (LinearLayout)inflater.inflate(R.layout.release_info, null);
			
			TextView releaseName = (TextView) header.findViewById(R.id.releaseinfo_releaseName);
			releaseName.setText(release.name);

			TextView releaseArtist = (TextView) header.findViewById(R.id.releaseinfo_artistName);
			releaseArtist.setText(release.artist.name);
			releaseArtist.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent(ReleaseInfoActivity.this, ArtistInfoActivity.class);
					i.putExtra("artistParcel", release.artist);
					startActivity(i);
				}
			});

			//set buy button
			Button buy = (Button)header.findViewById(R.id.releaseinfo_buy);
			buy.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(release.buyUrl));
					intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
			});

			//set add button
			Button add = (Button)header.findViewById(R.id.releaseinfo_add);
			add.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					RecSongsPlaylist.getInstance().addSongsToPlaylist(adapter.songList, ReleaseInfoActivity.this);
				}
			});
			
			//set share button
			Button share = (Button)header.findViewById(R.id.releaseinfo_share);
			share.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					final Intent intent = new Intent(Intent.ACTION_SEND);					 
					intent.setType("text/plain");
					intent.putExtra(Intent.EXTRA_SUBJECT, "New release!");
					intent.putExtra(Intent.EXTRA_TEXT, "I discovered the album '"+ release.name + " by "+ release.artist.name 
							+ "' using the Song Seeker app for Android! Check it out! "+ release.buyUrl);
					startActivity(Intent.createChooser(intent, "Share using..."));
				}				
			});
			
			//set image
			ImageView coverart = (ImageView) header.findViewById(R.id.releaseinfo_coverArt);
			ImageLoader.getLoader(getCacheDir()).DisplayImage(release.image, coverart, R.drawable.ic_menu_disc);
			
			getListView().addHeaderView(header);

			//set adapter for top tracks
			ReleaseInfoActivity.this.setListAdapter(adapter); 	
		}		
	}	
	
	private class SongListAdapter extends BaseAdapter {

		private ArrayList<SongInfo> songList;    

		public SongListAdapter() {    
			songList = null;
		}

		public int getCount() {
			if(songList == null)
				return 0;

			return songList.size();
		}

		public SongInfo getItem(int position) {
			return songList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.list_row, null);
			}

			final SongInfo song = getItem(position);
			final int pos = position;
			if (song != null) {
				TextView tt = (TextView) v.findViewById(R.id.firstLine);
				TextView bt = (TextView) v.findViewById(R.id.secondLine);
				ImageView coverart = (ImageView) v.findViewById(R.id.coverart);
				ImageView playpause = (ImageView) v.findViewById(R.id.playpause);

				bt.setText(song.artist.name);
				tt.setText(song.name);

				switch(MediaPlayerController.getCon().getStatus(pos)){				
				case PLAYING:
					playpause.setImageResource(R.drawable.pause);
					break;
				case LOADING:
				case PREPARED:
					playpause.setImageResource(R.drawable.icon);
					break;				
				case STOPPED:
				default:
					playpause.setImageResource(R.drawable.play);
					break;					
				}
				playpause.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						mp_task.cancel(true);
						mp_task = new StartMediaPlayerTask();
						mp_task.position = pos;
						mp_task.execute(song);
					}
				}); 			    

				ImageLoader.getLoader(getCacheDir()).DisplayImage(song.release.image, coverart, R.drawable.ic_menu_disc);
			}

			return v;
		}

		public void setSongList(ArrayList<SongInfo> tp){
			this.songList = tp;
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		SongInfo si = adapter.getItem(position-1);
		Intent i = new Intent(ReleaseInfoActivity.this, MusicInfoTab.class);
		i.putExtra("songParcel", si);
		startActivity(i);
	}
	
	private class StartMediaPlayerTask extends AsyncTask<SongInfo, Void, SongInfo>{
		private String err = null;
		public int position = -1;

		@Override
		protected SongInfo doInBackground(SongInfo... song) {


			if(isCancelled())
				return null;

			if(song[0].previewUrl == null){
				
				try{
					song[0].previewUrl = SevenDigitalComm.getComm().getPreviewUrl(song[0].id);
				} catch(Exception e){
					err = getString(R.string.err_mediaplayer);
					Log.e(Util.APP, "7digital getPreviewUrl() exception!", e);
					return null;
				} 
			}

			return song[0];
		}

		@Override
		protected void onPostExecute(SongInfo song) {
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();
				err = null;
				return;
			}	
			
			if(!isCancelled())
				MediaPlayerController.getCon().startStopMedia(song.previewUrl, position, adapter);
		}
	}	
}
