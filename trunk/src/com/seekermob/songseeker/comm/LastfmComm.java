package com.seekermob.songseeker.comm;

import java.util.Collection;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceID;
import com.seekermob.songseeker.util.Util;

import de.umass.lastfm.Artist;
import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Playlist;
import de.umass.lastfm.Result;
import de.umass.lastfm.Session;
import de.umass.lastfm.User;

public class LastfmComm {
	private static LastfmComm comm = new LastfmComm();
	private static Session session = null;
	private static String sessionKey;
	private static String username;
	
	private static final String KEY = "4c2fe8fbeda6545125c50798c20bc9c0";
	private static final String SECRET = "f7d0e539a08eb21e07a9d26fa66926a8";
	private static final String PREF_SESSIONKEY = "prefs.lastfm.sessionkey";
	private static final String PREF_USERNAME = "pref.lastfm.username";
	
	//private static final String ENDPOINT = "http://ws.audioscrobbler.com/2.0/?";
	
	private LastfmComm(){}
	
	public static LastfmComm getComm(SharedPreferences settings){
		
		if(sessionKey != null)
			return comm;
		
		sessionKey = settings.getString(PREF_SESSIONKEY, null);
		username = settings.getString(PREF_USERNAME, null);		
		
		return comm;
	}
	
	public static LastfmComm getComm(){
		Caller.getInstance().setCache(null);
		return comm;
	}
	
	public void requestAuthorize(String user, String pwd, SharedPreferences settings) throws ServiceCommException{
		
		try{
			session = Authenticator.getMobileSession(user, pwd, KEY, SECRET);			
		}catch(RuntimeException e){
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);			
		}
		
