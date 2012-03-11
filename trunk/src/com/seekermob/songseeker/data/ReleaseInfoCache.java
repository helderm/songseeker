package com.seekermob.songseeker.data;

import java.io.Serializable;

public class ReleaseInfoCache implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public String name;
	public String id;
	public String buyUrl;
	public String image;
	
	public ArtistInfoCache artist = null;
	
	
	public ReleaseInfoCache(ReleaseInfo release) {
		name = release.name;
		id = release.id;
		buyUrl = release.buyUrl;
		image = release.image;
		
		if(release.artist != null)
			artist = new ArtistInfoCache(release.artist);
	}
}

