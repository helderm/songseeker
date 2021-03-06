package com.seekermob.songseeker.comm;


import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceID;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.ReleaseInfo;
import com.seekermob.songseeker.data.SongInfo;
import com.seekermob.songseeker.util.Util;

public class SevenDigitalComm {

	private static SevenDigitalComm comm = new SevenDigitalComm(); 
	private static final String ENDPOINT = "http://api.7digital.com/1.2/";
	private static final String MOBILE_URL = "http://m.7digital.com/";
	private static String consumer_key = null;
	private static final String PARTNER_ID = "2539";
	private static final String IMAGE_SIZE = "200";

	private SevenDigitalComm() {}

	public static void initialize(Context c){
		consumer_key = c.getString(R.string.sevendigital_key);
	}
	
	static public SevenDigitalComm getComm(){
		return comm;
	}	

	public SongInfo querySongDetails(String trackId, String trackName, String artistName) throws ServiceCommException{
		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		SongInfo song;

		String urlStr = ENDPOINT + "track/details?";
		String reqParam = "trackid="+trackId+"&oauth_consumer_key="+ consumer_key+ "&imageSize="+IMAGE_SIZE;

		try {
			URL url = new URL(urlStr+reqParam);			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(url.openStream()));
			doc.getDocumentElement().normalize();

			//check response
			fstNmElmntLst = doc.getElementsByTagName("response");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){
				
				try{
					parseError(fstNmElmnt);
				}catch(ServiceCommException e){
					if(e.getErr() == ServiceErr.SONG_NOT_FOUND){
						//call ws using song and artist name
						song = querySongSearch(trackName, artistName);
						return song;
					}
				}
			}	

