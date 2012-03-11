package com.seekermob.songseeker.data;

import java.io.Serializable;

public class SongInfoCache implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public String name;
	public String trackNum;
	public String buyUrl;
	public String duration;
	public String id;
	public String previewUrl;
	public String version;

	public ArtistInfoCache artist = null;
	public ReleaseInfoCache release = null;
	
	public SongInfoCache(SongInfo song) {
		
		name = song.name;
		trackNum = song.trackNum;
		buyUrl = song.buyUrl;
		duration = song.duration;
		id = song.id;
		previewUrl = song.previewUrl;
		
		if(song.artist != null)
			artist = new ArtistInfoCache(song.artist);
		
		if(song.release != null)
			release = new ReleaseInfoCache(song.release);
		
	}

}
