package com.android.songseeker.comm.youtube;

import com.google.api.client.googleapis.GoogleUrl;
import com.google.api.client.util.Key;

public class Urls {
	static final String ROOT_URL = "https://gdata.youtube.com/feeds/api"; 
	
	public static class YouTubeVideoUrl extends GoogleUrl {	

		@Key("q")
		String query;

		@Key("max-results")
		Integer maxResults = 1;

		YouTubeVideoUrl(String encodedUrl) {
			super(encodedUrl);
			this.alt = "jsonc";
			this.prettyprint = true;
			this.key = YouTubeComm.DEVKEY;
		}

		private static YouTubeVideoUrl root() {
			return new YouTubeVideoUrl(ROOT_URL);
		}

		static YouTubeVideoUrl forVideosFeed() {
			YouTubeVideoUrl result = root();
			result.getPathParts().add("videos");
			return result;
		}
	}
	
	public static class YouTubePlaylistUrl extends GoogleUrl {		

		YouTubePlaylistUrl(String encodedUrl) {
			super(encodedUrl);
			this.alt = "jsonc";
			this.prettyprint = true;
			//this.key = YouTubeComm.DEVKEY;
		}

		private static YouTubePlaylistUrl root() {
			return new YouTubePlaylistUrl(ROOT_URL);
		}

		static YouTubePlaylistUrl forPlaylistsFeed(){
			YouTubePlaylistUrl result = root();
			result.getPathParts().add("users");
			result.getPathParts().add("default");
			result.getPathParts().add("playlists");
			return result;
		}
	}	
}
