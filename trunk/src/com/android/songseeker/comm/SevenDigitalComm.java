package com.android.songseeker.comm;


import java.io.IOException;

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
	private static final String CONSUMER_KEY = "7d9b53mkgqh6";
	
	private SevenDigitalComm() {}
	
	static public SevenDigitalComm getComm(){
		return comm;
	}
	
	public SongInfo querySongDetails(String trackId) throws ServiceCommException{
		Element fstNmElmnt;
		NodeList fstNm;
		
		SongInfo song = new SongInfo();;
		String id = trackId.split(":")[2];
		
		String urlStr = ENDPOINT + "track/details?";
		String reqParam = "trackid="+id+"&oauth_consumer_key="+ CONSUMER_KEY+ "&imageSize=350";
		
		try {
			//result = Util.sendGetRequest(urlStr, reqParam);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(urlStr+reqParam);
			doc.getDocumentElement().normalize();
			NodeList nodeLst = doc.getElementsByTagName("track");
					
			for (int s=0; s<nodeLst.getLength(); s++) {

				Node fstNode = nodeLst.item(s);

				if (fstNode.getNodeType() != Node.ELEMENT_NODE) 
					continue;
				
				//get title
				Element fstElmnt = (Element) fstNode;
				NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("title");				
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
				
				//get url
				fstNmElmntLst = fstElmnt.getElementsByTagName("url");				
				for(int j=0; j<fstNmElmntLst.getLength(); j++){
					fstNmElmnt = (Element) fstNmElmntLst.item(j);
					fstNm = fstNmElmnt.getChildNodes();
					
					if(fstNmElmnt.getParentNode().getNodeName().equalsIgnoreCase("artist")){
						song.artist.buyUrl = ((Node) fstNm.item(0)).getNodeValue();
					}else if(fstNmElmnt.getParentNode().getNodeName().equalsIgnoreCase("release")){
						song.release.buyUrl = ((Node) fstNm.item(0)).getNodeValue();
					}else if(fstNmElmnt.getParentNode().getNodeName().equalsIgnoreCase("track")){
						song.buyUrl = ((Node) fstNm.item(0)).getNodeValue();
					}
				}
				
				//get image
				fstNmElmntLst = fstElmnt.getElementsByTagName("image");
				fstNmElmnt = (Element) fstNmElmntLst.item(0);
				fstNm = fstNmElmnt.getChildNodes();
				song.release.image = ((Node) fstNm.item(0)).getNodeValue();
			}			
		}catch(IOException e) {
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.IO);		
		}catch(Exception e){
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}

        return song;
    }
	
	
	
}
