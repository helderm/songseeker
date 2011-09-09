package com.android.songseeker.activity;

import com.android.songseeker.util.Util;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class CreatePlaylistYoutubeActivity extends Activity {
	GoogleAccountManager accountManager;
	GoogleAccessProtectedResource accessProtectedResource = new GoogleAccessProtectedResource(null);
	private static final String AUTH_TOKEN_TYPE = "Youtube";

	private static final int DIALOG_ACCOUNTS = 0;
	public static final int REQUEST_AUTHENTICATE = 0;
	private static final String PREF = "MyPrefs";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		accountManager = new GoogleAccountManager(this);
		gotAccount(false);
	}


	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ACCOUNTS:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Select a Google account");
			final Account[] accounts = accountManager.getAccounts();
			final int size = accounts.length;
			String[] names = new String[size];
			for (int i = 0; i < size; i++) {
				names[i] = accounts[i].name;
			}
			builder.setItems(names, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					gotAccount(accounts[which]);
				}
			});
			return builder.create();
		}
		return null;
	}

	void gotAccount(boolean tokenExpired) {
		SharedPreferences settings = getSharedPreferences(PREF, 0);
		String accountName = settings.getString("accountName", null);
		Account account = accountManager.getAccountByName(accountName);
		if (account != null) {
			if (tokenExpired) {
				accountManager.invalidateAuthToken(accessProtectedResource.getAccessToken());
				accessProtectedResource.setAccessToken(null);
			}
			gotAccount(account);
			return;
		}
		showDialog(DIALOG_ACCOUNTS);
	}

	void gotAccount(final Account account) {
		SharedPreferences settings = getSharedPreferences(PREF, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("accountName", account.name);
		editor.commit();
		accountManager.manager.getAuthToken(
				account, AUTH_TOKEN_TYPE, true, new AccountManagerCallback<Bundle>() {

					public void run(AccountManagerFuture<Bundle> future) {
						try {
							Bundle bundle = future.getResult();
							if (bundle.containsKey(AccountManager.KEY_INTENT)) {
								Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
								intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivityForResult(intent, REQUEST_AUTHENTICATE);
							} else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
								accessProtectedResource.setAccessToken(
										bundle.getString(AccountManager.KEY_AUTHTOKEN));
								onAuthToken();
							}
						} catch (Exception e) {
							handleException(e);
						}
					}
				}, null);
	}

	void onAuthToken() {
		Log.i(Util.APP, "onAuthToken()");
	}

	void handleException(Exception e){
		Log.e(Util.APP, "handleException()", e);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.i(Util.APP, "onActivityResult()");

		switch (requestCode) {
		case REQUEST_AUTHENTICATE:
			if (resultCode == RESULT_OK) {
				gotAccount(false);
			} else {
				showDialog(DIALOG_ACCOUNTS);
			}
			break;
		}
	}

}
