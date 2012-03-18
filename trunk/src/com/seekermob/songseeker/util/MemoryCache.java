package com.seekermob.songseeker.util;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import android.graphics.Bitmap;

public class MemoryCache {
    private HashMap<String, SoftReference<Bitmap>> cache=new HashMap<String, SoftReference<Bitmap>>();
    
    private static final int MAX_CACHE_SIZE = 40;
    
    public Bitmap get(String id){
        if(!cache.containsKey(id))
            return null;
        SoftReference<Bitmap> ref=cache.get(id);
        return ref.get();
    }
    
    public void put(String id, Bitmap bitmap){
        //normally SoftReferences are only gc'ed when the app is low on memory
    	//so we clear it periodically to avoid it occupying too much mem
    	if(cache.size() >= MAX_CACHE_SIZE){
        	cache.clear();
        }
    	
    	cache.put(id, new SoftReference<Bitmap>(bitmap));
    }

    public void clear() {
        cache.clear();
    }
}