		if(session == null)
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.NOT_AUTH);
		
		Editor editor = settings.edit();
		editor.putString(PREF_SESSIONKEY, session.getKey());
		editor.putString(PREF_USERNAME, session.getUsername());
		editor.commit();		
	}
	
	/*public void rrequestAuthorize(String user, String pwd, SharedPreferences settings) throws ServiceCommException{
		//ArrayList<String> topArtists = new ArrayList<String>();
		Element fstNmElmnt;
		NodeList fstNmElmntLst;
		
		//get authToken and signature
		String authToken = md5(user.toLowerCase() + pwd);
		Map<String, String> params = map("api_key", KEY, "username", user.toLowerCase(), "authToken", authToken);
		String sig = createSignature("auth.getMobileSession", params, SECRET);		
		
		String urlStr = ENDPOINT + "method=auth.getMobileSession&";		
		String reqParam = "username="+user.toLowerCase()+ "&authToken="+ authToken+
							"&api_key="+ KEY+ "&api_sig="+ sig;
		
		try {
			URL url = new URL(urlStr+reqParam);			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(url.openStream()));
			doc.getDocumentElement().normalize();

			//check response
			fstNmElmntLst = doc.getElementsByTagName("lfm");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){				
				parseError(fstNmElmnt);
			}	

			//parse artists from response
			/*NodeList nodeLst = doc.getDocumentElement().getElementsByTagName("artist");
			for (int s=0; s<nodeLst.getLength(); s++) {
				
				Node fstNode = nodeLst.item(s);

				if (fstNode.getNodeType() != Node.ELEMENT_NODE) 
					continue;

				Element fstElmnt = (Element) fstNode;
				
				fstNmElmntLst = fstElmnt.getElementsByTagName("name");
				fstNmElmnt = (Element) fstNmElmntLst.item(0);
				NodeList fstNm = fstNmElmnt.getChildNodes();
				String artistName = ((Node) fstNm.item(0)).getNodeValue();
				
				topArtists.add(artistName);
			}

		}catch(IOException e) {
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);	
		}catch(NullPointerException e){
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.REQ_FAILED);
		}catch(ServiceCommException e){
			throw e;
		}catch(Exception e){
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.UNKNOWN);	
		}		
	}*/
	
	public Collection<Playlist> getUserPlaylists() throws ServiceCommException{
		if(session == null)
			return null;
		
		Collection<Playlist> pls;
		
		Log.i(Util.APP, "Retrieving the user playlists on Last.fm...");
		
		try{
			pls = User.getPlaylists(session.getUsername(), KEY);
		}catch(RuntimeException e){
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);			
		}
		
		Log.i(Util.APP, "Playlists fetched!");
		
		return pls;
	}
	
	public Playlist createPlaylist(String title, SharedPreferences settings) throws ServiceCommException{
		if(session == null)
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.NOT_AUTH);
		
		Playlist pl = null;

		Log.i(Util.APP, "Creating playlist on Last.fm...");
		
		try{
			pl = Playlist.create(title, "", session);
		}catch(RuntimeException e){
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);			
		}	
		
		if(pl == null){
			Log.e(Util.APP, "Error while trying to create playlist on Last.fm!");
			cleanAuth(settings);
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.REQ_FAILED);
		}
		
		Log.i(Util.APP, "Playlist created on Last.fm!");
		
		return pl;	
	}
	
	public void addToPlaylist(int playlistId, String artist, String track, SharedPreferences settings) throws ServiceCommException{
		if(session == null)
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.NOT_AUTH);
		
		Result res = null;
		try{
			res = Playlist.addTrack(playlistId, artist, track, session);
		}catch(RuntimeException e){
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);			
		}			
		
		Log.i(Util.APP, "Adding track ["+track+" - "+artist+"] to Last.fm playlist...");
		if(!res.isSuccessful()){
			switch (res.getErrorCode()) {
			case 4:
			case 9:
			case 14:
				cleanAuth(settings);
				throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.NOT_AUTH);
			case 6:
				Log.w(Util.APP, "Song ["+track+" - "+artist+"] not found on Last.fm!");
				return;
			case 8:
			case 16:
			case 29:
				throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.TRY_LATER);

			default:
				Log.e(Util.APP,"Unknown error on Last.fm Playlist.getTrack()! Err=[("+res.getErrorCode()+") - "+res.getErrorMessage() +"]");
				throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.UNKNOWN);
			}
		}
		
		Log.i(Util.APP, "Track added to Last.fm playlist!!");
	}
	
	public Collection<Artist> getTopArtists(String user) throws ServiceCommException{
		try{
			Collection<Artist> topArtists = User.getTopArtists(user, KEY);
			
			if(topArtists.isEmpty())
				throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.USER_NOT_FOUND);
			
			return topArtists;
		}catch(RuntimeException e){
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);			
		}	
	}
	
	/*public ArrayList<String> getTopArtists(String user) throws ServiceCommException{
		ArrayList<String> topArtists = new ArrayList<String>();
		Element fstNmElmnt;
		NodeList fstNmElmntLst;
		
		String urlStr = ENDPOINT + "method=user.gettopartists&";
		String reqParam = "user="+user+"&limit=30"+"&page=1"+"&api_key="+ KEY;
		
		try {
			URL url = new URL(urlStr+reqParam);			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(url.openStream()));
			doc.getDocumentElement().normalize();

			//check response
			fstNmElmntLst = doc.getElementsByTagName("lfm");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			if(!fstNmElmnt.getAttribute("status").equalsIgnoreCase("ok")){
				
				try{
					parseError(fstNmElmnt);
				}catch(ServiceCommException e){
					throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.SONG_NOT_FOUND);
				}
			}	

			//parse artists from response
			NodeList nodeLst = doc.getDocumentElement().getElementsByTagName("artist");
			for (int s=0; s<nodeLst.getLength(); s++) {
				
				Node fstNode = nodeLst.item(s);

				if (fstNode.getNodeType() != Node.ELEMENT_NODE) 
					continue;

				Element fstElmnt = (Element) fstNode;
				
				fstNmElmntLst = fstElmnt.getElementsByTagName("name");
				fstNmElmnt = (Element) fstNmElmntLst.item(0);
				NodeList fstNm = fstNmElmnt.getChildNodes();
				String artistName = ((Node) fstNm.item(0)).getNodeValue();
				
				topArtists.add(artistName);
			}

		}catch(IOException e) {
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.IO);	
		}catch(NullPointerException e){
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.REQ_FAILED);
		}catch(ServiceCommException e){
			throw e;
		}catch(Exception e){
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.UNKNOWN);	
		}
		
		return topArtists;
	}*/
	
	public boolean isAuthorized(){
		if(session == null){
			if(sessionKey == null || username == null)
				return false;
			session = Session.createSession(KEY, SECRET, sessionKey, username, false);
		}
		
		return true;	
	}
	
	public boolean hasUsername(){
		if(session == null && username == null)
			return false;
		
		return true;
	}
	
	public void cleanAuth(SharedPreferences settings){
		Editor editor = settings.edit();
		editor.putString(PREF_SESSIONKEY, null);
		editor.putString(PREF_USERNAME, null);
		editor.commit();
		
		session = null;
		sessionKey = null;
		username = null;
	}
	
	/*private void parseError(Element element) throws ServiceCommException, Exception{
		Element fstNmElmnt;
		NodeList fstNmElmntLst;

		//check response
		fstNmElmntLst = element.getElementsByTagName("error");
		fstNmElmnt = (Element) fstNmElmntLst.item(0);

		int code = Integer.parseInt(fstNmElmnt.getAttribute("code"));
		
		switch(code){
		case 6:
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.ID_NOT_FOUND);
		case 9:
		case 14:
		case 17:	
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.NOT_AUTH);
		case 8:
		case 16:
		case 29:
			throw new ServiceCommException(ServiceID.LASTFM, ServiceErr.TRY_LATER);
			
		default:
			Log.d(Util.APP, "Last.fm ws call failed with code ["+code+"]");
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.REQ_FAILED);			
		}
	}
	
	private static String md5(String s) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e1) {
			return null;
		}
		
		try {
			digest = MessageDigest.getInstance("MD5");
			byte[] bytes = digest.digest(s.getBytes("UTF-8"));
			StringBuilder b = new StringBuilder(32);
			for (byte aByte : bytes) {
				String hex = Integer.toHexString((int) aByte & 0xFF);
				if (hex.length() == 1)
					b.append('0');
				b.append(hex);
			}
			return b.toString();
		} catch (UnsupportedEncodingException e) {
		} catch (NoSuchAlgorithmException e) {	}
		
		return null;
	}
	
	private static String createSignature(String method, Map<String, String> params, String secret) {
		params = new TreeMap<String, String>(params);
		params.put("method", method);
		StringBuilder b = new StringBuilder(100);
		for (Entry<String, String> entry : params.entrySet()) {
			b.append(entry.getKey());
			b.append(entry.getValue());
		}
		b.append(secret);
		return md5(b.toString());
	}
	
	private static Map<String, String> map(String... strings) {
		if (strings.length % 2 != 0)
			throw new IllegalArgumentException("strings.length % 2 != 0");
		Map<String, String> mp = new HashMap<String, String>();
		for (int i = 0; i < strings.length; i += 2) {
			mp.put(strings[i], strings[i + 1]);
		}
		return mp;
	}*/
}
