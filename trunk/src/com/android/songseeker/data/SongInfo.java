package com.android.songseeker.data;

import android.os.Parcel;
import android.os.Parcelable;

public class SongInfo implements Parcelable{
	public String name;
	public String trackNum;
	public String buyUrl;
	public String duration;
	public String id;
	public String previewUrl;
	public String version;
	
	public ArtistInfo artist;
	public ReleaseInfo release;	
	
	public SongInfo() {
		artist = new ArtistInfo();
		release = new ReleaseInfo();
	}
	
	public SongInfo(Parcel in) {
		readFromParcel(in);
	} 
	
	@Override
	public int describeContents() {
		return 0;
	}
 
	@Override
	public void writeToParcel(Parcel dest, int flags) {
 
		dest.writeString(name);
		dest.writeString(trackNum);
		dest.writeString(buyUrl);
		dest.writeString(duration);
		dest.writeString(id);
		dest.writeString(previewUrl);
		dest.writeString(version);
		dest.writeParcelable(artist, flags);
		dest.writeParcelable(release, flags);
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
		trackNum = in.readString();
		buyUrl = in.readString();
		duration = in.readString();
		id = in.readString();
		previewUrl = in.readString();
		version = in.readString();
		artist = in.readParcelable(ArtistInfo.class.getClassLoader());
		release = in.readParcelable(ReleaseInfo.class.getClassLoader());
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
            public SongInfo createFromParcel(Parcel in) {
                return new SongInfo(in);
            }
 
            public SongInfo[] newArray(int size) {
                return new SongInfo[size];
            }
        };
}
