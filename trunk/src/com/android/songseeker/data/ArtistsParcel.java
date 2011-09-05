package com.android.songseeker.data;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class ArtistsParcel implements Parcelable{

	private ArrayList<String> artistList = new ArrayList<String>();

	public ArtistsParcel(){;}
	
	public void addArtist(String artist) {
		artistList.add(artist);		
	}

	public ArrayList<String> getArtistList() {
		return artistList;
	}

	//@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	//@Override
	public void writeToParcel(Parcel out, int flags) {		
		out.writeList(artistList);
	}

    public static final Parcelable.Creator<ArtistsParcel> CREATOR
            = new Parcelable.Creator<ArtistsParcel>() {
        public ArtistsParcel createFromParcel(Parcel in) {
            return new ArtistsParcel(in);
        }

        public ArtistsParcel[] newArray(int size) {
            return new ArtistsParcel[size];
        }
    };
    
    private ArtistsParcel(Parcel in) {
    	in.readList(artistList, null);
    	//mData = in.readInt();
    }

}
