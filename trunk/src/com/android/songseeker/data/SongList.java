package com.android.songseeker.data;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class SongList implements Parcelable{
	private ArrayList<String> songIDs = new ArrayList<String>();

	public SongList(){;}
	
	public synchronized void addSongID(String id) {
		songIDs.add(id);
	}

	public synchronized ArrayList<String> getSongIDs() {
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

    public static final Parcelable.Creator<SongList> CREATOR
            = new Parcelable.Creator<SongList>() {
        public SongList createFromParcel(Parcel in) {
            return new SongList(in);
        }

        public SongList[] newArray(int size) {
            return new SongList[size];
        }
    };
    
    private SongList(Parcel in) {
    	in.readList(songIDs, null);
    	//mData = in.readInt();
    }
	
}
