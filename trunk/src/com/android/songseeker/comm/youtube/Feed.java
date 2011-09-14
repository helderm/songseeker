package com.android.songseeker.comm.youtube;

import com.google.api.client.util.Key;

import java.util.List;

public class Feed<T extends Item> {

  @Key
  public List<T> items;

  @Key
  public int totalItems;
}
