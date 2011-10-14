package com.android.songseeker.comm;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONValue;

import com.android.songseeker.comm.ServiceCommException.ServiceErr;
import com.android.songseeker.comm.ServiceCommException.ServiceID;
import com.android.songseeker.util.Util;

import android.util.Log;

public class GroovesharkComm {
	private static GroovesharkComm comm = new GroovesharkComm();

	private static final String KEY = "heldermartins";
	private static final String SECRET = "b0518075945c057909da9829fe1639df";
	private static final String ENDPOINT = "https://api.grooveshark.com/ws/3.0/?sig=";
	private static final String CRYPT_ALG = "HmacMD5";
	
	private static String session = null;

	public static GroovesharkComm getComm(){
		return comm;
	}

	public void authorizeUser(String username, String password) throws ServiceCommException{
		if(session == null)
			startSession();
	}

	private void startSession() throws ServiceCommException{		
		HttpResponse response;
		
		try{
	
			LinkedHashMap<String, Object> args = new LinkedHashMap<String, Object>();
			args.put("method", "startSession");
			
			LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();
			header.put("wsKey", KEY);
			
			args.put("header", header);			
			args.put("parameters", "");
			
			String signature = getSignature(args);			
			
			HttpPost request = new HttpPost(ENDPOINT+signature);
			StringEntity body = new StringEntity(JSONValue.toJSONString(args));
			
			request.setEntity(body);			
			HttpClient httpClient = new DefaultHttpClient();
			response = httpClient.execute(request);

		} catch(ServiceCommException e){
			throw e;		
		} catch (IOException ex){
			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.IO);
		}catch(Exception e){
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.UNKNOWN);
		}
		
		if (response.getStatusLine().getStatusCode() != 200){
			Log.e(Util.APP, "HTTP client returned code different from 200! code: "+response.getStatusLine().getStatusCode()+" - "+response.getStatusLine().getReasonPhrase());
		} 
		
	}
	
	private String getSignature(LinkedHashMap<String, Object> args) throws ServiceCommException{
		byte[] keyBytes = SECRET.getBytes();
		Key key = new SecretKeySpec(keyBytes, 0, keyBytes.length, CRYPT_ALG); 			
		Mac mac;
		
		try {
			mac = Mac.getInstance(CRYPT_ALG);
			mac.init(key);
		} catch (InvalidKeyException e) {
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.UNKNOWN);
		} catch (NoSuchAlgorithmException e) {
			Log.e(Util.APP, e.getMessage(), e);
			throw new ServiceCommException(ServiceID.GROOVESHARK, ServiceErr.UNKNOWN);
		}
		
		String aux = JSONValue.toJSONString(args);
		
		byte[] result = mac.doFinal(aux.getBytes());
		return result.toString();
	}
	
	private String getStringFromList(List<NameValuePair> list){
		String str = null;
		
		StringBuilder sb = new StringBuilder();
		
		for(NameValuePair nameValue : list){
			sb.append(nameValue.getName() + "");
			
		}
		
		return sb.toString();
	}
}
