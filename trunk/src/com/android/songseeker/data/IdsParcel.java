package com.android.songseeker.data;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class IdsParcel implements Parcelable{
	private ArrayList<String> songIDs = new ArrayList<String>();

	public IdsParcel(){;}
	
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

    public static final Parcelable.Creator<IdsParcel> CREATOR
            = new Parcelable.Creator<IdsParcel>() {
        public IdsParcel createFromParcel(Parcel in) {
            return new IdsParcel(in);
        }

        public IdsParcel[] newArray(int size) {
            return new IdsParcel[size];
        }
    };
    
    private IdsParcel(Parcel in) {
    	in.readList(songIDs, null);
    	//mData = in.readInt();
    }
	
}
