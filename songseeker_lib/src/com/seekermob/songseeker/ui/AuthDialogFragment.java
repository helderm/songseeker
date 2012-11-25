package com.seekermob.songseeker.ui;

import com.seekermob.songseeker.R;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class AuthDialogFragment extends DialogFragment {
	private OnUserPassEnteredListener mListener;
	private static final String TAG = "auth-dialog";
	
	public static AuthDialogFragment newInstance(int hint){
		AuthDialogFragment diag = new AuthDialogFragment();
        Bundle args = new Bundle();
        args.putInt("hint", hint);
        
        diag.setArguments(args);
        return diag;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final int hint = getArguments().getInt("hint");
				
		View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_auth, null, false);		
		final EditText etUser = (EditText) v.findViewById(R.id.username_input);
		final EditText etPass = (EditText) v.findViewById(R.id.password_input);
		final Button btnOk = (Button) v.findViewById(R.id.btn_ok);
		
        //set Button        
        btnOk.setEnabled(false); //will be enabled when the user enters user/pass
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	mListener.onUserPassEntered(etUser.getText().toString(), etPass.getText().toString());
            	dismiss();            	
            }
        });  
		
        //set username edittext     
        etUser.setHint(hint);
        etUser.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {                
                if(s.length() <= 0 || etPass.getText().length() <= 0)
                	btnOk.setEnabled(false);
                else
                	btnOk.setEnabled(true);
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });
        
        //set password edittext        
        etPass.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {                
                if(s.length() <= 0 || etUser.getText().length() <= 0)
                	btnOk.setEnabled(false);
                else
                	btnOk.setEnabled(true);
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
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
        //ft.addToBackStack(null);

        // Create and show the dialog.        
        show(ft, TAG);
    }	
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnUserPassEnteredListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnUserPassEnteredListener");
        }
    }    
    
	public static interface OnUserPassEnteredListener {
		public void onUserPassEntered(String user, String pass);
	}	
}
