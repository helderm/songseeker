package com.seekermob.songseeker.ui;

import java.util.ArrayList;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
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

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.ReleaseInfo;
import com.seekermob.songseeker.data.UserProfile;
import com.seekermob.songseeker.data.UserProfile.ArtistProfile;
import com.seekermob.songseeker.util.ImageLoader;
import com.seekermob.songseeker.util.ImageLoader.ImageSize;
import com.seekermob.songseeker.util.Util;

public class ArtistInfoFragment extends SherlockListFragment{
	
	private ArtistReleasesAdapter mAdapter;
	private ArtistInfo mArtist;
	private ArtistDetailsTask mArtistDetailsTask;
	private ProfileTask mProfileTask;
	
	private Bundle mSavedState;
	
	private static final String STATE_ADAPTER_DATA = "adapterData";
	private static final String STATE_ARTIST_DETAILS_RUNNING = "artistDetailsRunning";
	private static final String STATE_ARTIST_IMAGE = "artistImage";	
	private static final String STATE_PROFILE_RUNNING = "profileRunning";
	public static final String BUNDLE_ARTIST = "artist"; 
	public static final String BUNDLE_ARTIST_RELEASES = "artistReleases"; 
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		//populate the optionsMenu 
		setHasOptionsMenu(true);
			
		//fetch the selected song
		mArtist = getArguments().getParcelable(BUNDLE_ARTIST);
		ArrayList<ReleaseInfo> artistReleases = getArguments().getParcelableArrayList(BUNDLE_ARTIST_RELEASES);

		//set adapter		
		mAdapter = new ArtistReleasesAdapter(artistReleases);
		
		//restore state
		restoreLocalState(savedInstanceState);
		
		//set header & adapter
		setListHeader();
		setListAdapter(mAdapter);		
					
		//set background image
		if(mArtist.image != null){
			ImageView bkg = (ImageView) getView().findViewById(R.id.background);
			ImageLoader.getLoader().DisplayImage(mArtist.image, getListView(), bkg, ImageSize.LARGE);
		}
		
		//if the adapter wasnt restored, fetch the adapter
		//but only if the task wasnt restored on restoreLocalState
		if((mAdapter.mReleases == null || mArtist.image == null) && !isTaskRunning()){			
			mArtistDetailsTask = (ArtistDetailsTask) new ArtistDetailsTask(mAdapter.mReleases).execute();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.listview_progress, null);		
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		
		//save the adapter data
		if(mAdapter != null && mAdapter.mReleases != null){
			outState.putParcelableArrayList(STATE_ADAPTER_DATA, new ArrayList<Parcelable>(mAdapter.mReleases));			
		}
		
		//save the task state
		final ArtistDetailsTask artistTask = mArtistDetailsTask;
        if(artistTask != null && artistTask.getStatus() != AsyncTask.Status.FINISHED) {
        	artistTask.cancel(true);
        	
        	outState.putBoolean(STATE_ARTIST_DETAILS_RUNNING, true);
        	mArtistDetailsTask = null;
        }
        
        //save the profile task state
		final ProfileTask profileTask = mProfileTask;
        if(profileTask != null && profileTask.getStatus() != AsyncTask.Status.FINISHED) {
        	profileTask.cancel(true);
        	
        	outState.putBoolean(STATE_PROFILE_RUNNING, true);
        	mProfileTask = null;
        }        
        
        //save the image
        if(mArtist.image != null){
        	outState.putString(STATE_ARTIST_IMAGE, mArtist.image);
        }
		
		mSavedState = outState;
		
