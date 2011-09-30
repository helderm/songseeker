package com.android.songseeker.data;

import android.os.Parcel;
import android.os.Parcelable;

public class ArtistInfo implements Parcelable{
	public String name;
	public String buyUrl;
	public String id;

	public ArtistInfo() { ; };
 
	public ArtistInfo(Parcel in) {
		readFromParcel(in);
	} 
	
	public String getName() {
		return name;
	} 
	public void setName(String name) {
		this.name = name;
	}
 
	public String getBuyUrl() {
		return buyUrl;
	} 
	public void seBuyUrl(String buyUrl) {
		this.buyUrl = buyUrl;
	}
	
	public String getId() {
		return name;
	} 
	public void setId(String id) {
		this.id = id;
	}
 
	@Override
	public int describeContents() {
		return 0;
	}
 
	@Override
	public void writeToParcel(Parcel dest, int flags) {
 
		dest.writeString(name);
		dest.writeString(buyUrl);
		dest.writeString(id);
	}
 
	/**
	 *
	 * Called from the constructor to create this
	 * object from a parcel.
	 *
	 * @param in parcel from which to re-create object
	 */
	private void readFromParcel(Parcel in) {
 
		// We just need to read back each
		// field in the order that it was
		// written to the parcel
		name = in.readString();
		buyUrl = in.readString();
		id = in.readString();
	}
 
    /**
     *
     * This field is needed for Android to be able to
     * create new objects, individually or as arrays.
     *
     * This also means that you can use use the default
     * constructor to create the object and use another
     * method to hyrdate it as necessary.
     *
     * I just find it easier to use the constructor.
     * It makes sense for the way my brain thinks ;-)
     *
     */
    public static final Parcelable.Creator CREATOR =
    	new Parcelable.Creator() {
            public ArtistInfo createFromParcel(Parcel in) {
                return new ArtistInfo(in);
            }
 
            public ArtistInfo[] newArray(int size) {
                return new ArtistInfo[size];
            }
        };
}
