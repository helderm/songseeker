package com.android.songseeker.comm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import android.util.Log;

import com.android.songseeker.comm.ServiceCommException.ServiceErr;
import com.android.songseeker.comm.ServiceCommException.ServiceID;
import com.android.songseeker.util.Util;

public class SevenDigitalComm {
	
	private static SevenDigitalComm comm = new SevenDigitalComm(); 
	private static final String ENDPOINT = "http://api.7digital.com/1.2/";
	private static final String CONSUMER_KEY = "7d9b53mkgqh6";
	
	private SevenDigitalComm() {}
	
	static public SevenDigitalComm getComm(){
		return comm;
	}
	
	public String queryTrackDetails(String trackId) throws ServiceCommException{
		String result = null;
		String[] parts = trackId.split(":");
		String id = parts[2];
		
		String urlStr = ENDPOINT + "track/details?";
		
		urlStr += "trackid="+id+"&oauth_consumer_key="+CONSUMER_KEY;		
		
		URL url;
		try {
			url = new URL(urlStr);
			URLConnection conn = url.openConnection ();
			
			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = rd.readLine()) != null)
			{
				sb.append(line);
			}
			rd.close();
			result = sb.toString();
		} catch (IOException e) {
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.IO);		
		} catch (Exception e){
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.SEVENDIGITAL, ServiceErr.UNKNOWN);	
		}

        return result;
    }
	
	
	
}
