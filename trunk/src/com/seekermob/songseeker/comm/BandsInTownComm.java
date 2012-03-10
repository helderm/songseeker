package com.seekermob.songseeker.comm;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import android.util.Log;

import com.seekermob.songseeker.comm.ServiceCommException.ServiceErr;
import com.seekermob.songseeker.comm.ServiceCommException.ServiceID;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.EventInfo;
import com.seekermob.songseeker.util.Util;

public class BandsInTownComm {
	private static BandsInTownComm comm = new BandsInTownComm();
	
	private static final String ENDPOINT = "http://api.bandsintown.com/";
	private static final String EVENT_RADIUS = "150";
	
	private BandsInTownComm(){}
	
	public static BandsInTownComm getComm(){
		return comm;
	}
	
 	public ArrayList<EventInfo> getRecommendedEvents(ArrayList<ArtistInfo> artists) throws ServiceCommException{		
 		
 		ArrayList<EventInfo> events = new ArrayList<EventInfo>();
 		
 		try{	
			StringBuilder query = new StringBuilder(ENDPOINT + "events/recommended?location=use_geoip&radius="+EVENT_RADIUS+
													"&format=json&app_id="+Util.APP+"&");
			
			for(ArtistInfo artist : artists){
				query.append("artists[]=" + artist.name.replace(" ", "+") + "&");
			}
			query.deleteCharAt(query.length()-1);
						
			HttpGet request = new HttpGet(query.toString());
	
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(request);

			if(response.getStatusLine().getStatusCode() != 200) {

				Log.w(Util.APP, "HTTP client returned code different from 200! code: "+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
				throw new ServiceCommException(ServiceID.BANDSINTOWN, ServiceErr.REQ_FAILED);
			} 		
 	 		
 			HttpEntity r_entity = response.getEntity();
 			String jsonString = EntityUtils.toString(r_entity);			
 			JSONParser parser = new JSONParser();			
 			JSONArray array = (JSONArray) parser.parse(jsonString);
 			
 			//parse the event
 			for(int i=0; i<array.size(); i++){
 				EventInfo event = new EventInfo();
 				
 				JSONObject eventj = (JSONObject) array.get(i);
 				
 				event.id = Long.toString((Long) eventj.get("id"));
 				event.url = (String) eventj.get("url"); 				
 				event.ticketStatus = (String) eventj.get("ticket_status"); 				
 				event.date = (String) eventj.get("datetime"); 				
 				event.ticketUrl = (String) eventj.get("ticket_url"); 				
 				event.artistName = (String)((JSONObject)((JSONArray) eventj.get("artists")).get(0)).get("name");
 				
 				event.venue.id = Long.toString((Long)((JSONObject)eventj.get("venue")).get("id")); 				
 				event.venue.city = (String)((JSONObject)eventj.get("venue")).get("city");
 				event.venue.country = (String)((JSONObject)eventj.get("venue")).get("country");
 				event.venue.latitude = Double.toString((Double)((JSONObject)eventj.get("venue")).get("latitude"));
 				event.venue.longitude = Double.toString((Double)((JSONObject)eventj.get("venue")).get("longitude"));
 				event.venue.name = (String)((JSONObject)eventj.get("venue")).get("name"); 				
 				event.venue.url = (String)((JSONObject)eventj.get("venue")).get("url"); 				
 				
 				events.add(event);		
 			}

 			return events;

 		} catch(ServiceCommException e){ 			
 			 			
 			throw e;		
 		} catch (IOException ex){
 			throw new ServiceCommException(ServiceID.BANDSINTOWN, ServiceErr.IO);
 		} catch(Exception e){
 			Log.w(Util.APP, e.getMessage(), e);
 			throw new ServiceCommException(ServiceID.BANDSINTOWN, ServiceErr.UNKNOWN);
 		}
 	}
}
