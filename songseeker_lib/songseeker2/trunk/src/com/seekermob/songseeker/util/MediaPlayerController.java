package com.seekermob.songseeker.util;

import java.io.IOException;

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.SevenDigitalComm;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class MediaPlayerController implements OnCompletionListener {
	private static MediaPlayerController controller = new MediaPlayerController();
	private static MediaPlayer mp = null;

	private static MediaPlayerCommander commander = controller.new MediaPlayerCommander();
	private static MediaInfo media = null;

	private static final int MP_PLAY = 0;
	private static final int MP_STOP = 1;

	private static boolean isPreparing = false;
	
	public static MediaPlayerController getCon(){
		if(mp == null){
			mp = new MediaPlayer();
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
		}

		return controller; 
	}

	public void release(){
		
		try{
			if(commander != null)
				commander.cancel(true);

			if(media != null){
				if(media.icon != null){
					media.icon.setImageResource(R.drawable.ic_image_play);	
					media = null;
				}else if(media.adapter != null){
					BaseAdapter adapter = media.adapter;
					media = null;
					adapter.notifyDataSetChanged();
				}
			}		

			//to avoid hanging inside a prepare()
			if(isPreparing)
				return;

			if(mp != null){
				mp.release();
				mp = null;
			}
		}catch(Exception e){ //TODO: this catches a reported crash, but should be fixed with mp != null
			Log.w(Util.APP, "Error while trying to release Media Player resources!", e);
		}
	}

	public void startStopMedia(String source, String songId, int position, BaseAdapter adapter){
		if(media == null || media.position != position || media.status == MediaStatus.STOPPED){

			if(media != null){
				media.position = position;
				media.status = MediaStatus.LOADING;
				
				if(media.icon != null){
					media.icon.setVisibility(View.VISIBLE);
					media.loading.setVisibility(View.GONE);
					media.icon.setImageResource(R.drawable.ic_image_play);
				}
				
				if(media.adapter != null)
					media.adapter.notifyDataSetChanged();
			}

			start(source, songId, position, adapter);
		}
		else{
			media.status = MediaStatus.STOPPED;
			
			if(media.adapter != null)
				media.adapter.notifyDataSetChanged();
			
			stop();
		}
	}
	
	public void startStopMedia(String source, String songId, ImageView icon, ProgressBar loading){
		if(media == null || media.adapter != null || media.status == MediaStatus.STOPPED){
			
			if(media != null){
				media.position = -1;
				media.status = MediaStatus.LOADING;
				
				if(media.icon != null){
					media.icon.setVisibility(View.VISIBLE);
					media.loading.setVisibility(View.GONE);
					media.icon.setImageResource(R.drawable.ic_image_play);
				}
				
				if(media.adapter != null)
					media.adapter.notifyDataSetChanged();
			
				media.icon = icon;
				media.loading = loading;
			}
			
			start(source, songId, icon, loading);
		}
		else{
			media.status = MediaStatus.STOPPED;
			
			if(media.icon != null){
				media.icon.setVisibility(View.VISIBLE);
				media.loading.setVisibility(View.GONE);
				media.icon.setImageResource(R.drawable.ic_image_play);
			}
			
			stop();
		}
	}

	public MediaStatus getStatus(int position){

		if(media == null || media.position != position)
			return MediaStatus.STOPPED;

		return media.status;
	}

	public void start(String source, String songId, int position, BaseAdapter adapter){
		commander.cancel(true);
		commander = new MediaPlayerCommander();

		MediaInfo newMedia = new MediaInfo();
		newMedia.songId = songId;
		newMedia.source = source;
		newMedia.position = position;
		newMedia.adapter = adapter;
		newMedia.icon = null;
		newMedia.loading = null;
		newMedia.status = MediaStatus.LOADING;		
		setNewMedia(newMedia);

		TaskParams tp = new TaskParams();
		tp.media = newMedia;
		tp.action = MP_PLAY;

		commander.execute(tp);
	}

	public void start(String source, String songId, ImageView icon, ProgressBar loading){
		commander.cancel(true);
		commander = new MediaPlayerCommander();
				
		MediaInfo newMedia = new MediaInfo();
		newMedia.songId = songId;
		newMedia.source = source;
		newMedia.icon = icon;
		newMedia.loading = loading;
		newMedia.adapter = null;
		newMedia.position = -1;
		newMedia.status = MediaStatus.LOADING;		
		setNewMedia(newMedia);
		
		newMedia.loading.setVisibility(View.VISIBLE);
		newMedia.icon.setVisibility(View.GONE);		
		
		TaskParams tp = new TaskParams();
		tp.media = newMedia;
		tp.action = MP_PLAY;
		
		commander.execute(tp);
	}
	
	private void setNewMedia(MediaInfo m){
		media = m;
		
		if(media.adapter != null)
			media.adapter.notifyDataSetChanged();
		
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
		protected void onProgressUpdate(MediaInfo... m) {
			
			if(m[0] != null && m[0].adapter != null){
				m[0].adapter.notifyDataSetChanged();
			}			
				
			if(m[0]!= null && m[0].icon != null){
				switch(m[0].status){
				case LOADING:
					m[0].loading.setVisibility(View.VISIBLE);
					m[0].icon.setVisibility(View.GONE);
					break;
				case PLAYING:
					m[0].icon.setVisibility(View.VISIBLE);
					m[0].loading.setVisibility(View.GONE);
					m[0].icon.setImageResource(R.drawable.ic_image_pause);
					break;
				case STOPPED:
				default:	
					m[0].icon.setVisibility(View.VISIBLE);
					m[0].loading.setVisibility(View.GONE);
					m[0].icon.setImageResource(R.drawable.ic_image_play);
					break;
				}	
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

					//fetches the preview url from 7digital, if it is not set already
					if(params.media.source == null){
						params.media.source = SevenDigitalComm.getComm().getPreviewUrl(params.media.songId);
						if(params.media.source == null)
							throw new Exception("Failed to fetch preview URL from 7digital!");
					}					
					
					preparePlayer(params.media);
					publishProgress(media);

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

			}catch(Exception e){
				Log.w(Util.APP, e.getMessage(), e);
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
			
			isPreparing = true;
			mp.prepare();  //TODO catch IOException
			isPreparing = false;
			
			mp.setOnCompletionListener(this);
		}	

		if(Thread.interrupted())
			return;
		
		m.status = MediaStatus.PREPARED;
		media = m;
	}

	private synchronized void startPlayer() throws IllegalStateException{
		mp.start();
		media.status = MediaStatus.PLAYING;		
	}

	private synchronized void stopPlayer() throws IllegalStateException{
		if(mp != null)
			mp.stop();
		
		if(media != null)
			media.status = MediaStatus.STOPPED;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		stopPlayer();
		if(media != null){
			if(media.adapter != null)
				media.adapter.notifyDataSetChanged();
			if(media.icon != null)
				media.icon.setImageResource(R.drawable.ic_image_play);
		}
	}

	private class TaskParams{
		public int action;
		public MediaInfo media;
	}

	private class MediaInfo{
		public String source;
		public String songId;
		public BaseAdapter adapter;
		public int position;	
		
		public ImageView icon;	//this is used only for the play button outside the adapter
		public ProgressBar loading; //this is used only for the play button outside the adapter
		
		public MediaStatus status;
	}

	public enum MediaStatus{
		LOADING, PREPARED, PLAYING, STOPPED;
	}


}
