package com.android.songseeker.data;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class SearchSeed implements Parcelable{

	private ArrayList<String> artistList = new ArrayList<String>();

	public SearchSeed(){;}
	
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

    public static final Parcelable.Creator<SearchSeed> CREATOR
            = new Parcelable.Creator<SearchSeed>() {
        public SearchSeed createFromParcel(Parcel in) {
            return new SearchSeed(in);
        }

        public SearchSeed[] newArray(int size) {
            return new SearchSeed[size];
        }
    };
    
    private SearchSeed(Parcel in) {
    	in.readList(artistList, null);
    	//mData = in.readInt();
    }

}
