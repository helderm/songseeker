package com.seekermob.songseeker.ui;

import com.seekermob.songseeker.R;

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

public class InputDialogFragment extends DialogFragment{
	private OnTextEnteredListener listener; 
	
	public static InputDialogFragment newInstance(int hint, OnTextEnteredListener l){
		InputDialogFragment diag = new InputDialogFragment();
        Bundle args = new Bundle();
        args.putInt("hint", hint);
        diag.setArguments(args);
        
        diag.listener = l;
                
        return diag;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
        int hint = getArguments().getInt("hint");
        
        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_input, null, false);
        
        final EditText etInput = (EditText) v.findViewById(R.id.text_input);
        
        //set Button
        final Button btnOk = (Button) v.findViewById(R.id.btn_ok);
        btnOk.setEnabled(false); //will be enabled when the user enters some text
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	listener.onDialogTextEntered(etInput.getText().toString());
            	dismiss();
            }
        });        
        
        //set EditText        
        etInput.setHint(hint);
        etInput.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {                
                if(s.length() <= 0)
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
        Fragment prev = activity.getSupportFragmentManager().findFragmentByTag("input-dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.        
        show(ft, "input-dialog");
    }	
	
	public static interface OnTextEnteredListener{
		public void onDialogTextEntered(String text);
	}
	
}
