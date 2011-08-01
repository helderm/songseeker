package com.android.songseeker;

import android.app.Activity;
import android.os.Bundle;
import android.widget.SeekBar;

public class PlaylistOptionsActivity extends Activity implements SeekBar.OnSeekBarChangeListener{
	SeekBar mood;
	SeekBar energy;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist_options);
        
        mood = (SeekBar)findViewById(R.id.seekBar_mood);
        mood.setOnSeekBarChangeListener(this);
        
        energy = (SeekBar)findViewById(R.id.seekBar_energy);
        energy.setOnSeekBarChangeListener(this);

    }    

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        
    	switch(seekBar.getId()){
    	case R.id.seekBar_energy:
    		Settings.energy = progress;
    		break;
    	case R.id.seekBar_mood:
    		Settings.mood = progress;
    		break;
    	
    	}
    	
    }

	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
    
    
    
    
}
