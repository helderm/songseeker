package com.android.songseeker.comm.youtube;

import com.google.api.client.util.Key;

import java.util.ArrayList;
import java.util.List;

public class Video extends Item {

  @Key
  public String description;

  @Key
  public List<String> tags = new ArrayList<String>();

  @Key
  public Player player;
}
