package com.android.songseeker.comm.youtube;

import com.google.api.client.util.Key;

public class Playlist extends Item{
	  
	@Key
	public Integer size;
	
	@Key
	public String description;
	
	@Key
	public String author;

	/*@Override
	public JsonHttpContent toContent(JsonFactory jsonFactory) {
		JsonHttpContent result = new JsonHttpContent(jsonFactory, this);
		return result;
	}*/
}