			song = parseSongDetails(doc.getDocumentElement()).get(0);
			if(song == null)
				throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);

		}catch(IOException e) {
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.IO);	
		}catch(NullPointerException e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
		}catch(ServiceCommException e){
			throw e;
		}catch(Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}

		return song;
	}
	
	public SongInfo querySongSearch(String trackName, String artistName) throws ServiceCommException{
		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		ArrayList<SongInfo> songs;

		String urlStr = ENDPOINT + "track/search?";
		String reqParam = "q="+ Uri.encode(trackName)+ "&oauth_consumer_key="+ consumer_key+ "&pagesize=30&page=1&imageSize="+IMAGE_SIZE;

		try {
			URL url = new URL(urlStr+reqParam);			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(url.openStream()));
			doc.getDocumentElement().normalize();

			//check response
			fstNmElmntLst = doc.getElementsByTagName("response");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){
				parseError(fstNmElmnt);
			}	

			songs = parseSongDetails(doc.getDocumentElement());
			if(songs == null)
				throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.SONG_NOT_FOUND);
			
			//search for the track with the same artist
			for(SongInfo song : songs){				
 				
 				if(artistName.equalsIgnoreCase(song.artist.name) || song.artist.name.toLowerCase().contains(artistName.toLowerCase()) 
 						|| artistName.toLowerCase().contains(song.artist.name.toLowerCase())){
 					
 					return song;
 				} 	
			}
			
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.SONG_NOT_FOUND);				

		}catch(IOException e) {
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.IO);	
		}catch(NullPointerException e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
		}catch(ServiceCommException e){
			throw e;
		}catch(Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}
	}
	
	public ArtistInfo queryArtistSearch(String artistName) throws ServiceCommException{
		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		ArrayList<ArtistInfo> artists;

		String urlStr = ENDPOINT + "artist/search?";
		String reqParam = "q=" + Uri.encode(artistName) + "&oauth_consumer_key="+ consumer_key+ "&pagesize=15&page=1&imageSize="+IMAGE_SIZE;

		try {
			URL url = new URL(urlStr+reqParam);			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(url.openStream()));
			doc.getDocumentElement().normalize();

			//check response
			fstNmElmntLst = doc.getElementsByTagName("response");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){
				parseError(fstNmElmnt);
			}	

			artists = parseArtistDetails(doc.getDocumentElement());
			if(artists == null)
				throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.ARTIST_NOT_FOUND);
			
			//search for the track with the same artist
			for(ArtistInfo artist : artists){				
 				
 				if(artistName.equalsIgnoreCase(artist.name) || artist.name.toLowerCase().contains(artistName.toLowerCase()) 
 						|| artistName.toLowerCase().contains(artist.name.toLowerCase())){
 					
 					return artist;
 				} 	
			}
			
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.ARTIST_NOT_FOUND);				

		}catch(IOException e) {
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.IO);	
		}catch(NullPointerException e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
		}catch(ServiceCommException e){
			throw e;
		}catch(Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}
	}	

	public ArrayList<SongInfo> queryArtistTopTracks(String artistId) throws ServiceCommException{
		ArrayList<SongInfo> songs;

		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		String urlStr = ENDPOINT + "artist/toptracks?";
		String reqParam = "artistid="+artistId+"&oauth_consumer_key="+ consumer_key+ "&pagesize=10&page=1&imageSize="+IMAGE_SIZE;

		try {
			URL url = new URL(urlStr+reqParam);			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(url.openStream()));
			doc.getDocumentElement().normalize();

			//check response
			fstNmElmntLst = doc.getElementsByTagName("response");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){
				Log.w(Util.APP, "7digital 'artist/toptracks' call failed with code ["+fstNmElmnt.getAttribute("status")+"]");
				throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
			}	

			songs = parseSongDetails(doc.getDocumentElement());

		}catch(IOException e) {
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.IO);
		}catch(NullPointerException e){
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
		}catch(ServiceCommException e){
			throw e;
		}catch(Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}		

		return songs;
	}

	public String getPreviewUrl(String trackId) throws ServiceCommException{
		String previewUrl;

		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		String urlStr = ENDPOINT + "track/preview?";
		String reqParam = "trackid="+trackId+"&oauth_consumer_key="+ consumer_key+ "&redirect=false";

		try {
			URL url = new URL(urlStr+reqParam);			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(url.openStream()));
			doc.getDocumentElement().normalize();

			//check response
			fstNmElmntLst = doc.getElementsByTagName("response");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){
				Log.w(Util.APP, "7digital 'track/preview' call failed with code ["+fstNmElmnt.getAttribute("status")+"]");
				throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
			}				

			fstNmElmntLst = doc.getElementsByTagName("url");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			NodeList fstNm = fstNmElmnt.getChildNodes();
			previewUrl = ((Node) fstNm.item(0)).getNodeValue();

		}catch(IOException e) {
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.IO);
		}catch(NullPointerException e){
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
		}catch(ServiceCommException e){
			throw e;
		}catch(Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}	

		return previewUrl;
	}

	public ReleaseInfo queryReleaseDetails(String releaseId) throws ServiceCommException{
		ReleaseInfo release;		
		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		String urlStr = ENDPOINT + "release/details?";
		String reqParam = "releaseid="+releaseId+"&oauth_consumer_key="+ consumer_key+ "&imageSize="+IMAGE_SIZE;

		try {
			URL url = new URL(urlStr+reqParam);			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(url.openStream()));
			doc.getDocumentElement().normalize();

			//check response
			fstNmElmntLst = doc.getElementsByTagName("response");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){
				Log.w(Util.APP, "7digital 'release/details' call failed with code ["+fstNmElmnt.getAttribute("status")+"]");
				throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
			}	

			release = parseReleaseDetails(doc.getDocumentElement()).get(0);			

		}catch(IOException e) {
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.IO);	
		}catch(NullPointerException e){
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
		}catch(ServiceCommException e){
			throw e;
		}catch(Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}		

		return release;
	}

	public ArrayList<SongInfo> queryReleaseSongList(String releaseId) throws ServiceCommException{
		ArrayList<SongInfo> songs;

		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		String urlStr = ENDPOINT + "release/tracks?";
		String reqParam = "releaseid="+releaseId+"&oauth_consumer_key="+ consumer_key+ "&pagesize=50&page=1&imageSize="+IMAGE_SIZE;

		try {
			URL url = new URL(urlStr+reqParam);			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(url.openStream()));
			doc.getDocumentElement().normalize();

			//check response
			fstNmElmntLst = doc.getElementsByTagName("response");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){
				Log.w(Util.APP, "7digital 'release/tracks' call failed with code ["+fstNmElmnt.getAttribute("status")+"]");
				throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
			}	

			songs = parseSongDetails(doc.getDocumentElement());

		}catch(IOException e) {
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.IO);
		}catch(NullPointerException e){
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
		}catch(ServiceCommException e){
			throw e;
		}catch(Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}		

		return songs;		
	}

	public ArrayList<ReleaseInfo> getArtistReleases(String artistId) throws ServiceCommException{
		ArrayList<ReleaseInfo> releases;

		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		String urlStr = ENDPOINT + "artist/releases?";
		String reqParam = "artistid="+artistId+"&oauth_consumer_key="+ consumer_key+ "&imageSize="+IMAGE_SIZE;

		try {
			URL url = new URL(urlStr+reqParam);			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(url.openStream()));
			doc.getDocumentElement().normalize();

			//check response
			fstNmElmntLst = doc.getElementsByTagName("response");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){
				Log.w(Util.APP, "7digital 'artist/releases' call failed with code ["+fstNmElmnt.getAttribute("status")+"]");
				throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
			}	

			releases = parseReleaseDetails(doc.getDocumentElement());			

		}catch(IOException e) {
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.IO);	
		}catch(NullPointerException e){
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
		}catch(ServiceCommException e){
			throw e;
		}catch(Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}		

		return releases;
	}

	public ArtistInfo queryArtistDetails(String artistId) throws ServiceCommException{
		ArtistInfo artist;		
		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		String urlStr = ENDPOINT + "artist/details?";
		String reqParam = "artistid="+artistId+"&oauth_consumer_key="+ consumer_key+ "&imageSize="+IMAGE_SIZE;

		try {
			
			URL url = new URL(urlStr+reqParam);			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			Document doc = db.parse(new InputSource(url.openStream()));
			doc.getDocumentElement().normalize();

			//check response
			fstNmElmntLst = doc.getElementsByTagName("response");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){
				Log.w(Util.APP, "7digital 'artist/details' call failed with code ["+fstNmElmnt.getAttribute("status")+"]");
				throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
			}	

			artist = parseArtistDetails(doc.getDocumentElement()).get(0);		
			
			if(artist == null)
				throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);

		}catch(IOException e) {
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.IO);	
		}catch(NullPointerException e){
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
		}catch(ServiceCommException e){
			throw e;
		}catch(Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}		

		return artist;
	}
	
	public ArrayList<ReleaseInfo> queryEditorialList() throws ServiceCommException{
		ArrayList<ReleaseInfo> queriedReleases = new ArrayList<ReleaseInfo>();
		ArrayList<ReleaseInfo> releases = new ArrayList<ReleaseInfo>();
		
		final int MIN_EDITORIAL_RESULTS = 16;
		
		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		try {
			
			String urlStr = ENDPOINT + "editorial/list?";
			String reqParam = "key=featured_albums" + "&oauth_consumer_key="+ consumer_key+ "&imageSize="+IMAGE_SIZE;
			
			URL url = new URL(urlStr+reqParam);			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(url.openStream()));
			doc.getDocumentElement().normalize();

			//check response
			fstNmElmntLst = doc.getElementsByTagName("response");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){
				parseError(fstNmElmnt);
			}	

			queriedReleases = parseReleaseDetails(doc.getDocumentElement());	
			releases.addAll(queriedReleases);			
			queriedReleases.clear();
			
			if(releases.size() >= MIN_EDITORIAL_RESULTS)
				return releases;
			
			reqParam = "key=new_releases" + "&oauth_consumer_key="+ consumer_key+ "&imageSize="+IMAGE_SIZE;
			
			url = new URL(urlStr+reqParam);			
			db = dbf.newDocumentBuilder();
			doc = db.parse(new InputSource(url.openStream()));
			doc.getDocumentElement().normalize();
			
			//check response
			fstNmElmntLst = doc.getElementsByTagName("response");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){
				parseError(fstNmElmnt);
			}	

			queriedReleases = parseReleaseDetails(doc.getDocumentElement());
			releases.addAll(queriedReleases);
			queriedReleases.clear();
			
			if(releases.size() >= MIN_EDITORIAL_RESULTS)
				return releases;
			
			reqParam = "key=staff_recommendations" + "&oauth_consumer_key="+ consumer_key+ "&imageSize="+IMAGE_SIZE;
			
			url = new URL(urlStr+reqParam);			
			db = dbf.newDocumentBuilder();
			doc = db.parse(new InputSource(url.openStream()));
			doc.getDocumentElement().normalize();
			
			queriedReleases = parseReleaseDetails(doc.getDocumentElement());
			releases.addAll(queriedReleases);

		}catch(IOException e) {
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.IO);	
		}catch(NullPointerException e){
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
		}catch(ServiceCommException e){
			throw e;
		}catch(Exception e){
			Log.w(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}	
		
		return releases;
	}
	
	private ArrayList<SongInfo> parseSongDetails(Element element) throws Exception{
		ArrayList<SongInfo> songs = new ArrayList<SongInfo>();
		SongInfo song;

		Element fstNmElmnt, fstElmnt;
		NodeList fstNm, fstNmElmntLst;

		NodeList nodeLst = element.getElementsByTagName("track");					
		//if(nodeLst.getLength() < 1)
		//	throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);

		for (int s=0; s<nodeLst.getLength(); s++) {
			if(Thread.interrupted())
				return songs;
			
			song = new SongInfo();

			Node fstNode = nodeLst.item(s);

			if (fstNode.getNodeType() != Node.ELEMENT_NODE) 
				continue;

			fstElmnt = (Element) fstNode;

			//get id
			song.id = fstElmnt.getAttribute("id");

			//get title
			fstNmElmntLst = fstElmnt.getElementsByTagName("title");				
			for(int j=0; j<fstNmElmntLst.getLength(); j++){
				fstNmElmnt = (Element) fstNmElmntLst.item(j);
				fstNm = fstNmElmnt.getChildNodes();

				if(fstNmElmnt.getParentNode().getNodeName().equalsIgnoreCase("track")){
					song.name = ((Node) fstNm.item(0)).getNodeValue();
					break;
				}
			}

			//get trackNumber
			fstNmElmntLst = fstElmnt.getElementsByTagName("trackNumber");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			fstNm = fstNmElmnt.getChildNodes();
			song.trackNum = ((Node) fstNm.item(0)).getNodeValue();

			//get duration
			fstNmElmntLst = fstElmnt.getElementsByTagName("duration");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			fstNm = fstNmElmnt.getChildNodes();
			song.duration = ((Node) fstNm.item(0)).getNodeValue();

			//get version
			fstNmElmntLst = fstElmnt.getElementsByTagName("version");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			fstNm = fstNmElmnt.getChildNodes();			
			if(fstNm.item(0) != null)
				song.version = ((Node) fstNm.item(0)).getNodeValue();

			//get artist
			song.artist = parseArtistDetails(fstElmnt).get(0);

			//get release details
			song.release = parseReleaseDetails(fstElmnt).get(0);

			//mount buy urls
			song.buyUrl = song.release.buyUrl;

			songs.add(song);
		}

		return songs;
	}	
	
	private ArrayList<ReleaseInfo> parseReleaseDetails(Element element) throws Exception{
		ArrayList<ReleaseInfo> releases = new ArrayList<ReleaseInfo>();
		ReleaseInfo release;
		Element fstNmElmnt, fstElmnt;
		NodeList fstNm, fstNmElmntLst;

		NodeList nodeLst = element.getElementsByTagName("release");				
		
		//if(nodeLst.getLength() < 1)
		//	throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);

		for (int s=0; s<nodeLst.getLength(); s++) {
			release = new ReleaseInfo();

			Node fstNode = nodeLst.item(s);

			if (fstNode.getNodeType() != Node.ELEMENT_NODE) 
				continue;

			fstElmnt = (Element) fstNode;

			//get id
			release.id = fstElmnt.getAttribute("id");

			//get title
			fstNmElmntLst = fstElmnt.getElementsByTagName("title");				
			for(int j=0; j<fstNmElmntLst.getLength(); j++){
				fstNmElmnt = (Element) fstNmElmntLst.item(j);
				fstNm = fstNmElmnt.getChildNodes();

				if(fstNmElmnt.getParentNode().getNodeName().equalsIgnoreCase("release")){
					release.name = ((Node) fstNm.item(0)).getNodeValue();
					break;
				}	
			}

			//get image
			fstNmElmntLst = fstElmnt.getElementsByTagName("image");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			fstNm = fstNmElmnt.getChildNodes();
			release.image = ((Node) fstNm.item(0)).getNodeValue();

			//get artist details
			release.artist = parseArtistDetails(fstElmnt).get(0);

			//mount buy url
			release.buyUrl = MOBILE_URL + "releases/" + release.id + "?partner=" + PARTNER_ID;		
			
			releases.add(release);
		}

		return releases;
	}	
	
	private ArrayList<ArtistInfo> parseArtistDetails(Element element) throws Exception{
		ArrayList<ArtistInfo> artists = new ArrayList<ArtistInfo>();				
		ArtistInfo artist;
		
		Element fstNmElmnt, fstElmnt;
		NodeList fstNm, fstNmElmntLst;

		NodeList nodeLst = element.getElementsByTagName("artist");					

		//if(nodeLst.getLength() < 1)
		//	throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);

		for (int s=0; s<nodeLst.getLength(); s++) {
			artist = new ArtistInfo();
			
			Node fstNode = nodeLst.item(s);

			if (fstNode.getNodeType() != Node.ELEMENT_NODE) 
				continue;

			fstElmnt = (Element) fstNode;
			
			//get id
			artist.id = fstElmnt.getAttribute("id");

			//get artist name
			fstNmElmntLst = fstElmnt.getElementsByTagName("name");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			fstNm = fstNmElmnt.getChildNodes();
			artist.name = ((Node) fstNm.item(0)).getNodeValue();
			
			//get image
			fstNmElmntLst = fstElmnt.getElementsByTagName("image");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(fstNmElmnt != null){	
				fstNm = fstNmElmnt.getChildNodes();			
				if(fstNm.item(0) != null)
					artist.image = ((Node) fstNm.item(0)).getNodeValue();
			}

			//mount buy url
			artist.buyUrl = MOBILE_URL + "artists/" + artist.id + "?partner=" + PARTNER_ID;
			
			artists.add(artist);
		}

		return artists;
	}
	
	private void parseError(Element element) throws ServiceCommException, Exception{
		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		//check response
		fstNmElmntLst = element.getElementsByTagName("error");
		fstNmElmnt = (Element) fstNmElmntLst.item(0);

		int code = Integer.parseInt(fstNmElmnt.getAttribute("code"));
		
		switch(code){
		case 2001:
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.SONG_NOT_FOUND);
		default:
			Log.d(Util.APP, "7digital ws call failed with code ["+code+"]");
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);			
		}
	}

}

