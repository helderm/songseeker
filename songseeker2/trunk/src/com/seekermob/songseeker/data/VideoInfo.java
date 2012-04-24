package com.seekermob.songseeker.data;

import android.os.Parcel;
import android.os.Parcelable;

public class VideoInfo implements Parcelable{
	public String id;
	public String title;
	public String description;
	public String image;	
	
	public VideoInfo(){}
	
	public VideoInfo(Parcel in){
		readFromParcel(in);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) { 
		dest.writeString(id);
		dest.writeString(title);
		dest.writeString(description);
		dest.writeString(image);		
	}

	private void readFromParcel(Parcel in) {
		id = in.readString();
		title = in.readString();
		description = in.readString();
		image = in.readString();
	}

    @SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR =
    	new Parcelable.Creator() {
            public VideoInfo createFromParcel(Parcel in) {
                return new VideoInfo(in);
            }
 
            public VideoInfo[] newArray(int size) {
                return new VideoInfo[size];
            }
        };
}
