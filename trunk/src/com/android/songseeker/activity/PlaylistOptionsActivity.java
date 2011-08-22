package com.android.songseeker.activity;

import java.util.List;
import java.util.Random;

import com.android.songseeker.R;
import com.android.songseeker.util.Settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class PlaylistOptionsActivity extends Activity implements SeekBar.OnSeekBarChangeListener{
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
	
	Button shuffle;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist_options);
        
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
        
        mood.setProgress(Settings.pl_mood);
        energy.setProgress(Settings.pl_energy);
        danceability.setProgress(Settings.pl_danceability);
        hotness.setProgress(Settings.pl_hotness);
        tempo.setProgress(Settings.pl_tempo);    
        variety.setProgress(Settings.pl_variety);
        max_results.setProgress(Settings.pl_max_results); 
        
        shuffle = (Button)findViewById(R.id.shuffle_pl_options);
        shuffle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                
            	//shuffle the playlist options
            	Random rand = new Random();
            	
            	mood.setProgress(rand.nextInt(100));
            	energy.setProgress(rand.nextInt(100));
            	danceability.setProgress(rand.nextInt(100));
            	hotness.setProgress(rand.nextInt(100));
            	tempo.setProgress(rand.nextInt(100));
            	variety.setProgress(rand.nextInt(100));            	
            }
        });        
    }    

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        
    	switch(seekBar.getId()){
    	case R.id.seekBar_energy:
    		Settings.pl_energy = progress;
    		
    		if(Settings.getMinEnergy() == -1.0f){
    			energy_label.setText(getResources().getText(R.string.energy_str) + " (Off)");
    		}else{
    			energy_label.setText(getResources().getText(R.string.energy_str) + " (" + progress + "%)");
    		}    		
    		break;
    	case R.id.seekBar_mood:
    		Settings.pl_mood = progress;
    		
    		List<String> moods = Settings.getMood();
    		if(moods == null){
    			mood_label.setText(getResources().getText(R.string.mood_str)+" (Off)");
    			break;
    		}
    		
    		if(moods.size() == 1){    		
	    		mood_label.setText(getResources().getText(R.string.mood_str) + 
	    							" (" + moods.get(0) + ")");
    		}else if(moods.size() == 2){
    			mood_label.setText(getResources().getText(R.string.mood_str) + 
						" (" + moods.get(0) + " - " + moods.get(1) + ")");
    		}
    		moods = null;
    		break;
    	case R.id.seekBar_tempo:
    		Settings.pl_tempo = progress;
    		
    		if(Settings.getMinTempo() == -1.0f){
    			tempo_label.setText(getResources().getText(R.string.tempo_str) + " (Off)");
    		}else{
    			tempo_label.setText(getResources().getText(R.string.tempo_str) + 
    					" (" + ((progress/100.0f)*Settings.PL_MAX_TEMPO) + " BPM)");
    		}
    		break;
    	case R.id.seekBar_danceability:
    		Settings.pl_danceability = progress;
    		
    		if(Settings.getMinDanceability() == -1.0f){
    			danceability_label.setText(getResources().getText(R.string.danceability_str) + " (Off)");
    		}else{
    			danceability_label.setText(getResources().getText(R.string.danceability_str) + " (" + progress + "%)");    			
    		}
    		break;
    	case R.id.seekBar_hotness:
    		Settings.pl_hotness = progress;
    		
    		if(Settings.getMinHotness() == -1.0f){
    			hotness_label.setText(getResources().getText(R.string.hotness_str) + " (Off)");
    		}else{
    			hotness_label.setText(getResources().getText(R.string.hotness_str) + " (" + progress + "%)");
    		}
    		break;   
    	case R.id.seekBar_variety:
    		Settings.pl_variety = progress;
    		
    		if(Settings.getVariety() == -1.0f){
    			variety_label.setText(getResources().getText(R.string.variety_str) + " (Off)");
    		}else{
    			variety_label.setText(getResources().getText(R.string.variety_str) + " (" + progress + "%)");
    		}    		
    		break;
    	case R.id.seekBar_max_results:
    		if(progress < 1)
    			progress = 1;
    		if(progress > 100)	
    			progress = 100;
    		
    		Settings.pl_max_results = progress;
    		max_results_label.setText(getResources().getText(R.string.max_results_str) + " (" + progress + ")");
    	}   	
    }

    //TODO: maybe Settings can be updated only onStopTracking, check!
	public void onStartTrackingTouch(SeekBar seekBar) {
		
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
	}
    
    
    
    
}
