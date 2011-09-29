package com.android.songseeker.data;

public class SongInfo {
	public String name;
	public String trackNum;
	public String buyUrl;
	public String duration;
	public String id;
	public String previewUrl;
	
	public ReleaseInfo release;	
	
	public SongInfo() {
		release = new ReleaseInfo();
	}
}
