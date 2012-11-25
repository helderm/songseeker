package com.seekermob.songseeker.data;

import android.os.Parcel;
import android.os.Parcelable;

public class PlaylistInfo implements Parcelable {
	public String id;
	public String name;
	public String numSongs;
	public String imageUrl;		
	
	public PlaylistInfo(){}
	
	public PlaylistInfo(Parcel in){
		readFromParcel(in);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) { 
		dest.writeString(id);
		dest.writeString(name);
		dest.writeString(numSongs);
		dest.writeString(imageUrl);		
	}

	private void readFromParcel(Parcel in) {
		id = in.readString();
		name = in.readString();
		numSongs = in.readString();
		imageUrl = in.readString();
	}

    @SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR =
    	new Parcelable.Creator() {
            public PlaylistInfo createFromParcel(Parcel in) {
                return new PlaylistInfo(in);
            }
 
            public PlaylistInfo[] newArray(int size) {
                return new PlaylistInfo[size];
            }
        };

}
