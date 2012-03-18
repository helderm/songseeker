package com.seekermob.songseeker.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import android.content.Context;
import android.util.Log;


public class FileCache {
    private static FileCache obj = new FileCache();
	
	private static File externalCacheDir = null;
    private static File internalCacheDir = null;
    
    private FileCache(){  }
    
    public static void setCacheDirs(Context context){
    	externalCacheDir = context.getExternalCacheDir();
    	internalCacheDir = context.getCacheDir();
    	
    	//build needed directories
    	File dir = new File(internalCacheDir + FileType.IMAGE.getDir());
    	if(!dir.exists())
    		dir.mkdirs();  
    	
    	dir = new File(internalCacheDir + FileType.SONG.getDir());
    	if(!dir.exists())
    		dir.mkdirs();  
    	
    	dir = new File(internalCacheDir + FileType.ARTIST.getDir());
    	if(!dir.exists())
    		dir.mkdirs();  
    	
    	if(externalCacheDir != null && android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
        	dir = new File(externalCacheDir + FileType.IMAGE.getDir());
        	if(!dir.exists())
        		dir.mkdirs();      
        	
        	dir = new File(externalCacheDir + FileType.SONG.getDir());
        	if(!dir.exists())
        		dir.mkdirs(); 
        	
        	dir = new File(externalCacheDir + FileType.ARTIST.getDir());
        	if(!dir.exists())
        		dir.mkdirs();  
    	}
    }
    
    public static FileCache getCache(Context context){
    	
    	if(externalCacheDir == null){
        	externalCacheDir = context.getExternalCacheDir();
        }
    	
    	if(internalCacheDir == null){
    		internalCacheDir = context.getCacheDir();
    	}
    	
    	if(externalCacheDir == null && internalCacheDir == null){
    		Log.w(Util.APP, "Cache dirs not set!");
    		return null;
    	}
    	return obj;
    }
    
    public static FileCache getCache(){
    	if(externalCacheDir == null && internalCacheDir == null){
    		Log.w(Util.APP, "Cache dirs not set!");
    		return null;
    	}
    	return obj;
    }
    
    
    public File getFile(String filename, FileType type){
    	boolean isMounted = false;
    	
    	if(externalCacheDir != null && android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
    		isMounted = true;
    	}
    	
    	switch(type){
    	
    	case IMAGE:
    		if(isMounted){
    			return new File(externalCacheDir + FileType.IMAGE.getDir(), String.valueOf(filename.hashCode()));
    		}else{
    			return new File(internalCacheDir + FileType.IMAGE.getDir(), String.valueOf(filename.hashCode()));
    		}  		
    		
    	case SONG:
    		if(isMounted){
    			return new File(externalCacheDir + FileType.SONG.getDir(), filename);
    		}else{
    			return new File(internalCacheDir + FileType.SONG.getDir(), filename);
    		}  
    		
    	case ARTIST:
    		if(isMounted){
    			return new File(externalCacheDir + FileType.ARTIST.getDir(), filename);
    		}else{
    			return new File(internalCacheDir + FileType.ARTIST.getDir(), filename);
    		}     		
    	}
    	
    	return null;
    }
    
    public void putObject(Serializable obj, String filename, FileType type) throws Exception{		
    	OutputStream fos = new FileOutputStream(getFile(filename, type));
		ObjectOutputStream out = new ObjectOutputStream(fos);
		out.writeObject(obj);
		out.close();
    }
    
    public Object getObject(String filename, FileType type) throws Exception{
    	try{
    		ObjectInputStream in = new ObjectInputStream(new FileInputStream(getFile(filename, type)));
			return in.readObject();    	
    	}catch(FileNotFoundException e){
    		return null;
    	}catch(Exception e){
    		Log.w(Util.APP, "Unable to load a file from cache...", e);
    		return null;
    	}
    }
    
    public void clear(){
    	File[] files = null;
    	File dir = null;
    	
    	//removes files in the external storage
    	if(externalCacheDir != null && android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
    		dir = new File(externalCacheDir + FileType.IMAGE.getDir());    		
    		if((files = dir.listFiles()) != null){	        	    	
    	    	for(File f : files){
    	        	f.delete();
    	        }
        	}
    		
    		dir = new File(externalCacheDir + FileType.SONG.getDir());
    		if((files = dir.listFiles()) != null){	        	    	
    	    	for(File f : files){
    	        	f.delete();
    	        }
        	}
    		
    		dir = new File(externalCacheDir + FileType.ARTIST.getDir());
    		if((files = dir.listFiles()) != null){	        	    	
    	    	for(File f : files){
    	        	f.delete();
    	        }
        	}    		
    	}
		
    	//removes files from the internal storage
    	dir = new File(internalCacheDir + FileType.IMAGE.getDir());    		
		if((files = dir.listFiles()) != null){	        	    	
	    	for(File f : files){
	        	f.delete();
	        }
    	}
		
		dir = new File(internalCacheDir + FileType.SONG.getDir());
		if((files = dir.listFiles()) != null){	        	    	
	    	for(File f : files){
	        	f.delete();
	        }
    	}    
		
		dir = new File(internalCacheDir + FileType.ARTIST.getDir());
		if((files = dir.listFiles()) != null){	        	    	
	    	for(File f : files){
	        	f.delete();
	        }
    	}     	
    }
    
    public long getCacheSize(){
    	long totalSize = 0;
    	File[] files = null;
    	File dir = null;

    	try{
    		
        	if(externalCacheDir != null && android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
        		dir = new File(externalCacheDir + FileType.IMAGE.getDir());    		
        		if((files = dir.listFiles()) != null){	        	    	
        	    	for(File f : files){
        	    		totalSize += f.length();
        	        }
            	}
        		
        		dir = new File(externalCacheDir + FileType.SONG.getDir());
        		if((files = dir.listFiles()) != null){	        	    	
        	    	for(File f : files){
        	    		totalSize += f.length();
        	        }
            	}
        		
        		dir = new File(externalCacheDir + FileType.ARTIST.getDir());
        		if((files = dir.listFiles()) != null){	        	    	
        	    	for(File f : files){
        	    		totalSize += f.length();
        	        }
            	}
        	}
    		
        	dir = new File(internalCacheDir + FileType.IMAGE.getDir());    		
    		if((files = dir.listFiles()) != null){	        	    	
    	    	for(File f : files){
    	    		totalSize += f.length();
    	        }
        	}
    		
    		dir = new File(internalCacheDir + FileType.SONG.getDir());
    		if((files = dir.listFiles()) != null){	        	    	
    	    	for(File f : files){
    	    		totalSize += f.length();
    	        }
        	} 
    		
    		dir = new File(internalCacheDir + FileType.ARTIST.getDir());
    		if((files = dir.listFiles()) != null){	        	    	
    	    	for(File f : files){
    	    		totalSize += f.length();
    	        }
        	} 
    		
    	}catch (Exception e) { //TODO: TEMPORARY! Just for an emergency issue. Should be fixed with the 'if(listFiles == null)'
    		Log.e(Util.APP, "Error while trying to fetch the cache size!", e);
    		return 0;
    	}

    	return totalSize;
    }
    
    public enum FileType{
    	IMAGE ("/images"),
    	SONG ("/songs"),
    	ARTIST ("/artists");
    	
    	private String dir;

		FileType(String dir) {
	        this.dir = dir;	        
	    }		
		public String getDir(){
			return this.dir;
		}    	
    }
}