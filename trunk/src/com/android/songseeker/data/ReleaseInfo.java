package com.android.songseeker.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.android.songseeker.data.ArtistInfo;

public class ReleaseInfo implements Parcelable {
	public String name;
	public String id;
	public String buyUrl;
	public String image;
	
	public ArtistInfo artist;
	
	public ReleaseInfo() { 
		artist = new ArtistInfo(); 
	}
 
	/**
	 *
	 * Constructor to use when re-constructing object
	 * from a parcel
	 *
	 * @param in a parcel from which to read this object
	 */
	public ReleaseInfo(Parcel in) {
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
	
	public String getImage(){
		return image;
	}
	public void setImage(String image){
		this.image = image;
	}
	
	public ArtistInfo getArtist(){
		return artist;
	}
	public void setArtist(ArtistInfo artist){
		this.artist = artist;
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
		dest.writeString(image);
		dest.writeParcelable(artist, flags);
	}
 
	private void readFromParcel(Parcel in) {
 
		// We just need to read back each
		// field in the order that it was
		// written to the parcel
		name = in.readString();
		buyUrl = in.readString();
		id = in.readString();
		image = in.readString();
		
		// readParcelable needs the ClassLoader
		// but that can be picked up from the class
		// This will solve the BadParcelableException
		// because of ClassNotFoundException
		artist = in.readParcelable(ArtistInfo.class.getClassLoader());
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
            public ReleaseInfo createFromParcel(Parcel in) {
                return new ReleaseInfo(in);
            }
 
            public ReleaseInfo[] newArray(int size) {
                return new ReleaseInfo[size];
            }
        };
}
