package com.seekermob.songseeker.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.seekermob.songseeker.util.Util;

import android.app.Activity;

public class PlaylistOptions implements Serializable{
	private static PlaylistOptions obj = new PlaylistOptions();
	private static PlaylistOptionsData data = null;
	
	public static final int PL_MAX_TEMPO = 500;		
	private static final String PL_OPTIONS_FILENAME = "settings";
	
	private static final long serialVersionUID = 1L;
	
	public static PlaylistOptions getInstance(Activity activity){		
		
		//check if we have settings written in the disk		
		if(data == null){			
			if((data = (PlaylistOptionsData)Util.readObjectFromDevice(activity, PL_OPTIONS_FILENAME)) == null){
				data = obj.new PlaylistOptionsData();
			}
		}
		
		return obj;
	}
	
	public static PlaylistOptions getInstance(){
		return obj;
	}
	
	public PlaylistOptionsData getSettings(){
		return data;
	}
	
	public float getMinEnergy(){
		if(data.pl_energy == 0){
			return -1.0f;
		}
		
		if(data.pl_energy < 20){
			return 0.0f;
		}		
		return (data.pl_energy - 20)/100.0f;
	}	
	public float getMaxEnergy(){
		if(data.pl_energy == 0){
			return -1.0f;
		}
		
		if(data.pl_energy > 80){
			return 1.0f;
		}		
		return (data.pl_energy + 20)/100.0f;
	}
	
	public float getMinDanceability(){
		if(data.pl_danceability == 0){
			return -1.0f;
		}
		
		if(data.pl_danceability < 20){
			return 0.0f;
		}		
		return (data.pl_danceability - 20)/100.0f;
	}	
	public float getMaxDanceability(){
		if(data.pl_danceability == 0){
			return -1.0f;
		}
		
		if(data.pl_danceability > 80){
			return 1.0f;
		}		
		return (data.pl_danceability + 20)/100.0f;
	}
	
	public float getMinTempo(){
		if(data.pl_tempo == 0){
			return -1.0f;
		}
		
		if(data.pl_tempo < 20){
			return 0.0f;
		}		
		return (((data.pl_tempo - 20)/100.0f)*PL_MAX_TEMPO);
	}	
	public float getMaxTempo(){
		if(data.pl_tempo == 0){
			return -1.0f;
		}
		
		if(data.pl_tempo > 80){
			return 1.0f;
		}		
		return (((data.pl_tempo + 20)/100.0f)*PL_MAX_TEMPO);
	}
	
	public float getMinHotness(){
		if(data.pl_hotness == 0){
			return -1.0f;
		}
		
		if(data.pl_hotness < 20){
			return 0.0f;
		}		
		return (data.pl_hotness - 20)/100.0f;
	}	
	public float getMaxHotness(){
		if(data.pl_hotness == 0){
			return -1.0f;
		}
		
		if(data.pl_hotness > 80){
			return 1.0f;
		}		
		return (data.pl_hotness + 20)/100.0f;
	}
	
	public List<String> getMood(){
		if(data.pl_mood == 0){
			return null;
		}
		
		List<String> moods = new ArrayList<String>();		
		
		if(data.pl_mood <= 10){
			moods.add("sad");
			return moods;
		}
		if(data.pl_mood >= 11 && data.pl_mood <= 22){
			moods.add("sad");
			moods.add("angry");
			return moods;
		}
		if(data.pl_mood >= 23 && data.pl_mood <= 33){
			moods.add("angry");
			return moods;
		}
		if(data.pl_mood >= 34 && data.pl_mood <= 45){
			moods.add("angry");
			moods.add("cool");
			return moods;
		}
		if(data.pl_mood >= 46 && data.pl_mood <= 56){
			moods.add("cool");
			return moods;
		}
		if(data.pl_mood >= 57 && data.pl_mood <= 68){
			moods.add("cool");
			moods.add("happy");
			return moods;
		}
		if(data.pl_mood >= 69 && data.pl_mood <= 79){
			moods.add("happy");
			return moods;
		}
		if(data.pl_mood >= 80 && data.pl_mood <= 91){
			moods.add("happy");
			moods.add("excited");
			return moods;
		}
		if(data.pl_mood >= 91){
			moods.add("excited");
			return moods;
		}
		
		return null;
	}
	
	public float getVariety(){
		if(data.pl_variety == 0){
			return -1.0f;
		}
		
		return data.pl_variety/100.0f;		
	}
	
	public int getMaxResults(){
		return data.pl_max_results;
	}
	
	public void saveSettings(Activity activity){
		Util.writeObjectToDevice(activity, data, PL_OPTIONS_FILENAME);		
	}
	
	public class PlaylistOptionsData implements Serializable{
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
