package com.android.songseeker.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import android.util.Log;

import com.android.songseeker.data.UserProfile.Profile;

public class FileCache {
    
    private File cacheDir;
    
    private static final String PROFILE_FILE = "profile";
    
    public FileCache(File unmountedCacheDir){
        //Find the dir to save cached images
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
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
    
    public Profile getProfile(){
        File f = new File(cacheDir, PROFILE_FILE);
        Profile profile = null;
        
        try{
	        //  Create a stream for reading.
	        FileInputStream fis = new FileInputStream(f);

	        //  Next, create an object that can read from that file.
	        ObjectInputStream inStream = new ObjectInputStream(fis);
	
	        // Retrieve the Serializable object.
	        profile = (Profile)inStream.readObject();
        }catch(FileNotFoundException e){
        	return null;        
    	}catch(Exception e){
        	Log.w(Util.APP, "Unable to fetch user profile from cache", e);
        }
        
        return profile;	
    }
    
    public void saveProfile(Profile prof){
    	File f = new File(cacheDir, PROFILE_FILE);

    	try{
	    	OutputStream fos = new BufferedOutputStream(new FileOutputStream(f));
	
	        //  Next, create an object that can write to that file.
	        ObjectOutputStream outStream = new ObjectOutputStream( fos );
	
	        //  Save each object.
	        outStream.writeObject(prof);
	
	        outStream.flush();
    	
    	}catch(Exception e){
        	Log.w(Util.APP, "Unable to fetch user profile from cache", e);
        }
    }
    
    public void clear(){
    	File[] files=cacheDir.listFiles();
        for(File f:files){
        	if(f.getName().equalsIgnoreCase(PROFILE_FILE))
        		continue;
        	f.delete();
        }
    }

}