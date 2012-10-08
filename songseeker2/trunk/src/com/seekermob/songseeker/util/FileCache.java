package com.seekermob.songseeker.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.integralblue.httpresponsecache.HttpResponseCache;
import com.jakewharton.DiskLruCache;
import com.jakewharton.DiskLruCache.Editor;

import android.content.Context;
import android.util.Log;

public class FileCache {
	private static FileCache obj = new FileCache();

	private File externalCacheDir;
	private File internalCacheDir;
	
	private DiskLruCache imageCache;
	
	private static final int CACHE_VERSION = 1;	
	private static final int CACHE_SIZE = 1024 * 1024 * 5; //5MB
	
	private FileCache(){ }

	public static void install(Context context){		
		File httpCacheDir, imageCacheDir;
        
		obj.externalCacheDir = context.getExternalCacheDir();		
		obj.internalCacheDir = context.getCacheDir();
		
		if(obj.externalCacheDir == null && obj.internalCacheDir == null){
			Log.e(Util.APP, "Failed to install cache");
			return;
		}
		
		if(obj.externalCacheDir != null && android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
			httpCacheDir = new File(obj.externalCacheDir, "http");
			imageCacheDir = new File(obj.externalCacheDir, "images");
		}else{
			httpCacheDir = new File(obj.internalCacheDir, "http");	
			imageCacheDir = new File(obj.internalCacheDir, "images");
		}
		try {
				com.integralblue.httpresponsecache.HttpResponseCache.install(httpCacheDir, CACHE_SIZE);
	    }catch(Exception e) {
	        	Log.e(Util.APP, "Failed to set up Http Cache", e);
	    } 			

		try{
			obj.imageCache = DiskLruCache.open(imageCacheDir, CACHE_VERSION, 1, CACHE_SIZE);
		}catch(Exception e){
			Log.e(Util.APP, "Failed to set up Image Cache", e);
		}
	}

	public static FileCache getCache(){
		return obj;
	}

	public BufferedInputStream getImage(String key) throws IOException{
        DiskLruCache.Snapshot snapshot;

        if(imageCache == null) 
        	return null;
        
        snapshot = imageCache.get(Integer.toString(key.hashCode()));
        if (snapshot == null) 
        	return null;
                               
        return new BufferedInputStream(snapshot.getInputStream(0)); 
	}
	
	public void putImage(InputStream is, String key) throws IOException{
		
        if(imageCache == null) 
        	return;
		
		Editor editor = imageCache.edit(Integer.toString(key.hashCode()));		
		Util.CopyStream(is, editor.newOutputStream(0));
		editor.commit();		
	}

	public void clear(Context c){

		try {
			if(imageCache != null){
				imageCache.delete();				
			}
			
			HttpResponseCache httpCache = com.integralblue.httpresponsecache.HttpResponseCache.getInstalled();
			if(httpCache != null)
				httpCache.delete();
			
			install(c);
			
		} catch (IOException e) {
			Log.e(Util.APP, "Failed to destroy the Http Cache", e);
			return;
		}		
		
	}
}