package com.seekermob.songseeker.util;

import java.io.File;

import android.util.Log;


public class FileCache {
    private File cacheDir;
    
    public FileCache(File unmountedCacheDir, boolean isImportant){
        //Find the dir to save cached images
        if(!isImportant && android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            cacheDir=new File(android.os.Environment.getExternalStorageDirectory(),"data/SongSeeker");
        else
            cacheDir=unmountedCacheDir;
        if(!cacheDir.exists())
            cacheDir.mkdirs();
    }
    
    public File getFile(String url){
        //I identify images by hashcode. Not a perfect solution, good for the demo.
        String filename=String.valueOf(url.hashCode());
        File f = new File(cacheDir, filename);
        return f;
        
    }
    
    public void clear(File unmountedCacheDir){
    	File[] files = null;
    	
    	if((files = cacheDir.listFiles()) != null){	        	    	
	    	for(File f : files){
	        	f.delete();
	        }
    	}
        
        //we need to garantee that the internal storage is cleaned also
        if(!unmountedCacheDir.getAbsolutePath().equalsIgnoreCase(cacheDir.getAbsolutePath())){
        	if((files = unmountedCacheDir.listFiles()) == null)
        		return;
        	
        	for(File f:files){           	
            	f.delete();
            }
        }        	
    }
    
    public long getCacheSize(File unmountedCacheDir){
    	long totalSize = 0;
    	File[] files = null;

    	try{
    		if((files = cacheDir.listFiles()) != null){
        		for(File f:files){
        			totalSize += f.length();
        		}
    		}
    		
            //check internal storage also
            if(!unmountedCacheDir.getAbsolutePath().equalsIgnoreCase(cacheDir.getAbsolutePath())){
            	if((files = unmountedCacheDir.listFiles()) == null)
            		return 0;
            	
            	for(File f:files){
                	totalSize += f.length();
                }
            }  
    		
    	}catch (Exception e) { //TODO: TEMPORARY! Just for an emergency issue. Should be fixed with the 'if(listFiles == null)'
    		Log.e(Util.APP, "Error while trying to fetch the cache size!", e);
    		return 0;
    	}

    	return totalSize;
    }
}