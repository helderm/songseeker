package com.android.songseeker.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.database.Cursor;
import android.provider.MediaStore;

public class Util {

	public static final String APP = "SongSeeker";

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
			
			if(i == 30)
				break;
		}

		cursor.close();

		return artists;
	}

	public static void CopyStream(InputStream is, OutputStream os)
	{
		final int buffer_size=1024;
		try
		{
			byte[] bytes=new byte[buffer_size];
			for(;;)
			{
				int count=is.read(bytes, 0, buffer_size);
				if(count==-1)
					break;
				os.write(bytes, 0, count);
			}
		}
		catch(Exception ex){}
	}
}
