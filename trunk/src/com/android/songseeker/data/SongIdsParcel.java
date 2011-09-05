package com.android.songseeker.data;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class SongIdsParcel implements Parcelable{
	private ArrayList<String> songIDs = new ArrayList<String>();

	public SongIdsParcel(){;}
	
	public void addSongID(String id) {
		songIDs.add(id);
	}

	public ArrayList<String> getSongIDs() {
		return songIDs;
	}

	//@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	//@Override
	public void writeToParcel(Parcel out, int flags) {		
		out.writeList(songIDs);
	}

    public static final Parcelable.Creator<SongIdsParcel> CREATOR
            = new Parcelable.Creator<SongIdsParcel>() {
        public SongIdsParcel createFromParcel(Parcel in) {
            return new SongIdsParcel(in);
        }

        public SongIdsParcel[] newArray(int size) {
            return new SongIdsParcel[size];
        }
    };
    
    private SongIdsParcel(Parcel in) {
    	in.readList(songIDs, null);
    	//mData = in.readInt();
    }
	
}
