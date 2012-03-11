package com.seekermob.songseeker.util;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;

public class Settings implements Serializable{
	private static Settings obj = new Settings();
	private static SettingsData settings = null;
	
	public static final int PL_MAX_TEMPO = 500;		
	private static final String SETTINGS_FILENAME = "settings";
	
	private static final long serialVersionUID = 1L;
	
	public static Settings getInstance(Activity activity){		
		
		//TODO: This checks if we have the 'settings' file in the 'cache' dir
		//if we do, restore that copy, to later save under the 'files' dir
		//this will be needed until everyone updates to version 10 or later!
		SettingsData cache = null;
		if((cache = (SettingsData)Util.readObjectFromCache(activity, SETTINGS_FILENAME)) != null){
			//loaded file from the cache! need to remove it now
			settings = cache;
			cache = null;			
			File f = new File(activity.getCacheDir(), SETTINGS_FILENAME);
			f.delete();
			Util.writeObjectToDevice(activity, settings, SETTINGS_FILENAME);
		}
		
		//check if we have settings written in the disk		
		if(settings == null){			
			if((settings = (SettingsData)Util.readObjectFromDevice(activity, SETTINGS_FILENAME)) == null){
				settings = obj.new SettingsData();
			}
		}
		
		return obj;
	}
	
	public static Settings getInstance(){
		return obj;
	}
	
	public SettingsData getSettings(){
		return settings;
	}
	
	public float getMinEnergy(){
		if(settings.pl_energy == 0){
			return -1.0f;
		}
		
		if(settings.pl_energy < 20){
			return 0.0f;
		}		
		return (settings.pl_energy - 20)/100.0f;
	}	
	public float getMaxEnergy(){
		if(settings.pl_energy == 0){
			return -1.0f;
		}
		
		if(settings.pl_energy > 80){
			return 1.0f;
		}		
		return (settings.pl_energy + 20)/100.0f;
	}
	
	public float getMinDanceability(){
		if(settings.pl_danceability == 0){
			return -1.0f;
		}
		
		if(settings.pl_danceability < 20){
			return 0.0f;
		}		
		return (settings.pl_danceability - 20)/100.0f;
	}	
	public float getMaxDanceability(){
		if(settings.pl_danceability == 0){
			return -1.0f;
		}
		
		if(settings.pl_danceability > 80){
			return 1.0f;
		}		
		return (settings.pl_danceability + 20)/100.0f;
	}
	
	public float getMinTempo(){
		if(settings.pl_tempo == 0){
			return -1.0f;
		}
		
		if(settings.pl_tempo < 20){
			return 0.0f;
		}		
		return (((settings.pl_tempo - 20)/100.0f)*PL_MAX_TEMPO);
	}	
	public float getMaxTempo(){
		if(settings.pl_tempo == 0){
			return -1.0f;
		}
		
		if(settings.pl_tempo > 80){
			return 1.0f;
		}		
		return (((settings.pl_tempo + 20)/100.0f)*PL_MAX_TEMPO);
	}
	
	public float getMinHotness(){
		if(settings.pl_hotness == 0){
			return -1.0f;
		}
		
		if(settings.pl_hotness < 20){
			return 0.0f;
		}		
		return (settings.pl_hotness - 20)/100.0f;
	}	
	public float getMaxHotness(){
		if(settings.pl_hotness == 0){
			return -1.0f;
		}
		
		if(settings.pl_hotness > 80){
			return 1.0f;
		}		
		return (settings.pl_hotness + 20)/100.0f;
	}
	
	public List<String> getMood(){
		if(settings.pl_mood == 0){
			return null;
		}
		
		List<String> moods = new ArrayList<String>();		
		
		if(settings.pl_mood <= 10){
			moods.add("sad");
			return moods;
		}
		if(settings.pl_mood >= 11 && settings.pl_mood <= 22){
			moods.add("sad");
			moods.add("angry");
			return moods;
		}
		if(settings.pl_mood >= 23 && settings.pl_mood <= 33){
			moods.add("angry");
			return moods;
		}
		if(settings.pl_mood >= 34 && settings.pl_mood <= 45){
			moods.add("angry");
			moods.add("cool");
			return moods;
		}
		if(settings.pl_mood >= 46 && settings.pl_mood <= 56){
			moods.add("cool");
			return moods;
		}
		if(settings.pl_mood >= 57 && settings.pl_mood <= 68){
			moods.add("cool");
			moods.add("happy");
			return moods;
		}
		if(settings.pl_mood >= 69 && settings.pl_mood <= 79){
			moods.add("happy");
			return moods;
		}
		if(settings.pl_mood >= 80 && settings.pl_mood <= 91){
			moods.add("happy");
			moods.add("excited");
			return moods;
		}
		if(settings.pl_mood >= 91){
			moods.add("excited");
			return moods;
		}
		
		return null;
	}
	
	public float getVariety(){
		if(settings.pl_variety == 0){
			return -1.0f;
		}
		
		return settings.pl_variety/100.0f;		
	}
	
	public int getMaxResults(){
		return settings.pl_max_results;
	}
	
	public void saveSettings(Activity activity){
		Util.writeObjectToDevice(activity, settings, SETTINGS_FILENAME);		
	}
	
	public class SettingsData implements Serializable{
		public int pl_energy = 0;
		public int pl_danceability = 0;
		public int pl_tempo = 0;
		public int pl_hotness = 0;
		public int pl_mood = 0; 
		public int pl_variety = 0;
		public int pl_max_results = 10;
		public boolean isSimilar = false;
		private static final long serialVersionUID = 1L;
	}
}
