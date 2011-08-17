package com.android.songseeker.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Util {

	public static Bitmap downloadImage(String fileUrl){
		URL myFileUrl =null;
		try {
			myFileUrl= new URL(fileUrl);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		try {
			HttpURLConnection conn= (HttpURLConnection)myFileUrl.openConnection();
			conn.setDoInput(true);
			conn.connect();
			InputStream is = conn.getInputStream();

			Bitmap bmImg = BitmapFactory.decodeStream(is);
			return bmImg;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}



}
