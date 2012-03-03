package com.seekermob.songseeker.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

public class Util {

	public static final String APP = "SongSeeker";
	public static final String ECHONEST_URL = "http://the.echonest.com/";
	public static final String SEVENDIGITAL_URL = "http://www.7digital.com/";
	public static final String DESIGNER_URL = "http://www.lucassanchez.com.br/";
	public static final String ANDROIDICONS_URL = "http://www.androidicons.com/"; 
	public static final String RDIO_URL = "http://www.rdio.com/";
	public static final String GROOVESHARK_URL = "http://www.grooveshark.com/";
	public static final String YOUTUBE_URL = "http://www.youtube.com/";
	public static final String LASTFM_URL = "http://www.lastfm.com/";
	
	public static ArrayList<String> getArtistsFromDevice(Activity a) throws Exception{
		ArrayList<String> artists = new ArrayList<String>();

		String[] projection = new String[] {
				MediaStore.Audio.ArtistColumns.ARTIST,
				MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS
		};

		Cursor cursor = a.getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, 
				projection, null, null, MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS + " DESC");

		int music_column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.ARTIST);

		for(int i=0; i<cursor.getCount(); i++){
			cursor.moveToPosition(i);
			String artist = cursor.getString(music_column_index);	
			if(artist != null)
				artists.add(artist);
			
			if(i == 50)
				break;
		}

		cursor.close();

		return artists;
	}

	public static void CopyStream(InputStream is, OutputStream os){
		final int buffer_size=1024;
		try{
			byte[] bytes=new byte[buffer_size];
			for(;;){
				int count=is.read(bytes, 0, buffer_size);
				if(count==-1)
					break;
				os.write(bytes, 0, count);
			}
		}
		catch(Exception ex){}
	}
	
	/** Save the object to the device persistent storage*/
	public static void  writeObjectToDevice(Activity activity, Object object, String filename){		
		ObjectOutputStream out = null;
		
		try {
			FileOutputStream fos = activity.openFileOutput(filename, Context.MODE_PRIVATE);
			out = new ObjectOutputStream(fos);
			out.writeObject(object);
			out.close();
		} catch (Exception e) {
			Log.w(Util.APP, "Couldnt write "+filename +" file to device memory, it may be lost in the next reboot of the app!", e);
		} finally{
			if(out != null){
				try {
					out.close();
				} catch (IOException e) {}
			}
		}
	}
	
	/** Restore the object written on disk, if it exists */
	public static Object readObjectFromDevice(Activity activity, String filename){
		Object object = null;
		ObjectInputStream in = null;
		try{			
			in = new ObjectInputStream(activity.openFileInput(filename));
			object = in.readObject();			
		}catch(FileNotFoundException e){   
			return null;
		}catch(Exception e){
			Log.w(Util.APP, "Couldnt read "+filename +" file from device memory, nothing serious...", e);
			return null;
		}finally{
			if(in != null){
				try {
					in.close();
				} catch (IOException e) {}
			}
		}
		
		return object;
	}	
	
	/** Restore the object written on the cache, if it exists */
	public static Object readObjectFromCache(Activity activity, String filename){
		Object object = null;
		ObjectInputStream in = null;
		
		File f = new File(activity.getCacheDir(), filename);
		
		try{			
			in = new ObjectInputStream(new FileInputStream(f));
			object = in.readObject();			
		}catch(FileNotFoundException e){   
			return null;
		}catch(Exception e){			
			return null;
		}finally{
			if(in != null){
				try {
					in.close();
				} catch (IOException e) {}
			}
		}
		
		return object;
	}	
}
