package com.seekermob.songseeker.data;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.comm.SevenDigitalComm;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.util.Util;

public class UserProfile implements Serializable, OnCancelListener{

	private static UserProfile obj = new UserProfile();
	private static Profile profile = null;	
	
	private static AddToProfileTask addTask = null;

	private static final String PROFILE_FILENAME = "profile";
	private static final long serialVersionUID = 1L;
	
	private UserProfile(){}
	
	public static UserProfile getInstance(Activity activity){

		//TODO: This checks if we have the 'settings' file in the 'cache' dir
		//if we do, restore that copy and save under the 'files' dir
		//this will be needed until everyone updates to version 10 or later!
		Profile cache = null;
		if((cache = (Profile)Util.readObjectFromCache(activity, PROFILE_FILENAME)) != null){
			//loaded file from the cache! need to remove it now
			profile = cache;
			cache = null;
			File f = new File(activity.getCacheDir(), PROFILE_FILENAME);
			f.delete();
			Util.writeObjectToDevice(activity, profile, PROFILE_FILENAME);
		}
		
		//check if we have a profile written in the disk		
		if(profile == null){
			if((profile = (Profile)Util.readObjectFromDevice(activity, PROFILE_FILENAME)) == null){
				profile = obj.new Profile();
			}
		}
		
		return obj;
	}
	
	public Profile getProfile(){
		return profile;
	}
	
	public void addToProfile(ArrayList<String> names, Activity a, BaseAdapter ad){
		addTask = (AddToProfileTask) new AddToProfileTask(names, a, ad, true, null).execute();
	}
	
	public void addToProfile(ArrayList<String> names, Activity a, BaseAdapter ad, ProgressDialog d){
		addTask = (AddToProfileTask) new AddToProfileTask(names, a, ad, true, d).execute();
	}
	
	public void addIdToProfile(ArrayList<String> ids, Activity a, BaseAdapter ad){
		addTask = (AddToProfileTask) new AddToProfileTask(ids, a, ad, false, null).execute();
	}	
	
	private class AddToProfileTask extends AsyncTask<Void, Integer, Void>{

		private String msg = null;
		private String err = null;
		private Activity activity = null;
		private BaseAdapter adapter = null;
		private ProgressDialog dialog = null;
		private ArrayList<String> artistsList = null;
		
		private boolean isSearch; //true if we have a list of names, false if we have a list of ID's
		private boolean isCancel = false; //true when the user cancel the import
		
		public AddToProfileTask(ArrayList<String> al, Activity a, BaseAdapter ad, boolean is, ProgressDialog d) {
			artistsList = al;
			activity = a;
			adapter = ad;
			isSearch = is;
			dialog = d;
		}
		
		@Override
		protected void onPreExecute() {
			if(dialog != null){				
				dialog.setOnCancelListener(UserProfile.this);
				dialog.show();
				dialog.setMax(artistsList.size());
			}else	
				Toast.makeText(activity.getApplicationContext(), "Adding artist to profile, please wait...", 
								Toast.LENGTH_LONG).show();
		}
		
		@Override
		protected void onProgressUpdate(Integer... value) {
			if(dialog == null)
				return;
			
			dialog.setProgress(value[0]);
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			
			ArrayList<ArtistProfile> artists = new ArrayList<ArtistProfile>();
			ArtistInfo artist = null;
			int alreadyProfileCount = 0;
			int artistsImported = 0;
			
			for(String artistNameID : artistsList){

				if(isCancel)
					break;
				
				if(isSearch && isAlreadyInProfile(artistNameID)){
					alreadyProfileCount++;
					publishProgress(++artistsImported);
					continue;
				}
				
				try{
					if(isSearch)
						artist = SevenDigitalComm.getComm().queryArtistSearch(artistNameID);
					else
						artist = SevenDigitalComm.getComm().queryArtistDetails(artistNameID, activity.getApplicationContext());
						
				}catch(ServiceCommException e) {
					if(e.getErr() == ServiceErr.IO || e.getErr() == ServiceErr.TRY_LATER){
						break;
					}
					
					Log.i(Util.APP, "Unable to add artist ["+artistNameID+"] to profile, skipping...");
					publishProgress(++artistsImported);
					continue;
				} 
				
				//the string passed by the user may be diff from what is stored at the profile
				if(isAlreadyInProfile(artist.name)){
					alreadyProfileCount++;
					publishProgress(++artistsImported);
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
				if(isAlreadyAdd){
					publishProgress(++artistsImported);
					continue;
				}
		
				artists.add(ap);
				
				publishProgress(++artistsImported);
			}				

			if(artistsList.size() == alreadyProfileCount || (artists.size() == 0 && alreadyProfileCount > 0)){
				msg = "Artist(s) already in your profile!";
				return null;
			}else if(artists.size() == 0){
				err = "Failed to add artist(s) to your profile!";
				return null;
			}else if(artists.size() < artistsList.size()){
				msg = "Some artists were successfully added to your profile!";
			}else
				msg = "Artist(s) successfully added to your profile!";				
			
			syncAddArtistsToProfile(artists, activity);
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(dialog != null)
				dialog.dismiss();
			
			if(err != null){
				Toast.makeText(activity.getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				return;
			}

			if(adapter != null)
				adapter.notifyDataSetChanged();		
			
			Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_LONG).show();
			
			activity = null;
			adapter = null;
			dialog = null;
		}		
	}	
	
	synchronized void syncAddArtistsToProfile(ArrayList<ArtistProfile> artists, Activity activity){
		profile.artists.addAll(artists);		
		Util.writeObjectToDevice(activity, profile, PROFILE_FILENAME);
	}
	
	/** Removes an artist from the profile
	 * Is not sync because it is called from the UI thread. Shouldn't be a problem while the addToProfile task has a ProgressDialog*/
	public void removeArtistFromProfile(int position, ListActivity activity){
		profile.artists.remove(position);
		Util.writeObjectToDevice(activity, profile, PROFILE_FILENAME);	
		
		((BaseAdapter)activity.getListAdapter()).notifyDataSetChanged();
	}
	
	public boolean isAlreadyInProfile(String artist){
		for(ArtistProfile a : profile.artists){
			if(a.name.equalsIgnoreCase(artist))
				return true;
		}
		
		return false;
	}
	
	public boolean isEmpty(){
		if(profile == null || profile.artists.size() == 0)
			return true;
		
		return false;
	}
	
	/** Get random artists from the profile.
	 * Main use is to feed the playlist/event creation*/
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
		profile = obj.new Profile();
		
		Util.writeObjectToDevice(activity, profile, PROFILE_FILENAME);
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
	
	@Override
	public void onCancel(DialogInterface dialog) {
		if(addTask != null){
			addTask.isCancel = true;
		}
	}	
}
