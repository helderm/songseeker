package com.seekermob.songseeker.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Stack;
import java.util.WeakHashMap;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

public class ImageLoader {
	
	private static ImageLoader loader = new ImageLoader();
    
	private static MemoryCache memoryCache = new MemoryCache();
	private static PhotosQueue photosQueue = loader.new PhotosQueue();
	private static FileCache fileCache = null;
    
    private Map<View, String> views = Collections.synchronizedMap(new WeakHashMap<View, String>());
    private static PhotosLoader photoLoaderThread = null;
    private int stub_id;    
    
    private static final int BMP_ALPHA = 80; 
    
	private ImageLoader(){}
	
	public static ImageLoader getLoader(File unmountedCacheDir){
		if(fileCache == null)
			fileCache = new FileCache(unmountedCacheDir, false);
		
		if(photoLoaderThread == null)
			photoLoaderThread = loader.new PhotosLoader();
		
		return loader;
	}
	
    public void DisplayImage(String url, View view, int stubid){
        stub_id = stubid;
    	views.put(view, url);
        Bitmap bitmap=memoryCache.get(url);
        
        if(bitmap != null){
        	if(view instanceof ImageView){
        		((ImageView)view).setImageBitmap(bitmap);
        	}else{
        		BitmapDrawable drawable = new BitmapDrawable(bitmap);
        		//drawable.setGravity(Gravity.CENTER);
        		drawable.setAlpha(BMP_ALPHA);        		
        		((ListView)view).setBackgroundDrawable(drawable);
        		((ListView)view).setCacheColorHint(0);
        	}
        }
        else{
            if(url != null)
            	queuePhoto(url, view, false);
            
            if(view instanceof ImageView)
            	((ImageView)view).setImageResource(stub_id);
        }    
    }
    
    public void DisplayImage(String url, ListView list, ImageView image){
    	views.put(image, url);
        Bitmap bitmap=memoryCache.get(url);
        
        list.setCacheColorHint(0);     
		
        if(bitmap != null){
            BitmapDrawable drawable = new BitmapDrawable(bitmap);
    		image.setImageDrawable(drawable);
    		drawable.setAlpha(BMP_ALPHA);
        } else if(url != null){
        	queuePhoto(url, image, true);
        }       	
    }
        
    private void queuePhoto(String url, View view, boolean isBkg)
    {
        //This ImageView may be used for other images before. So there may be some old tasks in the queue. We need to discard them. 
        photosQueue.Clean(view);
        PhotoToLoad p = new PhotoToLoad(url, view, isBkg);
        synchronized(photosQueue.photosToLoad){
            photosQueue.photosToLoad.push(p);
            photosQueue.photosToLoad.notifyAll();
        }
        
        //start thread if it's not started yet
        if(photoLoaderThread.getState()==Thread.State.NEW ||
        	photoLoaderThread.getState()==Thread.State.TERMINATED)
            
        	photoLoaderThread.start();
    }
    
    private Bitmap getBitmap(String url) 
    {
        File f=fileCache.getFile(url);
        
        //from SD cache
        Bitmap b = decodeFile(f);
        if(b!=null)
            return b;
        
        //from web
        try {
            Bitmap bitmap=null;
            URL imageUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection)imageUrl.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            InputStream is=conn.getInputStream();
            OutputStream os = new FileOutputStream(f);
            Util.CopyStream(is, os);
            os.close();
            bitmap = decodeFile(f);
            return bitmap;
        } catch (Exception ex){
           Log.w(Util.APP, "Failed to download image from ["+url+"]", ex);
           return null;
        }
    }

    //decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f){
        try {
            //decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f),null,o);
            
            //Find the correct scale value. It should be the power of 2.
            final int REQUIRED_SIZE=70;
            int width_tmp=o.outWidth, height_tmp=o.outHeight;
            int scale=1;
            while(true){
                if(width_tmp/2<REQUIRED_SIZE || height_tmp/2<REQUIRED_SIZE)
                    break;
                width_tmp/=2;
                height_tmp/=2;
                scale*=2;
            }
            
            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize=scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {}
        return null;
    }
    
    //Task for the queue
    private class PhotoToLoad {
        public String url;
        public View view;
        public boolean isBkg;
        public PhotoToLoad(String u, View i, boolean b){
            url=u; 
            view=i;
            isBkg = b;
        }
    }
    
    public void stopThread(){
        photoLoaderThread.interrupt();
        photoLoaderThread = null;
    }
    
    //stores list of photos to download
    class PhotosQueue
    {
        private Stack<PhotoToLoad> photosToLoad=new Stack<PhotoToLoad>();
        
        //removes all instances of this ImageView
        public void Clean(View image){
            for(int j=0; j<photosToLoad.size();){
                if(photosToLoad.get(j).view==image)
                    photosToLoad.remove(j);
                else
                    ++j;
            }
        }
    }
    
    class PhotosLoader extends Thread {
        public void run() {
            try {
                while(true)
                {
                    //thread waits until there are any images to load in the queue
                    if(photosQueue.photosToLoad.size()==0)
                        synchronized(photosQueue.photosToLoad){
                            photosQueue.photosToLoad.wait();
                        }
                    if(photosQueue.photosToLoad.size()!=0)
                    {
                        PhotoToLoad photoToLoad;
                        synchronized(photosQueue.photosToLoad){
                            photoToLoad=photosQueue.photosToLoad.pop();
                        }
                        Bitmap bmp=getBitmap(photoToLoad.url);
                        memoryCache.put(photoToLoad.url, bmp);
                        String tag=views.get(photoToLoad.view);
                        if(tag!=null && tag.equals(photoToLoad.url)){
                            BitmapDisplayer bd=new BitmapDisplayer(bmp, photoToLoad.view, photoToLoad.isBkg);
                            Activity a=(Activity)photoToLoad.view.getContext();
                            a.runOnUiThread(bd);
                        }
                    }
                    if(Thread.interrupted())
                        break;
                }
            } catch (InterruptedException e) {
                //allow thread to exit
            }
        }
    }
    
    //Used to display bitmap in the UI thread
    class BitmapDisplayer implements Runnable{
    	Bitmap bitmap;
    	View view;
    	boolean isBkg;
    	
    	public BitmapDisplayer(Bitmap b, View i, boolean o){bitmap=b; view=i; isBkg = o;}
    	public void run(){

    		if(bitmap!=null){
   				if(!isBkg)
   					((ImageView)view).setImageBitmap(bitmap);
   				else{
   		            BitmapDrawable drawable = new BitmapDrawable(bitmap);   		    		       		
   		    		((ImageView)view).setImageDrawable(drawable);
   		    		drawable.setAlpha(BMP_ALPHA); 
   				}
   					
    		}else
    			if(!isBkg){
    				((ImageView)view).setImageResource(stub_id);
    			}
    		}
    }

	public void clearCache(File unmountedCacheDir) {
		memoryCache.clear();
		fileCache.clear(unmountedCacheDir);
	}    
}