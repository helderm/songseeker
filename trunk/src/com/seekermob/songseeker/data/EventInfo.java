package com.seekermob.songseeker.data;

import android.os.Parcel;
import android.os.Parcelable;

public class EventInfo implements Parcelable{
	public String id;
	public String artistName;
	public String url;	
	public String ticketStatus;
	public String ticketUrl;
	public String date;
	public VenueInfo venue;

	public EventInfo() { 
		id = null;
		artistName = null;
		url = null;
		ticketStatus = null;
		ticketUrl = null;
		date = null;
		venue = new VenueInfo();
	};
 
	public EventInfo(Parcel in) {
		readFromParcel(in);
	} 
 
	@Override
	public int describeContents() {
		return 0;
	}
 
	@Override
	public void writeToParcel(Parcel dest, int flags) {
 
		dest.writeString(id);
		dest.writeString(artistName);
		dest.writeString(url);
		dest.writeString(ticketStatus);
		dest.writeString(ticketUrl);
		dest.writeString(date);
		dest.writeParcelable(venue, flags);
	}
 
	private void readFromParcel(Parcel in) {
 
		// We just need to read back each
		// field in the order that it was
		// written to the parcel
		id = in.readString();
		artistName = in.readString();
		url = in.readString();
		ticketStatus = in.readString();
		ticketUrl = in.readString();
		date = in.readString();
		venue = in.readParcelable(VenueInfo.class.getClassLoader());
	}
 
    @SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR =
    	new Parcelable.Creator() {
            public EventInfo createFromParcel(Parcel in) {
                return new EventInfo(in);
            }
 
            public EventInfo[] newArray(int size) {
                return new EventInfo[size];
            }
        };
}
