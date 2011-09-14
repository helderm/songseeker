package com.android.songseeker.comm.youtube;

import com.google.api.client.util.DateTime;
import com.google.api.client.util.Key;

public class Item {

  @Key
  public String title;

  @Key
  public DateTime updated;
  
  @Key
  public String id;
}
