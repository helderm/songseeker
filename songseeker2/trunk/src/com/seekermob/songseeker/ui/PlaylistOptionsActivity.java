package com.seekermob.songseeker.ui;

import java.util.List;
import java.util.Random;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.data.PlaylistOptions;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class PlaylistOptionsActivity extends SherlockFragmentActivity implements SeekBar.OnSeekBarChangeListener{

	TextView mood_label;
	SeekBar mood;
	TextView energy_label;
	SeekBar energy;
	TextView danceability_label;
	SeekBar danceability;
	TextView hotness_label;
	SeekBar hotness;
	TextView tempo_label;
	SeekBar tempo;
	TextView variety_label;
	SeekBar variety;
	TextView max_results_label;
	SeekBar max_results;

	RadioButton exactArtists;
	RadioButton similarArtists;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.playlist_options);

		//set action bar
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.playlist_options);

		//set seekbar and labels
		mood = (SeekBar)findViewById(R.id.seekBar_mood);
		mood.setOnSeekBarChangeListener(this);
		mood_label = (TextView)findViewById(R.id.label_mood);

		energy = (SeekBar)findViewById(R.id.seekBar_energy);
		energy.setOnSeekBarChangeListener(this);
		energy_label = (TextView)findViewById(R.id.label_energy);

		danceability = (SeekBar)findViewById(R.id.seekBar_danceability);
		danceability.setOnSeekBarChangeListener(this);
		danceability_label = (TextView)findViewById(R.id.label_danceability);

		hotness = (SeekBar)findViewById(R.id.seekBar_hotness);
		hotness.setOnSeekBarChangeListener(this);
		hotness_label = (TextView)findViewById(R.id.label_hotness);

		tempo = (SeekBar)findViewById(R.id.seekBar_tempo);
		tempo.setOnSeekBarChangeListener(this);
		tempo_label = (TextView)findViewById(R.id.label_tempo);

		variety = (SeekBar)findViewById(R.id.seekBar_variety);
		variety.setOnSeekBarChangeListener(this); 
		variety_label = (TextView)findViewById(R.id.label_variety);

		max_results = (SeekBar)findViewById(R.id.seekBar_max_results);
		max_results.setOnSeekBarChangeListener(this); 
		max_results_label = (TextView)findViewById(R.id.label_max_results);

		PlaylistOptions.getInstance(this);

		mood.setProgress(PlaylistOptions.getInstance().getSettings().pl_mood);
		energy.setProgress(PlaylistOptions.getInstance().getSettings().pl_energy);
		danceability.setProgress(PlaylistOptions.getInstance().getSettings().pl_danceability);
		hotness.setProgress(PlaylistOptions.getInstance().getSettings().pl_hotness);
		tempo.setProgress(PlaylistOptions.getInstance().getSettings().pl_tempo);    
		variety.setProgress(PlaylistOptions.getInstance().getSettings().pl_variety);
		max_results.setProgress(PlaylistOptions.getInstance().getSettings().pl_max_results); 
		
		//set radio buttons
        exactArtists = (RadioButton) findViewById(R.id.exact_artists);
        exactArtists.setChecked(!PlaylistOptions.getInstance().getSettings().isSimilar);
        exactArtists.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View arg0) {
				PlaylistOptions.getInstance().getSettings().isSimilar = false;				
			}
		});       
        
        similarArtists = (RadioButton) findViewById(R.id.similar_artists);
        similarArtists.setChecked(PlaylistOptions.getInstance().getSettings().isSimilar);
        similarArtists.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View arg0) {
				PlaylistOptions.getInstance().getSettings().isSimilar = true;				
			}
		});		
	}
	
    @Override
    protected void onDestroy() {
       	super.onDestroy();
       	
       	PlaylistOptions.getInstance(this).saveSettings(this);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.playlist_options_menu, menu);		
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item)	{

		switch (item.getItemId()){
		case android.R.id.home:
			finish();
			return true;
		case R.id.menu_shuffle_options:
        	//shuffle the playlist options
        	Random rand = new Random();
        	
        	mood.setProgress(rand.nextInt(100));
        	energy.setProgress(rand.nextInt(100));
        	danceability.setProgress(rand.nextInt(100));
        	hotness.setProgress(rand.nextInt(100));
        	tempo.setProgress(rand.nextInt(100));
        	variety.setProgress(rand.nextInt(100));  
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    	int seekBarId = seekBar.getId();
    	
    	PlaylistOptions.getInstance(this);
    	
    	if(seekBarId == R.id.seekBar_energy){
    		PlaylistOptions.getInstance().getSettings().pl_energy = progress;
    		
    		if(PlaylistOptions.getInstance().getMinEnergy() == -1.0f){
    			energy_label.setText(getResources().getText(R.string.energy_str) + " (Off)");
    		}else{
    			energy_label.setText(getResources().getText(R.string.energy_str) + " (" + progress + "%)");
    		}   
    		
    		return;
    	}
    	
    	if(seekBarId == R.id.seekBar_mood){
    		PlaylistOptions.getInstance().getSettings().pl_mood = progress;
    		
    		List<String> moods = PlaylistOptions.getInstance().getMood();
    		if(moods == null){
    			mood_label.setText(getResources().getText(R.string.mood_str)+" (Off)");
    			return;
    		}
    		
    		if(moods.size() == 1){    		
	    		mood_label.setText(getResources().getText(R.string.mood_str) + 
	    							" (" + moods.get(0) + ")");
    		}else if(moods.size() == 2){
    			mood_label.setText(getResources().getText(R.string.mood_str) + 
						" (" + moods.get(0) + " - " + moods.get(1) + ")");
    		}
    		moods = null;    		
    		return;
    	}
    	
    	if(seekBarId == R.id.seekBar_tempo){
    		PlaylistOptions.getInstance().getSettings().pl_tempo = progress;
    		
    		if(PlaylistOptions.getInstance().getMinTempo() == -1.0f){
    			tempo_label.setText(getResources().getText(R.string.tempo_str) + " (Off)");
    		}else{
    			tempo_label.setText(getResources().getText(R.string.tempo_str) + 
    					" (" + ((progress/100.0f)*PlaylistOptions.PL_MAX_TEMPO) + " BPM)");
    		}
    		return;
    	}
    	
    	if(seekBarId == R.id.seekBar_danceability){
    		PlaylistOptions.getInstance().getSettings().pl_danceability = progress;
    		
    		if(PlaylistOptions.getInstance().getMinDanceability() == -1.0f){
    			danceability_label.setText(getResources().getText(R.string.danceability_str) + " (Off)");
    		}else{
    			danceability_label.setText(getResources().getText(R.string.danceability_str) + " (" + progress + "%)");    			
    		}
    		return;
    	}
    	
    	if(seekBarId == R.id.seekBar_hotness){
    		PlaylistOptions.getInstance().getSettings().pl_hotness = progress;
    		
    		if(PlaylistOptions.getInstance().getMinHotness() == -1.0f){
    			hotness_label.setText(getResources().getText(R.string.hotness_str) + " (Off)");
    		}else{
    			hotness_label.setText(getResources().getText(R.string.hotness_str) + " (" + progress + "%)");
    		}
    		return;    		
    	}
    	
    	if(seekBarId == R.id.seekBar_variety){
    		PlaylistOptions.getInstance().getSettings().pl_variety = progress;
    		
    		if(PlaylistOptions.getInstance().getVariety() == -1.0f){
    			variety_label.setText(getResources().getText(R.string.variety_str) + " (Off)");
    		}else{
    			variety_label.setText(getResources().getText(R.string.variety_str) + " (" + progress + "%)");
    		}    		
    		return;
    	}
    	
    	if(seekBarId == R.id.seekBar_max_results){
    		if(progress < 1)
    			progress = 1;
    		if(progress > 100)	
    			progress = 100;
    		
    		PlaylistOptions.getInstance().getSettings().pl_max_results = progress;
    		max_results_label.setText(getResources().getText(R.string.max_results_str) + " (" + progress + ")");
    		
    		return;
    	}
		
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
		
	}	

}
