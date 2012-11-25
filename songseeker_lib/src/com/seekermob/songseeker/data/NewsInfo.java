package com.seekermob.songseeker.data;

import java.text.DateFormat;

import com.echonest.api.v4.News;

import android.os.Parcel;
import android.os.Parcelable;

public class NewsInfo implements Parcelable {

	public String name;
	public String summary;
	public String date;
	public String url;	
	
	public NewsInfo(){}
	
	public NewsInfo(News news, DateFormat df){
		name = news.getName();
		summary = news.getSummary().replaceAll("\\<.*?>","");
		date = df.format(news.getDatePosted());
		url = news.getURL();
	}
	
	public NewsInfo(Parcel in){
		readFromParcel(in);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) { 
		dest.writeString(name);
		dest.writeString(summary);
		dest.writeString(date);
		dest.writeString(url);		
	}

	private void readFromParcel(Parcel in) {
		name = in.readString();
		summary = in.readString();
		date = in.readString();
		url = in.readString();
	}

    @SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR =
    	new Parcelable.Creator() {
            public NewsInfo createFromParcel(Parcel in) {
                return new NewsInfo(in);
            }
 
            public NewsInfo[] newArray(int size) {
                return new NewsInfo[size];
            }
        };
}
