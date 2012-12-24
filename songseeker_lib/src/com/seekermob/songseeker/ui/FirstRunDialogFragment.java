package com.seekermob.songseeker.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;

import com.seekermob.songseeker.R;

public class FirstRunDialogFragment extends DialogFragment {

    private static final String PREF_KEY_FIRSTRUN = "first_run";
    protected static final String TAG = "FirstRunFragment";

    private OnFirstRunImportProfileListener mListener;

    public static FirstRunDialogFragment newInstance() {
        FirstRunDialogFragment f = new FirstRunDialogFragment();
        return f;
    }

    public interface OnFirstRunImportProfileListener {
        public void OnFirstRunImportProfile();
    }

    public static boolean hasSeenFirstRunFragment(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_KEY_FIRSTRUN, false);
    }
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        prefs.edit().putBoolean(PREF_KEY_FIRSTRUN, true).commit();
		
        View v = getActivity().getLayoutInflater().inflate(R.layout.first_run, null, false);
        
        //set Button
        final Button btnImportProfile = (Button) v.findViewById(R.id.btn_import_profile);
        btnImportProfile.setEnabled(true);
        btnImportProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
           		mListener.OnFirstRunImportProfile();
            	dismiss();            	
            }
        });        
        
        Dialog dialog = new Dialog(getActivity(), R.style.DialogThemeNoTitle);
        dialog.setContentView(v);
        return dialog;
	}

    public void showDialog(FragmentActivity activity) {
        // show dialog
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        Fragment prev = activity.getSupportFragmentManager().findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.        
        show(ft, TAG);
    }	
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFirstRunImportProfileListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFirstRunImportProfileListener");
        }
    }
}

