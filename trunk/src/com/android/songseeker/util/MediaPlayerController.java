package com.android.songseeker.util;

import java.io.IOException;

import com.android.songseeker.R;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.BaseAdapter;
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
		commander.cancel(true);

		//if(media != null && media.icon != null){
		//	media.icon.setImageResource(R.drawable.play);			
		//}
		BaseAdapter adapter;
		if(media != null){
			adapter = media.adapter;
			media = null;
			adapter.notifyDataSetChanged();			
		}		

		mp.release();
		mp = null;
	}

	public void startStopMedia(String source, ImageView icon){
		if(media == null || !media.source.equalsIgnoreCase(source) || media.status == MediaStatus.STOPPED){

			//if(media != null && media.icon != null){
			//media.icon.setImageResource(R.drawable.play);
			//media.icon = icon;
			//}

			//start(source, icon);
		}
		else
			stop();
	}

	public void startStopMedia(String source, int position, BaseAdapter adapter){
		if(media == null || media.position != position || media.status == MediaStatus.STOPPED){

			if(media != null){
				media.position = position;
				media.status = MediaStatus.LOADING;
				media.adapter.notifyDataSetChanged();
			}

			start(source, position, adapter);
		}
		else
			stop();
	}

	public MediaStatus getStatus(int position){

		if(media == null || media.position != position)
			return MediaStatus.STOPPED;

		return media.status;
	}


	public void start(String source, int position, BaseAdapter adapter){
		commander.cancel(true);
		commander = new MediaPlayerCommander();

		MediaInfo newMedia = new MediaInfo();
		newMedia.source = source;
		newMedia.position = position;
		newMedia.adapter = adapter;
		newMedia.status = MediaStatus.LOADING;		
		setNewMedia(newMedia);

		TaskParams tp = new TaskParams();
		tp.media = newMedia;
		tp.action = MP_PLAY;

		commander.execute(tp);
	}

	private void setNewMedia(MediaInfo m){
		media = m;
		media.adapter.notifyDataSetChanged();
	}

	public void stop(){
		commander.cancel(true);
		commander = new MediaPlayerCommander();

		TaskParams tp = new TaskParams();
		tp.action = MP_STOP;		

		commander.execute(tp);
	}

	private class MediaPlayerCommander extends AsyncTask<TaskParams, Void, TaskParams>{
		private String err = null;

		@Override
		protected void onPreExecute() {			
			//set imageview to not playing icon
			if(media != null)
				setIcon(MediaStatus.STOPPED);
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			if(media != null)
				media.adapter.notifyDataSetChanged();
		}

		@Override
		protected TaskParams doInBackground(TaskParams... tp) {
			TaskParams params = tp[0];

			try{
				switch(params.action){
				case MP_PLAY:

					if(isCancelled())
						return null;

					preparePlayer(params.media);
					publishProgress();

					if(isCancelled()){
						publishProgress();
						return null;
					}

					startPlayer();
					publishProgress();

					break;
				case MP_STOP:

					if(isCancelled())
						return null;

					stopPlayer();
					publishProgress();

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
				media.status == MediaStatus.STOPPED || media.status == MediaStatus.LOADING){						

			mp.reset();
			mp.setDataSource(m.source);
			mp.prepare();
			mp.setOnCompletionListener(this);
		}	

		m.status = MediaStatus.PREPARED;

		if(Thread.interrupted())
			return;

		media = m;
	}

	private synchronized void startPlayer() throws IllegalStateException{
		mp.start();
		media.status = MediaStatus.PLAYING;		
	}

	private synchronized void stopPlayer() throws IllegalStateException{
		//if(mp.isPlaying()){
			mp.stop();
			media.status = MediaStatus.STOPPED;
		//}

	}

	private void setIcon(MediaStatus status){
		switch(status) {
		case PLAYING:
			//media.icon.setImageResource(R.drawable.pause);
			break;
		case STOPPED:
		default:				
			//media.icon.setImageResource(R.drawable.play);
			break;
		}
	}		

	@Override
	public void onCompletion(MediaPlayer mp) {
		stopPlayer();
		media.adapter.notifyDataSetChanged();
		//media.icon.setImageResource(R.drawable.play);
	}

	private class TaskParams{
		public int action;
		public MediaInfo media;
	}

	private class MediaInfo{
		public String source;
		public BaseAdapter adapter;
		//public ImageView icon;
		public int position;
		public MediaStatus status;
	}

	public enum MediaStatus{
		LOADING, PREPARED, PLAYING, STOPPED;
	}


}
