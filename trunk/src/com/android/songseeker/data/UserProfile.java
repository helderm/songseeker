package com.android.songseeker.data;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.android.songseeker.comm.SevenDigitalComm;
import com.android.songseeker.util.FileCache;
import com.android.songseeker.util.Util;

public class UserProfile implements Serializable{

	private static UserProfile obj = new UserProfile();
	private static Profile profile = null;
	
	private static FileCache fileCache = null;

	private static final long serialVersionUID = 1L;
	
	private UserProfile(){}
	
	public static UserProfile getInstance(File unmountedCacheDir){
		if(fileCache == null)
			fileCache = new FileCache(unmountedCacheDir);
		
		//check if we have a profile written in the disk		
		if(profile == null){
			profile = fileCache.getProfile();
			
			if(profile == null)
				profile = obj.new Profile();
		}
		
		return obj;
	}
	
	public Profile getProfile(){
		return profile;
	}
	
	@SuppressWarnings("unchecked")
	public void addToProfile(ArrayList<String> names, Activity a, BaseAdapter ad){
		new GetArtistInfoTask(a, ad, true).execute(names);
	}	
	
	@SuppressWarnings("unchecked")
	public void addIdToProfile(ArrayList<String> ids, Activity a, BaseAdapter ad){
		new GetArtistInfoTask(a, ad, false).execute(ids);
	}	
	
	private class GetArtistInfoTask extends AsyncTask<ArrayList<String>, Void, Void>{

		private String msg = null;
		private String err = null;
		private Activity activity = null;
		private BaseAdapter adapter = null;
		private boolean isSearch;
		
		public GetArtistInfoTask(Activity a, BaseAdapter ad, boolean is) {
			activity = a;
			adapter = ad;
			isSearch = is;
		}
		
		@Override
		protected void onPreExecute() {
			Toast.makeText(activity.getApplicationContext(), "Adding artist(s) to profile, please wait...", 
								Toast.LENGTH_LONG).show();
		}
		
		@Override
		protected Void doInBackground(ArrayList<String>... params) {
			
			ArrayList<ArtistProfile> artists = new ArrayList<ArtistProfile>();
			ArtistInfo artist = null;
			int alreadyProfileCount = 0;
			
			for(String artistNameID : params[0]){

				if(isSearch && isAlreadyInProfile(artistNameID)){
					alreadyProfileCount++;
					continue;
				}
				
				try{
					if(isSearch)
						artist = SevenDigitalComm.getComm().queryArtistSearch(artistNameID);
					else
						artist = SevenDigitalComm.getComm().queryArtistDetails(artistNameID);
						
				}catch(Exception e) {
					Log.w(Util.APP, e.getMessage(), e);
					continue;
				} 
				
				//the string passed by the user may be diff from what is stored at the profile
				if(isAlreadyInProfile(artist.name)){
					alreadyProfileCount++;
					continue;
				}
				
				ArtistProfile ap = new ArtistProfile();
				ap.name = artist.name;
				ap.image = artist.image;
				ap.id = artist.id;
				ap.buyUrl = artist.buyUrl;
				
				//check if the artist was already added to the list
				boolean isAlreadyAdd = false;
				for(ArtistProfile aux : artists){
					if(aux.id.equalsIgnoreCase(ap.id)){
						isAlreadyAdd = true;
						break;
					}
				}
				if(isAlreadyAdd)
					continue;
		
				artists.add(ap);
			}				

			if(params[0].size() == alreadyProfileCount){
				msg = "Artist(s) already in profile!";
				return null;
			}else if(artists.size() == 0){
				err = "Failed to add artist(s) to profile!";
				return null;
			}else if(artists.size() < params[0].size()){
				msg = "Some artists were successfully added to profile!";
			}else
				msg = "Artist(s) successfully added to profile!";				
			
			syncAddArtistsToProfile(artists);
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(err != null){
				Toast.makeText(activity.getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				return;
			}

			adapter.notifyDataSetChanged();		
			
			Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_LONG).show();
		}		
	}	
	
	synchronized void syncAddArtistsToProfile(ArrayList<ArtistProfile> artists){
		profile.artists.addAll(artists);		
		fileCache.saveProfile(profile);
	}
	
	public void removeArtistFromProfile(int position, BaseAdapter adapter){
		profile.artists.remove(position);
		adapter.notifyDataSetChanged();
	}
	
	public boolean isAlreadyInProfile(String artist){
		for(ArtistProfile a : profile.artists){
			if(a.name.equalsIgnoreCase(artist))
				return true;
		}
		
		return false;
	}
	
	public class Profile implements Serializable{
		public ArrayList<ArtistProfile> artists = new ArrayList<ArtistProfile>();
		
		private static final long serialVersionUID = 1L;
	}

	//Note to self: Cannot use ArtistInfo since it implements Parcel, and Parcels are not suitable to write to disk
	public class ArtistProfile implements Serializable {
		public String name;
		public String image;
		public String id;
		public String buyUrl;
		
		private static final long serialVersionUID = 1L;		
	}
	
}
