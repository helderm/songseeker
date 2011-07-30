package com.android.songseeker;

public class Settings {

	private float energy;
	private float danceability;
	private int tempo;
	private int mood; //1-[sad]; 2-[sad,angry]; 3-[angry]; 4-[angry-cool]; 5-[cool]; 
						//6-[cool-happy]; 7-[happy]; 8-[happy-excited]; 9-[excited]
	private float hotness;
	
	
	public Settings(){
		energy = 0.5f;
		danceability = 0.5f;
		tempo = 250;
		mood = 5;		
	}


	public float getEnergy() {
		return energy;
	}


	public void setEnergy(float energy) {
		this.energy = energy;
	}


	public float getDanceability() {
		return danceability;
	}


	public void setDanceability(float danceability) {
		this.danceability = danceability;
	}


	public int getTempo() {
		return tempo;
	}


	public void setTempo(int tempo) {
		this.tempo = tempo;
	}


	public int getMood() {
		return mood;
	}


	public void setMood(int mood) {
		this.mood = mood;
	}


	public float getHotness() {
		return hotness;
	}


	public void setHotness(float hotness) {
		this.hotness = hotness;
	}
	
	
}
