package com.android.songseeker.data;

import com.android.songseeker.data.ArtistInfo;

public class ReleaseInfo {
	public String name;
	public String id;
	public String buyUrl;
	public String image;
	
	public ArtistInfo artist;
	
	public ReleaseInfo() {
		artist = new ArtistInfo();
	}
}
