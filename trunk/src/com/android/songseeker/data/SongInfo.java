package com.android.songseeker.data;

public class SongInfo {
	public String name;
	public String trackNum;
	public String buyUrl;
	public String duration;
	public String id;
	
	public ArtistInfo artist;
	public ReleaseInfo release;	
	
	public SongInfo() {
		artist = new ArtistInfo();
		release = new ReleaseInfo();
	}
	
	public class ArtistInfo{
		public String name;
		public String buyUrl;
		public String id;
	}
	
	public class ReleaseInfo{
		public String name;
		public String id;
		public String buyUrl;
		public String image;
	}
}
