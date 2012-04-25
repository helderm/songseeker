package com.seekermob.songseeker.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.echonest.api.v4.Biography;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.EchoNestComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.data.ArtistInfo;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

public class ArtistBioActivity extends SherlockFragmentActivity {
	
	private String mBiography;
	private String mBioUrl;
	private BioTask mTask;
	
	private Bundle mSavedState;
	
	private static final String STATE_BIO_DATA = "bioData";
	private static final String STATE_BIO_URL_DATA = "bioUrlData";	
	private static final String STATE_TASK_RUNNING = "taskRunning";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.bio);

		//set action bar
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.artist_bio_title);	
		
		//restore state
		restoreLocalState(savedInstanceState);
		
		if(mBiography == null && !isTaskRunning()){
			mTask = (BioTask) new BioTask().execute();
		}else{
			TextView tvText = (TextView)findViewById(R.id.text);
			tvText.setText(mBiography + (mBioUrl!=null? (" (" + mBioUrl + ")") : ""));
		}		
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		
		//save bio data
		if(mBiography != null){
			outState.putString(STATE_BIO_DATA, mBiography);			
		}
		if(mBioUrl != null){
			outState.putString(STATE_BIO_URL_DATA, mBioUrl);			
		}
		
		//save the task state
		final BioTask task = mTask;
        if(task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
        	task.cancel(true);
        	
        	outState.putBoolean(STATE_TASK_RUNNING, true);
        	mTask = null;
        }
		
		mSavedState = outState;
		
		super.onSaveInstanceState(outState);
	}
	
	/** Restores the saved instance of this fragment*/
	private void restoreLocalState(Bundle savedInstanceState){	
		if(savedInstanceState == null){
			return;
		}
		
		//restore the bio data		
		mBiography = savedInstanceState.getString(STATE_BIO_DATA);
		mBioUrl = savedInstanceState.getString(STATE_BIO_URL_DATA);
		
		//restore the top tracks task
		if(savedInstanceState.getBoolean(STATE_TASK_RUNNING)){
			mTask = (BioTask) new BioTask().execute();
		}
		
		mSavedState = null;
	}	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(isTaskRunning())
			mTask.cancel(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
        if(mSavedState != null) {
            restoreLocalState(mSavedState);
        }
	}		
	
	public boolean onOptionsItemSelected(MenuItem item)	{

		switch (item.getItemId()){
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private class BioTask extends AsyncTask<Void, Void, Biography>{
		private String err = null;		
		
		@Override
		protected void onPreExecute() {			
			setTextShown(false);
		}
		
		@Override
		protected Biography doInBackground(Void... arg0) {
			Biography bio;
			ArtistInfo artist = getIntent().getExtras().getParcelable(ArtistInfoFragment.BUNDLE_ARTIST);
			
			if(isCancelled())
				return null;
			
			try {	
				bio = EchoNestComm.getComm().getArtistBioFromBucket(artist.id);
			} catch (ServiceCommException e) {
				err = e.getMessage();
				return null;
			}

			return bio;
		}

		@Override
		protected void onPostExecute(Biography bio) {

			setTextShown(true);
			
			TextView tvText = (TextView)findViewById(R.id.text);
			
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				tvText.setText(R.string.artist_bio_frag_empty);
				return;
			}
			
			if(bio.getText() == null){				
				tvText.setText(R.string.artist_bio_frag_empty);
				return;
			}			
					
			mBiography = bio.getText();
			mBioUrl = bio.getURL();
			
			tvText.setText(mBiography + (mBioUrl!=null? (" (" + mBioUrl + ")") : ""));
		}
	}
	
	private boolean isTaskRunning(){				
		if(mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED){
			return true;
		}
		
		return false;
	}
	
    private void setTextShown(boolean isShown){
    	View v;
    	
    	if(isShown){			
			v = findViewById(R.id.progress_container);
			v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
			v.setVisibility(View.GONE);
			
			v = findViewById(R.id.data_container);
			v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
			v.setVisibility(View.VISIBLE);	
			
    	}else{			
			v = findViewById(R.id.data_container);
			v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
			v.setVisibility(View.GONE);			
			
			v = findViewById(R.id.progress_container);
			v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
			v.setVisibility(View.VISIBLE);			
    	}
    	
    	v = null;    	
    }
}
