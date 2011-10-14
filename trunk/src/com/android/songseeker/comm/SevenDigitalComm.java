package com.android.songseeker.comm;


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

import android.util.Log;

import com.android.songseeker.comm.ServiceCommException.ServiceErr;
import com.android.songseeker.comm.ServiceCommException.ServiceID;
import com.android.songseeker.data.ArtistInfo;
import com.android.songseeker.data.ReleaseInfo;
import com.android.songseeker.data.SongInfo;
import com.android.songseeker.util.Util;

public class SevenDigitalComm {

	private static SevenDigitalComm comm = new SevenDigitalComm(); 
	private static final String ENDPOINT = "http://api.7digital.com/1.2/";
	private static final String MOBILE_URL = "http://m.7digital.com/";
	private static final String CONSUMER_KEY = "7d9b53mkgqh6";
	private static final String PARTNER_ID = "2539";

	private SevenDigitalComm() {}

	static public SevenDigitalComm getComm(){
		return comm;
	}

	public SongInfo querySongDetails(String trackId) throws ServiceCommException{
		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		SongInfo song;

		String urlStr = ENDPOINT + "track/details?";
		String reqParam = "trackid="+trackId+"&oauth_consumer_key="+ CONSUMER_KEY+ "&imageSize=200";

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
				//TODO if(errorCode == 2001 (track not found)) then query 7digital id with 'search'
				Log.w(Util.APP, "7digital 'track/details' call for track ["+trackId+"] failed with code ["+fstNmElmnt.getAttribute("status")+"]");
				throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
			}	

			song = parseSongDetails(doc.getDocumentElement()).get(0);
			if(song == null)
				throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);

		}catch(IOException e) {
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.IO);	
		}catch(NullPointerException e){
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
		}catch(ServiceCommException e){
			throw e;
		}catch(Exception e){
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}

		return song;
	}

	public ArrayList<SongInfo> queryArtistTopTracks(String artistId) throws ServiceCommException{
		ArrayList<SongInfo> songs;

		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		String urlStr = ENDPOINT + "artist/toptracks?";
		String reqParam = "artistid="+artistId+"&oauth_consumer_key="+ CONSUMER_KEY+ "&pagesize=5&page=1";

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
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}		

		return songs;
	}

	public String getPreviewUrl(String trackId) throws ServiceCommException{
		String previewUrl;

		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		String urlStr = ENDPOINT + "track/preview?";
		String reqParam = "trackid="+trackId+"&oauth_consumer_key="+ CONSUMER_KEY+ "&redirect=false";

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
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}	

		return previewUrl;
	}

	public ReleaseInfo queryReleaseDetails(String releaseId) throws ServiceCommException{
		ReleaseInfo release;		
		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		String urlStr = ENDPOINT + "release/details?";
		String reqParam = "releaseid="+releaseId+"&oauth_consumer_key="+ CONSUMER_KEY+ "&imageSize=200";

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
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}		

		return release;
	}

	public ArrayList<SongInfo> queryReleaseSongList(String releaseId) throws ServiceCommException{
		ArrayList<SongInfo> songs;

		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		String urlStr = ENDPOINT + "release/tracks?";
		String reqParam = "releaseid="+releaseId+"&oauth_consumer_key="+ CONSUMER_KEY+ "&pagesize=50&page=1";

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

			//songs = parseSongDetails(doc, true);
			songs = parseSongDetails(doc.getDocumentElement());

		}catch(IOException e) {
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.IO);
		}catch(NullPointerException e){
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
		}catch(ServiceCommException e){
			throw e;
		}catch(Exception e){
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}		

		return songs;		
	}

	public ArrayList<ReleaseInfo> getArtistReleases(String artistId) throws ServiceCommException{
		ArrayList<ReleaseInfo> releases;

		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		String urlStr = ENDPOINT + "artist/releases?";
		String reqParam = "artistid="+artistId+"&oauth_consumer_key="+ CONSUMER_KEY+ "&imageSize=200";

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
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}		

		return releases;
	}

	public ArtistInfo queryArtistDetails(String artistId) throws ServiceCommException{
		ArtistInfo artist;		
		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		String urlStr = ENDPOINT + "artist/details?";
		String reqParam = "artistid="+artistId+"&oauth_consumer_key="+ CONSUMER_KEY+ "&imageSize=200";

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

			artist = parseArtistDetails(doc.getDocumentElement());			

		}catch(IOException e) {
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.IO);	
		}catch(NullPointerException e){
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
		}catch(ServiceCommException e){
			throw e;
		}catch(Exception e){
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}		

		return artist;
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
			song.artist = parseArtistDetails(fstElmnt);

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

		//NodeList nodeLst = doc.getElementsByTagName("release");					

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
			//release.artist = parseReleaseArtistDetails(doc);
			release.artist = parseArtistDetails(fstElmnt);

			//mount buy url
			release.buyUrl = MOBILE_URL + "releases/" + release.id + "?partner=" + PARTNER_ID;		
			
			releases.add(release);
		}

		return releases;
	}	
	
	private ArtistInfo parseArtistDetails(Element element) throws Exception{
		ArtistInfo artist = new ArtistInfo();				
		Element fstNmElmnt, fstElmnt;
		NodeList fstNm, fstNmElmntLst;

		NodeList nodeLst = element.getElementsByTagName("artist");					

		if(nodeLst.getLength() < 1)
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);

		Node fstNode = nodeLst.item(0);
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
		return artist;
	}

}

