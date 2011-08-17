package com.android.songseeker.util;

import java.util.ArrayList;
import java.util.List;

public class Settings {

	//playlist options
	public static int pl_energy = 50;
	public static int pl_danceability = 50;
	public static int pl_tempo = 50;
	public static int pl_hotness = 50;
	public static int pl_mood = 50; 
	public static int pl_variety = 50;
	public static int pl_max_results = 20;
	
	public static final int PL_MAX_TEMPO = 500;
	
	public static float getMinEnergy(){
		if(pl_energy == 0){
			return -1.0f;
		}
		
		if(pl_energy < 20){
			return 0.0f;
		}		
		return (pl_energy - 20)/100.0f;
	}	
	public static float getMaxEnergy(){
		if(pl_energy == 0){
			return -1.0f;
		}
		
		if(pl_energy > 80){
			return 1.0f;
		}		
		return (pl_energy + 20)/100.0f;
	}
	
	public static float getMinDanceability(){
		if(pl_danceability == 0){
			return -1.0f;
		}
		
		if(pl_danceability < 20){
			return 0.0f;
		}		
		return (pl_danceability - 20)/100.0f;
	}	
	public static float getMaxDanceability(){
		if(pl_danceability == 0){
			return -1.0f;
		}
		
		if(pl_danceability > 80){
			return 1.0f;
		}		
		return (pl_danceability + 20)/100.0f;
	}
	
	public static float getMinTempo(){
		if(pl_tempo == 0){
			return -1.0f;
		}
		
		if(pl_tempo < 20){
			return 0.0f;
		}		
		return (((pl_tempo - 20)/100.0f)*PL_MAX_TEMPO);
	}	
	public static float getMaxTempo(){
		if(pl_tempo == 0){
			return -1.0f;
		}
		
		if(pl_tempo > 80){
			return 1.0f;
		}		
		return (((pl_tempo + 20)/100.0f)*PL_MAX_TEMPO);
	}
	
	public static float getMinHotness(){
		if(pl_hotness == 0){
			return -1.0f;
		}
		
		if(pl_hotness < 20){
			return 0.0f;
		}		
		return (pl_hotness - 20)/100.0f;
	}	
	public static float getMaxHotness(){
		if(pl_hotness == 0){
			return -1.0f;
		}
		
		if(pl_hotness > 80){
			return 1.0f;
		}		
		return (pl_hotness + 20)/100.0f;
	}
	
	public static List<String> getMood(){
		if(pl_mood == 0){
			return null;
		}
		
		List<String> moods = new ArrayList<String>();		
		
		if(pl_mood <= 10){
			moods.add("sad");
			return moods;
		}
		if(pl_mood >= 11 && pl_mood <= 22){
			moods.add("sad");
			moods.add("angry");
			return moods;
		}
		if(pl_mood >= 23 && pl_mood <= 33){
			moods.add("angry");
			return moods;
		}
		if(pl_mood >= 34 && pl_mood <= 45){
			moods.add("angry");
			moods.add("cool");
			return moods;
		}
		if(pl_mood >= 46 && pl_mood <= 56){
			moods.add("cool");
			return moods;
		}
		if(pl_mood >= 57 && pl_mood <= 68){
			moods.add("cool");
			moods.add("happy");
			return moods;
		}
		if(pl_mood >= 69 && pl_mood <= 79){
			moods.add("happy");
			return moods;
		}
		if(pl_mood >= 80 && pl_mood <= 91){
			moods.add("happy");
			moods.add("excited");
			return moods;
		}
		if(pl_mood >= 91){
			moods.add("excited");
			return moods;
		}
		
		return null;
	}
	
	public static float getVariety(){
		if(pl_variety == 0){
			return -1.0f;
		}
		
		return pl_variety/100.0f;		
	}
	
	public static int getMaxResults(){
		return pl_max_results;
	}
	
}
