package com.android.songseeker.comm.youtube;

import com.google.api.client.util.Key;

public class Playlist extends Item{
	  
	@Key
	public FeedLink feedLink;
	
	@Key
	public String description;
	
	@Key
	public String author;
}
