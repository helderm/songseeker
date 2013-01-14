package com.seekermob.songseeker.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;

import com.seekermob.songseeker.util.Util;

public class UserProfile implements Serializable{

	private static UserProfile obj = new UserProfile();
	private static Profile profile = null;	
	
	//private static AddToProfileTask addTask = null;

	public static final String PROFILE_FILENAME = "profile";
	private static final long serialVersionUID = 1L;
	
	private UserProfile(){}
	
	public static UserProfile getInstance(Context context){
		//check if we have a profile written in the disk		
		if(profile == null){
			if((profile = (Profile)Util.readObjectFromDevice(context, PROFILE_FILENAME)) == null){
				profile = new Profile();
			}
		}
		
		return obj;
	}
	
	public Profile getProfile(){	
		return profile;
	}
	
	public synchronized void syncAddArtistsToProfile(ArrayList<ArtistProfile> artists, Activity activity){
		profile.artists.addAll(artists);		
		Util.writeObjectToDevice(activity, profile, PROFILE_FILENAME);
	}
	
	/** Removes an artist from the profile **/
	public void removeArtistFromProfile(int position, Fragment frag){
		profile.artists.remove(position);
		Util.writeObjectToDevice(frag.getActivity(), profile, PROFILE_FILENAME);	
	}
	
	public boolean isAlreadyInProfile(String id, String name){
		
		if(id != null){
			for(ArtistProfile a : profile.artists){
				if(a.id.equalsIgnoreCase(id))
					return true;
			}
		}
		
		if(name != null){
			for(ArtistProfile a : profile.artists){
				if(a.name.equalsIgnoreCase(name))
					return true;
			}
		}		
		
		return false;
	}
	
	public boolean isEmpty(){
		if(profile == null || profile.artists.size() == 0)
			return true;
		
		return false;
	}
	
	/** Get random artists from the profile.
	 * Main use is to feed the playlist creation*/
	public ArrayList<ArtistInfo> getRandomArtists(int numArtists){
		ArrayList<ArtistInfo> chosenArtists = new ArrayList<ArtistInfo>();
		ArrayList<ArtistProfile> profileArtists = new ArrayList<ArtistProfile>(profile.artists);
		Random rand = new Random();
		
		//add all artists if the profile isnt large enough
		if(numArtists >= profileArtists.size()){
			for(ArtistProfile a : profileArtists){
				ArtistInfo artist = new ArtistInfo();
				artist.name = a.name;
				chosenArtists.add(artist);				
			}
			return chosenArtists;
		}
		
		while(numArtists > 0){        	
			ArtistInfo artist = new ArtistInfo();
			
			//choose a new artist from profile
        	int i = rand.nextInt(profileArtists.size());
			
        	artist.name = profileArtists.get(i).name;
        	chosenArtists.add(artist);
			
			profileArtists.remove(i);		
			numArtists--;
		}
		
		return chosenArtists;
		
	}
	
	public void clearProfile(Activity activity){
		profile = new Profile();
		
		Util.writeObjectToDevice(activity, profile, PROFILE_FILENAME);
	}
	
	public static class Profile implements Serializable{
		public ArrayList<ArtistProfile> artists = new ArrayList<ArtistProfile>();
		
		private static final long serialVersionUID = 1L;
	}

	//Note to self: Cannot use ArtistInfo since it implements Parcel, and Parcels are not suitable to write to disk
	public static class ArtistProfile implements Serializable {
		public String name;
		public String image;
		public String id;
		public String buyUrl;
		
		private static final long serialVersionUID = 1L;
		
		public ArtistProfile() {}
		
		public ArtistProfile(ArtistInfo artist){
			name = artist.name;
			image = artist.image;
			id = artist.id;
			buyUrl = artist.buyUrl;	
		}
	}		
}
