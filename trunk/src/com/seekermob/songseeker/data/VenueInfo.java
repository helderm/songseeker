package com.seekermob.songseeker.data;

import android.os.Parcel;
import android.os.Parcelable;

public class VenueInfo implements Parcelable{
	public String id;
	public String name;
	public String city;
	public String latitude;
	public String longitude;
	public String country;
	public String url;

	public VenueInfo() { 
		name = null;
		city = null;
		id = null;
		latitude = null;		
		longitude = null;
		country = null;
		url = null;		
	};
 
	public VenueInfo(Parcel in) {
		readFromParcel(in);
	} 
 
	@Override
	public int describeContents() {
		return 0;
	}
 
	@Override
	public void writeToParcel(Parcel dest, int flags) { 
		dest.writeString(name);
		dest.writeString(city);
		dest.writeString(id);
		dest.writeString(latitude);		
		dest.writeString(longitude);
		dest.writeString(country);
		dest.writeString(url);
	}
 
	private void readFromParcel(Parcel in) {
 
		// We just need to read back each
		// field in the order that it was
		// written to the parcel
		name = in.readString();
		city = in.readString();
		id = in.readString();
		latitude = in.readString();		
		longitude = in.readString();
		country = in.readString();
		url = in.readString();		
	}
 
    @SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR =
    	new Parcelable.Creator() {
            public VenueInfo createFromParcel(Parcel in) {
                return new VenueInfo(in);
            }
 
            public VenueInfo[] newArray(int size) {
                return new VenueInfo[size];
            }
        };
}
