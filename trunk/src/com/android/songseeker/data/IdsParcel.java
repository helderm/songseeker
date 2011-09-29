package com.android.songseeker.data;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class IdsParcel implements Parcelable{
	private ArrayList<String> ids = new ArrayList<String>();

	public IdsParcel(){;}
	
	public void addId(String id) {
		ids.add(id);
	}

	public ArrayList<String> getIds() {
		return ids;
	}

	//@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	//@Override
	public void writeToParcel(Parcel out, int flags) {		
		out.writeList(ids);
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
    	in.readList(ids, null);
    	//mData = in.readInt();
    }
	
}
