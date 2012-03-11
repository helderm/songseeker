package com.seekermob.songseeker.data;

import java.io.Serializable;

public class ArtistInfoCache implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public String name;
	public String buyUrl;
	public String id;
	public String image;
	
	public ArtistInfoCache(ArtistInfo artist){
		name = artist.name;
		buyUrl = artist.buyUrl;
		id = artist.id;
		image = artist.image;
	}
}
