package com.seekermob.songseeker.data;

import com.seekermob.songseeker.data.UserProfile.ArtistProfile;

import android.os.Parcel;
import android.os.Parcelable;

public class ArtistInfo implements Parcelable{
	public String name;
	public String buyUrl;
	public String id;
	public String image;

	public ArtistInfo() { 
		name = null;
		buyUrl = null;
		id = null;
		image = null;
	};
	
	/*public ArtistInfo(ArtistInfoCache artist){
		if(artist == null){
			name = null;
			buyUrl = null;
			id = null;
			image = null;
			return;
		}
		
		name = artist.name;
		buyUrl = artist.buyUrl;
		id = artist.id;
		image = artist.image;	
	}*/
 
	public ArtistInfo(ArtistProfile artist){
		name = artist.name;
		buyUrl = artist.buyUrl;
		id = artist.id;
		image = artist.image;
	}
	
	public ArtistInfo(Parcel in) {
		readFromParcel(in);
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
	}
 
	private void readFromParcel(Parcel in) {
 
		// We just need to read back each
		// field in the order that it was
		// written to the parcel
		name = in.readString();
		buyUrl = in.readString();
		id = in.readString();
		image = in.readString();
	}
 
    @SuppressWarnings("rawtypes")
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
