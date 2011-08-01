package com.android.songseeker;

import java.util.Random;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

public class PlaylistOptionsActivity extends Activity implements SeekBar.OnSeekBarChangeListener{
	SeekBar mood;
	SeekBar energy;
	SeekBar danceability;
	SeekBar hotness;
	SeekBar tempo;
	
	Button shuffle;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist_options);
        
        mood = (SeekBar)findViewById(R.id.seekBar_mood);
        mood.setOnSeekBarChangeListener(this);
        
        energy = (SeekBar)findViewById(R.id.seekBar_energy);
        energy.setOnSeekBarChangeListener(this);
        
        danceability = (SeekBar)findViewById(R.id.seekBar_danceability);
        danceability.setOnSeekBarChangeListener(this);
        
        hotness = (SeekBar)findViewById(R.id.seekBar_hotness);
        hotness.setOnSeekBarChangeListener(this);

        tempo = (SeekBar)findViewById(R.id.seekBar_tempo);
        tempo.setOnSeekBarChangeListener(this);       
        
        mood.setProgress(Settings.pl_mood);
        energy.setProgress(Settings.pl_energy);
        danceability.setProgress(Settings.pl_danceability);
        hotness.setProgress(Settings.pl_hotness);
        tempo.setProgress(Settings.pl_tempo);        
        
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
            	
            }
        });        
    }    

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        
    	switch(seekBar.getId()){
    	case R.id.seekBar_energy:
    		Settings.pl_energy = progress;
    		break;
    	case R.id.seekBar_mood:
    		Settings.pl_mood = progress;
    		break;
    	case R.id.seekBar_tempo:
    		Settings.pl_tempo = progress;
    		break;
    	case R.id.seekBar_danceability:
    		Settings.pl_danceability = progress;
    		break;
    	case R.id.seekBar_hotness:
    		Settings.pl_hotness = progress;
    		break;    	
    	}   	
    }

    //TODO: maybe Settings can be updated only onStopTracking, check!
	public void onStartTrackingTouch(SeekBar seekBar) {
		
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
	}
    
    
    
    
}
