package com.android.songseeker.data;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class SongNamesParcel implements Parcelable{

	private ArrayList<String> songNames = new ArrayList<String>();

	public SongNamesParcel(){;}
	
	public void addName(String name) {
		songNames.add(name);		
	}

	public ArrayList<String> getSongNames() {
		return songNames;
	}

	//@Override
	public int describeContents() {
		return 0;
	}

	//@Override
	public void writeToParcel(Parcel out, int flags) {		
		out.writeList(songNames);
	}

    public static final Parcelable.Creator<SongNamesParcel> CREATOR
            = new Parcelable.Creator<SongNamesParcel>() {
        public SongNamesParcel createFromParcel(Parcel in) {
            return new SongNamesParcel(in);
        }

        public SongNamesParcel[] newArray(int size) {
            return new SongNamesParcel[size];
        }
    };
    
    private SongNamesParcel(Parcel in) {
    	in.readList(songNames, null);
    }	
}
