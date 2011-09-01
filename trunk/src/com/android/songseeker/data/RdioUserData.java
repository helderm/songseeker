package com.android.songseeker.data;

import java.util.ArrayList;

public class RdioUserData {
	
	private ArrayList<Playlist> playlists = new ArrayList<RdioUserData.Playlist>();
	
	public RdioUserData(){}
	
	public int getPlaylistsSize(){
		return playlists.size();
	}
	
	public String getPlaylistName(int i){
		return playlists.get(i).name;
	}
	
	public String getPlaylistNumSongs(int i){
		return playlists.get(i).numSongs;
	}
	
	public String getPlaylistImage(int i){
		return playlists.get(i).imageUrl;
	}
	
	public void addPlaylist(String name, String description, String imageUrl){
		Playlist pl = new Playlist(name, description, imageUrl);
		playlists.add(pl);
	}
	
	private class Playlist{
		public String name;
		public String numSongs;
		public String imageUrl;	
		
		public Playlist(String n, String d, String i){
			name = n;
			numSongs = d;
			imageUrl = i;
		}
	}
	
}


