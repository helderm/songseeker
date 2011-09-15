package com.android.songseeker.comm.youtube;

import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.Key;

public class Item {

  @Key
  public String title;

  @Key
  public DateTime updated;
  
  @Key
  public String id;
  
  public JsonHttpContent toContent(JsonFactory jsonFactory) {
	  JsonHttpContent result = new JsonHttpContent(jsonFactory, this);
	  return result;
  }
}
