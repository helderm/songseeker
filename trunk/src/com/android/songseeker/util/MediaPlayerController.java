package com.android.songseeker.util;

import java.io.IOException;

import com.android.songseeker.R;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

public class MediaPlayerController implements OnCompletionListener {
	private static MediaPlayerController controller = new MediaPlayerController();
	private static MediaPlayer mp = null;
		
	private static MediaPlayerCommander commander = controller.new MediaPlayerCommander();
	private static MediaInfo media = null;
	
	private static final int MP_PLAY = 0;
	private static final int MP_STOP = 1;
	
	public static MediaPlayerController getCon(){
		if(mp == null){
			mp = new MediaPlayer();
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
		}
		
		return controller; 
	}
	
	public synchronized void release(){
		mp.release();
		mp = null;
	}
		
	public void startStopMedia(String source, ImageView icon){
		if(media == null || !media.source.equalsIgnoreCase(source) || media.status == MediaStatus.STOPPED){
			
			if(media != null && media.icon != null){
				media.icon.setImageResource(R.drawable.play);
				media.icon = icon;
			}
			
			start(source, icon);
		}
		else
			stop();
	}
	
	public void start(String source, ImageView icon){
		commander.cancel(true);
		commander = new MediaPlayerCommander();
				
		MediaInfo newMedia = new MediaInfo();
		newMedia.source = source;
		newMedia.icon = icon;
		newMedia.status = MediaStatus.LOADING;		
		
		TaskParams tp = new TaskParams();
		tp.media = newMedia;
		tp.action = MP_PLAY;
		
		commander.execute(tp);
	}
	
	public void stop(){
		commander.cancel(true);
		commander = new MediaPlayerCommander();
		
		TaskParams tp = new TaskParams();
		tp.action = MP_STOP;		
		
		commander.execute(tp);
	}
	
	private class MediaPlayerCommander extends AsyncTask<TaskParams, MediaInfo, TaskParams>{
		private String err = null;
		
		@Override
		protected void onPreExecute() {			
			//set imageview to not playing icon
			if(media != null)
				setIcon(MediaStatus.STOPPED);
		}
		
		@Override
		protected void onProgressUpdate(MediaInfo... media) {
			switch(media[0].status){
			case LOADING:
				media[0].icon.setImageResource(R.drawable.icon);
				break;
			case PLAYING:
				media[0].icon.setImageResource(R.drawable.pause);
				break;
			case STOPPED:
			default:				
				media[0].icon.setImageResource(R.drawable.play);
				break;
			}			
		}
		
		@Override
		protected TaskParams doInBackground(TaskParams... tp) {
			TaskParams params = tp[0];
			
			try{
				switch(params.action){
				case MP_PLAY:
					
					if(isCancelled())
						return null;
						
					publishProgress(params.media);
					
					preparePlayer(params.media);
					
					if(isCancelled()){
						publishProgress(media);
						return null;
					}

					startPlayer();
					publishProgress(media);

					break;
				case MP_STOP:
					
					if(isCancelled())
						return null;
					
					stopPlayer();
					
					publishProgress(media);
					break;
				}

			}catch(IOException e){
				Log.e(Util.APP, e.getMessage(), e);
				err = "Unable to " + (params.action==MP_PLAY? "start":"stop") + " the Media Player. Please try again.";
			}catch(IllegalStateException e){
				Log.e(Util.APP, e.getMessage(), e);
				err = "Unable to " + (params.action==MP_PLAY? "start":"stop") + " the Media Player. Please try again.";
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(TaskParams tp) {
			
			if(err != null)
				return;
			
		}
	}
	
	private synchronized void preparePlayer(MediaInfo m) throws IllegalStateException, IOException{
		
		if(Thread.interrupted())
			return;
		
		//if they are not the same media or if it is stopped, we will need to prepare() again
		if( media == null || !media.source.equalsIgnoreCase(m.source) ||
				media.status == MediaStatus.STOPPED){						
			
			mp.reset();
			mp.setDataSource(m.source);
			mp.prepare();
			mp.setOnCompletionListener(this);
		}	
		
		m.status = MediaStatus.PREPARED;
		
		media = m;
	}
	
	private synchronized void startPlayer() throws IllegalStateException{
		mp.start();
		media.status = MediaStatus.PLAYING;
	}
	
	private synchronized void stopPlayer() throws IllegalStateException{
		if(mp.isPlaying()){
			mp.stop();
			media.status = MediaStatus.STOPPED;
		}
	}
	
	private void setIcon(MediaStatus status){
		switch(status) {
		case PLAYING:
			media.icon.setImageResource(R.drawable.pause);
			break;
		case STOPPED:
		default:				
			media.icon.setImageResource(R.drawable.play);
			break;
		}
	}		

	@Override
	public void onCompletion(MediaPlayer mp) {
		stopPlayer();
		media.icon.setImageResource(R.drawable.play);
	}
	
	private class TaskParams{
		public int action;
		public MediaInfo media;
	}
	
	private class MediaInfo{
		public String source;
		public ImageView icon;
		public MediaStatus status;
	}
	
	private enum MediaStatus{
		LOADING, PREPARED, PLAYING, STOPPED;
	}


}