		super.onSaveInstanceState(outState);
	}
	
	/** Restores the saved instance of this fragment*/
	private void restoreLocalState(Bundle savedInstanceState){	
		if(savedInstanceState == null){
			return;
		}
		
		//restore the adapter
		//but the one restored from the bundle has priority over this
		ArrayList<ReleaseInfo> adapterData = null;
		if(mAdapter.mReleases == null && (adapterData = savedInstanceState.getParcelableArrayList(STATE_ADAPTER_DATA)) != null){
			mAdapter.setArtistReleases(adapterData);			
		}
		
		//restore the artist image
		String image;
		if((image = savedInstanceState.getString(STATE_ARTIST_IMAGE)) != null){
			mArtist.image = image;
		}
		
		//restore the top tracks task
		if(savedInstanceState.getBoolean(STATE_ARTIST_DETAILS_RUNNING)){
			mArtistDetailsTask = (ArtistDetailsTask) new ArtistDetailsTask(mAdapter.mReleases).execute();
		}
		
		//restore the profile task
		if(savedInstanceState.getBoolean(STATE_PROFILE_RUNNING) && !isTaskRunning()){
			mProfileTask = (ProfileTask) new ProfileTask(mArtist).execute();
		}		
		
		mSavedState = null;
	}		
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(mArtistDetailsTask != null)
			mArtistDetailsTask.cancel(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
        if(mSavedState != null) {
            restoreLocalState(mSavedState);
        }
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.artistinfo_menu, menu);		
		super.onCreateOptionsMenu(menu, inflater);
	}	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final Intent intent;
		
		switch(item.getItemId()) {
		case R.id.menu_buy:
			intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mArtist.buyUrl));
			intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return true;

		case R.id.menu_share:
			intent = new Intent(Intent.ACTION_SEND);					 
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_artist_text) + " [" + mArtist.name +
					"] ("+ mArtist.buyUrl +")");
			startActivity(Intent.createChooser(intent, getString(R.string.share_using)));
			return true;
		case R.id.menu_add_to_profile:
			if(isTaskRunning()){
				Toast.makeText(getActivity(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
				return true;
			}
			mProfileTask = (ProfileTask) new ProfileTask(mArtist).execute();
			return true;
		case R.id.menu_watch:
			intent = new Intent(getActivity(), YouTubeVideosActivity.class);
			intent.putExtra(BUNDLE_ARTIST, mArtist);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}	
	
	private void setListHeader(){				
		//set transparent background to show album image
		//getListView().setBackgroundColor(0);
		
		//set album info header
		LayoutInflater inflater = getActivity().getLayoutInflater();
		LinearLayout header = (LinearLayout)inflater.inflate(R.layout.artist_info, null);
		
		TextView artistName = (TextView) header.findViewById(R.id.artistinfo_artistName);
		artistName.setText(mArtist.name);
		
		TextView tvBio = (TextView)header.findViewById(R.id.artistinfo_biography);
		tvBio.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {			
				Intent i = new Intent(getActivity(), ArtistBioActivity.class);
				i.putExtra(BUNDLE_ARTIST, mArtist);
				startActivity(i);
			}
		});

		TextView tvNews = (TextView)header.findViewById(R.id.artistinfo_news);
		tvNews.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {			
				Intent i = new Intent(getActivity(), ArtistNewsActivity.class);
				i.putExtra(BUNDLE_ARTIST, mArtist);
				startActivity(i);
			}
		});	
		
		TextView tvSimilar = (TextView)header.findViewById(R.id.artistinfo_similar);
		tvSimilar.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(getActivity(), SimilarArtistsActivity.class);
				i.putExtra(BUNDLE_ARTIST, mArtist);
				startActivity(i);
			}
		});		
		
		//set image
		if(mArtist.image != null){
			ImageView coverart = (ImageView) header.findViewById(R.id.artistinfo_image);
			ImageLoader.getLoader().DisplayImage(mArtist.image, coverart, R.drawable.ic_disc_stub, ImageSize.MEDIUM);
		}	
		
		getListView().addHeaderView(header, null, false);
	}
	
	private class ArtistReleasesAdapter extends BaseAdapter {

		private ArrayList<ReleaseInfo> mReleases;    
		private LayoutInflater inflater;
		
		public ArtistReleasesAdapter(ArrayList<ReleaseInfo> r) {    
			mReleases = r;
			inflater = getActivity().getLayoutInflater();
		}

		public int getCount() {
			if(mReleases == null)
				return 0;

			return mReleases.size();
		}

		public ReleaseInfo getItem(int position) {
			return mReleases.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
	    	
			if (convertView == null) {			    
				convertView = inflater.inflate(R.layout.list_item_2_image_media, null);
				
				holder = new ViewHolder();
				holder.topText = (TextView) convertView.findViewById(R.id.firstLine);
			    holder.botText = (TextView) convertView.findViewById(R.id.secondLine);
			    holder.image = (ImageView) convertView.findViewById(R.id.image);
			    			    			    
			    convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}

			ReleaseInfo release = getItem(position);
			if (release == null) {
				return convertView;
			}
			
			holder.botText.setText(release.artist.name);
			holder.topText.setText(release.name);
										
			ImageLoader.getLoader().DisplayImage(release.image, holder.image, R.drawable.ic_disc_stub, ImageSize.SMALL);			

			return convertView;
		}

		public void setArtistReleases(ArrayList<ReleaseInfo> tp){
			this.mReleases = tp;
			notifyDataSetChanged();
		}
		
		@Override
		public boolean isEmpty() {		
			//overriding this so it always shows th header view, even when the adapter is empty
			return false;
		}		
		
	    public class ViewHolder{
	    	public TextView topText;
	    	public TextView botText;
	    	public ImageView image;
	    }
	}	
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		ReleaseInfo ri = mAdapter.getItem(position-1);
		Intent i = new Intent(getActivity(), MusicInfoActivity.class);
		i.putExtra(ReleaseInfoFragment.BUNDLE_RELEASE, ri);
		i.putParcelableArrayListExtra(BUNDLE_ARTIST_RELEASES, mAdapter.mReleases);
		startActivity(i);
	}
	
	private class ArtistDetailsTask extends AsyncTask<Void, Void, ArrayList<ReleaseInfo>>{
		ArrayList<ReleaseInfo> mReleases;
		String err = null;		
		
		public ArtistDetailsTask(ArrayList<ReleaseInfo> releases){
			mReleases = releases;
		}		
		
		@Override
		protected void onPreExecute() {
			Util.setListShown(ArtistInfoFragment.this, false);
		}

		@Override
		protected ArrayList<ReleaseInfo> doInBackground(Void... args) {
			ArrayList<ReleaseInfo> releases;

			try{
				if(isCancelled())
					return null;
				
				//will need to fetch artist details, since we dont have the artist image url 
				if(mArtist.image == null){				
					mArtist = SevenDigitalComm.getComm().queryArtistDetails(mArtist.id, getActivity());
				}	
				
				if(isCancelled())
					return null;
				
				if(mReleases == null){
					releases = SevenDigitalComm.getComm().getArtistReleases(mArtist.id);
				} else{
					releases = mReleases;
				}		
				
			}catch(ServiceCommException e){
				err = e.getMessage(getActivity());		
				return null;
			}			
			
			return releases;
		}

		@Override
		protected void onPostExecute(ArrayList<ReleaseInfo> releases) {
			Util.setListShown(ArtistInfoFragment.this, true);
			
			if(err != null){
				Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();
				return;
			}
						
			mAdapter.setArtistReleases(releases);
			
			if(mArtist.image != null){
				//set list header image
				ImageView coverart = (ImageView) getListView().findViewById(R.id.artistinfo_image);
				ImageLoader.getLoader().DisplayImage(mArtist.image, coverart, R.drawable.ic_disc_stub, ImageSize.MEDIUM);
				
				//set background image
				if(mArtist.image != null){
					ImageView bkg = (ImageView) getView().findViewById(R.id.background);
					ImageLoader.getLoader().DisplayImage(mArtist.image, getListView(), bkg, ImageSize.LARGE);
				}
			}
		}		
	}	
	
	private boolean isTaskRunning(){				
		if(mArtistDetailsTask != null && mArtistDetailsTask.getStatus() != AsyncTask.Status.FINISHED){
			return true;
		}
		
		return false;
	}
	
	private class ProfileTask extends AsyncTask<Void, Void, Void>{
		private ArtistInfo mArtist;
		private UserProfile mUserProfile;
		
		private String err;
		private String msg;
		
		protected ProfileTask(ArtistInfo a) {
			mArtist = a;
			mUserProfile = UserProfile.getInstance(getActivity());
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			ArrayList<ArtistProfile> artistsProfile = new ArrayList<ArtistProfile>();
			ArtistProfile artistProfile;
			
			//check if the artist is already present at the list
			if(mUserProfile.isAlreadyInProfile(mArtist.id, null)){
				msg = getString(R.string.artists_already_profile);
				return null;
			}
			
			//all artist data is fetched, no need to call the webservice
			if(mArtist.name != null && mArtist.image != null && mArtist.buyUrl != null){
				artistProfile = new ArtistProfile(mArtist);
				artistsProfile.add(artistProfile);
				mUserProfile.syncAddArtistsToProfile(artistsProfile, getActivity());
				msg = getString(R.string.success_add_artists_profile);
				return null;
			}
			
			//data missing, call the webservice before adding
			try {
				mArtist = SevenDigitalComm.getComm().queryArtistDetails(mArtist.id, getActivity());
			} catch (ServiceCommException e) {
				Log.w(Util.APP, "Failed to add artist to profile!", e);
				err = getString(R.string.err_add_artists_profile);
				return null;
			}
			
			artistProfile = new ArtistProfile(mArtist);
			artistsProfile.add(artistProfile);
			mUserProfile.syncAddArtistsToProfile(artistsProfile, getActivity());
			msg = getString(R.string.success_add_artists_profile);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(err != null){
				Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();
				return;
			}			
			
			Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();	
		}
	}
	
}
