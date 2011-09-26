package com.android.songseeker.comm;


import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;

import com.android.songseeker.comm.ServiceCommException.ServiceErr;
import com.android.songseeker.comm.ServiceCommException.ServiceID;
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
		Element fstNmElmnt, fstElmnt;
		NodeList fstNm, fstNmElmntLst;
		
		SongInfo song = new SongInfo();;
				
		String urlStr = ENDPOINT + "track/details?";
		String reqParam = "trackid="+trackId+"&oauth_consumer_key="+ CONSUMER_KEY+ "&imageSize=200";
		
		try {
			//result = Util.sendGetRequest(urlStr, reqParam);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(urlStr+reqParam);
			doc.getDocumentElement().normalize();

			//check response
			fstNmElmntLst = doc.getElementsByTagName("response");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){
				throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
			}	
			
			NodeList nodeLst = doc.getElementsByTagName("track");					
			for (int s=0; s<nodeLst.getLength(); s++) {

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
					}else if(fstNmElmnt.getParentNode().getNodeName().equalsIgnoreCase("release")){
						song.release.name = ((Node) fstNm.item(0)).getNodeValue();
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
				
				//get image
				fstNmElmntLst = fstElmnt.getElementsByTagName("image");
				fstNmElmnt = (Element) fstNmElmntLst.item(0);
				fstNm = fstNmElmnt.getChildNodes();
				song.release.image = ((Node) fstNm.item(0)).getNodeValue();
				
				//get release id
				fstNmElmntLst = fstElmnt.getElementsByTagName("release");
				fstNmElmnt = (Element) fstNmElmntLst.item(0);
				song.release.id = fstNmElmnt.getAttribute("id");
				
				//get artist id
				fstNmElmntLst = fstElmnt.getElementsByTagName("artist");
				fstNmElmnt = (Element) fstNmElmntLst.item(0);
				song.artist.id = fstNmElmnt.getAttribute("id");
				
				//get artist name
				fstNmElmntLst = fstElmnt.getElementsByTagName("name");
				fstNmElmnt = (Element) fstNmElmntLst.item(0);
				fstNm = fstNmElmnt.getChildNodes();
				song.artist.name = ((Node) fstNm.item(0)).getNodeValue();
				
				//mount buy urls
				song.release.buyUrl = MOBILE_URL + "releases/" + song.release.id + "?partner=" + PARTNER_ID;
				song.buyUrl = song.release.buyUrl;
				song.artist.buyUrl = MOBILE_URL + "artists/" + song.artist.id + "?partner=" + PARTNER_ID;
			}			
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

        return song;
    }
	
	public ArrayList<SongInfo> queryArtistTopTracks(String artistId) throws ServiceCommException{
		ArrayList<SongInfo> songs = new ArrayList<SongInfo>();
		SongInfo song;
		
		Element fstNmElmnt, fstElmnt;
		NodeList fstNm, fstNmElmntLst;
		
		String urlStr = ENDPOINT + "artist/toptracks?";
		String reqParam = "artistid="+artistId+"&oauth_consumer_key="+ CONSUMER_KEY+ "&pagesize=5&page=1";
		
		try {
			//result = Util.sendGetRequest(urlStr, reqParam);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(urlStr+reqParam);
			doc.getDocumentElement().normalize();

			//check response
			fstNmElmntLst = doc.getElementsByTagName("response");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){
				throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);
			}	
			
			NodeList nodeLst = doc.getElementsByTagName("track");					
			for (int s=0; s<nodeLst.getLength(); s++) {
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
					}else if(fstNmElmnt.getParentNode().getNodeName().equalsIgnoreCase("release")){
						song.release.name = ((Node) fstNm.item(0)).getNodeValue();
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
				
				//get image
				fstNmElmntLst = fstElmnt.getElementsByTagName("image");
				fstNmElmnt = (Element) fstNmElmntLst.item(0);
				fstNm = fstNmElmnt.getChildNodes();
				song.release.image = ((Node) fstNm.item(0)).getNodeValue();
				
				//get release id
				fstNmElmntLst = fstElmnt.getElementsByTagName("release");
				fstNmElmnt = (Element) fstNmElmntLst.item(0);
				song.release.id = fstNmElmnt.getAttribute("id");
				
				//get artist id
				fstNmElmntLst = fstElmnt.getElementsByTagName("artist");
				fstNmElmnt = (Element) fstNmElmntLst.item(0);
				song.artist.id = fstNmElmnt.getAttribute("id");
				
				//get artist name
				fstNmElmntLst = fstElmnt.getElementsByTagName("name");
				fstNmElmnt = (Element) fstNmElmntLst.item(0);
				fstNm = fstNmElmnt.getChildNodes();
				song.artist.name = ((Node) fstNm.item(0)).getNodeValue();
				
				//mount buy urls
				song.release.buyUrl = MOBILE_URL + "releases/" + song.release.id + "?partner=" + PARTNER_ID;
				song.buyUrl = song.release.buyUrl;
				song.artist.buyUrl = MOBILE_URL + "artists/" + song.artist.id + "?partner=" + PARTNER_ID;
				
				songs.add(song);
			}			
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
			//result = Util.sendGetRequest(urlStr, reqParam);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(urlStr+reqParam);
			doc.getDocumentElement().normalize();

			//check response
			fstNmElmntLst = doc.getElementsByTagName("response");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){
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
}

