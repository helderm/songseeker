package com.android.songseeker.comm.youtube;

import com.google.api.client.util.Key;

public class Player {
  // "default" is a Java keyword, so need to specify the JSON key manually
  @Key("default")
  public String defaultUrl;
}

