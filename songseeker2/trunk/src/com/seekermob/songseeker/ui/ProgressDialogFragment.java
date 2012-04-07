package com.seekermob.songseeker.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class ProgressDialogFragment extends DialogFragment{
    
	public static ProgressDialogFragment newInstance(int msg, int max) {
		ProgressDialogFragment frag = new ProgressDialogFragment();
        Bundle args = new Bundle();
        args.putInt("message", msg);
        args.putInt("max", max);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int msg = getArguments().getInt("message");
        int max = getArguments().getInt("max");
        
        //this dismiss the dialog as I wish it to do when the orientation changes, 
        //but this may be a bug. Well, it works for now!
        setRetainInstance(true);
        
        ProgressDialog pd = new ProgressDialog(getActivity());
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);               
        pd.setCancelable(true);
        pd.setMax(max);       
        
        if(msg != 0)
        	pd.setMessage(getString(msg));
                
        //setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Holo);
        
        return pd;
    }	
    
    public static DialogFragment showDialog(int msg, int max, FragmentManager fm) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction. We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = ProgressDialogFragment.newInstance(msg, max);
        newFragment.show(ft, "dialog");
        
        return newFragment;
    }
}
