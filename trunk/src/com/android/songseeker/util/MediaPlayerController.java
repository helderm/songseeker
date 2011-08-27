package com.android.songseeker.util;

import android.media.AudioManager;
import android.media.MediaPlayer;

public class MediaPlayerController {
	private static MediaPlayerController mp_con = new MediaPlayerController();
	private static MediaPlayer mp = null;
	private static MediaPlayer.OnCompletionListener listener = null;
	
	public static MediaPlayerController getCon(){
		if(mp == null){
			mp = new MediaPlayer();
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
		}
		
		return mp_con; 
	}
	
	public void setOnCompletionListener(MediaPlayer.OnCompletionListener l){
		listener = l;
	}
	
	public synchronized void release(){
		mp.release();
		mp = null;
	}
	
	public synchronized void stop(){
		mp.stop();
	}
	
	public boolean isPlaying(){
		return mp.isPlaying();
	}
	
	public synchronized void resetAndStart(String source) throws Exception{
		mp.reset();
		mp.setDataSource(source);
		mp.prepare();
		if(listener != null)
			mp.setOnCompletionListener(listener);
		mp.start();
	}
	

}
