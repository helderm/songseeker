package com.seekermob.songseeker.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.data.ArtistInfo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.AnimationUtils;

public class Util {

	public static final String APP = "SongSeeker";
	public static final String ECHONEST_URL = "http://the.echonest.com/";
	public static final String SEVENDIGITAL_URL = "http://www.7digital.com/";
	public static final String DESIGNER_URL = "http://www.lucassanchez.com.br/";
	public static final String ANDROIDICONS_URL = "http://www.androidicons.com/"; 
	public static final String RDIO_URL = "http://www.rdio.com/";
	public static final String GROOVESHARK_URL = "http://www.grooveshark.com/";
	public static final String YOUTUBE_URL = "http://www.youtube.com/";
	public static final String LASTFM_URL = "http://www.lastfm.com/";
	
	public static ArrayList<ArtistInfo> getArtistsFromDevice(Activity a) throws Exception{
		ArrayList<ArtistInfo> artists = new ArrayList<ArtistInfo>();

		String[] projection = new String[] {
				MediaStore.Audio.ArtistColumns.ARTIST,
				MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS
		};

		Cursor cursor = a.getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, 
				projection, null, null, MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS + " DESC");

		int music_column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.ARTIST);

		for(int i=0; i<cursor.getCount(); i++){
			cursor.moveToPosition(i);
			String artistName = cursor.getString(music_column_index);	
			if(artistName != null){
				ArtistInfo artist = new ArtistInfo();
				artist.name = artistName;
				artists.add(artist);
			}
			
			if(i == 50)
				break;
		}

		cursor.close();

		return artists;
	}

	public static void CopyStream(InputStream is, OutputStream os){
		final int buffer_size=1024;
		try{
			byte[] bytes=new byte[buffer_size];
			for(;;){
				int count=is.read(bytes, 0, buffer_size);
				if(count==-1)
					break;
				os.write(bytes, 0, count);
			}
		}
		catch(Exception ex){}
	}
	
	/** Save the object to the device persistent storage*/
	public static void  writeObjectToDevice(Activity activity, Object object, String filename){		
		ObjectOutputStream out = null;
		
		try {
			FileOutputStream fos = activity.openFileOutput(filename, Context.MODE_PRIVATE);
			out = new ObjectOutputStream(fos);
			out.writeObject(object);
			out.close();
		} catch (Exception e) {
			Log.w(Util.APP, "Couldnt write "+filename +" file to device memory, it may be lost in the next reboot of the app!", e);
		} finally{
			if(out != null){
				try {
					out.close();
				} catch (IOException e) {}
			}
		}
	}
	
	/** Restore the object written on disk, if it exists */
	public static Object readObjectFromDevice(Context context, String filename){
		Object object = null;
		ObjectInputStream in = null;
		try{			
			in = new ObjectInputStream(context.openFileInput(filename));
			object = in.readObject();			
		}catch(FileNotFoundException e){   
			return null;
		}catch(Exception e){
			Log.w(Util.APP, "Couldnt read "+filename +" file from device memory, nothing serious...", e);
			return null;
		}finally{
			if(in != null){
				try {
					in.close();
				} catch (IOException e) {}
			}
		}
		
		return object;
	}	
	
	/** Restore the object written on the cache, if it exists */
	public static Object readObjectFromCache(Context context, String filename){
		Object object = null;
		ObjectInputStream in = null;
		
		File f = new File(context.getCacheDir(), filename);
		
		try{			
			in = new ObjectInputStream(new FileInputStream(f));
			object = in.readObject();			
		}catch(FileNotFoundException e){   
			return null;
		}catch(Exception e){			
			return null;
		}finally{
			if(in != null){
				try {
					in.close();
				} catch (IOException e) {}
			}
		}
		
		return object;
	}	
	
	/** Check if the specified app is installed on the device*/
    public static boolean isAppInstalled(String uri, Context context){
    	PackageManager pm = context.getPackageManager();
    	boolean app_installed = false;

    	try{
    		pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
    		app_installed = true;
    	}catch (NameNotFoundException e){
    		app_installed = false;
    	}

        return app_installed ;
    }
    
    /** Sets the view in and out of visibility graciously. The fragment must contain
     * id's 'data_container' and 'progress_container' that points to the list and the progress
     * bar, respectively*/
    public static void setListShown(Fragment fragment, boolean isShown){
    	Context context;
    	View v, fragmentView;
    	
    	//TODO: Check again!!! This occurs when changing orientation
    	if(fragment.getActivity() == null)
    		return;
    	
    	fragmentView = fragment.getView();    	
    	context = fragment.getActivity().getApplicationContext();
    	
    	if(isShown){			
			v = fragmentView.findViewById(R.id.progress_container);
			v.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out));
			v.setVisibility(View.GONE);
			
			v = fragmentView.findViewById(R.id.data_container);
			v.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in));
			v.setVisibility(View.VISIBLE);	
			
    	}else{			
			v = fragmentView.findViewById(R.id.data_container);
			v.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out));
			v.setVisibility(View.GONE);			
			
			v = fragmentView.findViewById(R.id.progress_container);
			v.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in));
			v.setVisibility(View.VISIBLE);			
    	}
    	
    	v = null; fragmentView = null; context = null;   	
    }
    
    /** Fades the progress overlay in and out of view graciously. The fragment must
     * contain the overlay viewstub with id's overlay_update and stub_update
     * Returns the progressOverlay view*/
    public static View setProgressShown(Fragment fragment, boolean isShown){
    	View fragmentView, progressOverlay;
    	Context context;
    	
    	//TODO: Check again!!! This occurs when changing orientation
    	if(fragment.getActivity() == null)
        	return null;
    	
    	fragmentView = fragment.getView();    	   	
    	
        // see if we already inflated the progress overlay
        progressOverlay = fragmentView.findViewById(R.id.overlay_update);
        if (progressOverlay == null) {
        	progressOverlay = ((ViewStub) fragmentView.findViewById(R.id.stub_update)).inflate();
        }     
    	       	        
        context = fragment.getActivity().getApplicationContext();
        
        if(isShown){
        	progressOverlay.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in));
        	progressOverlay.setVisibility(View.VISIBLE);
        }else{
        	progressOverlay.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out));
        	progressOverlay.setVisibility(View.GONE);
        }  
        
        fragmentView = null; context = null;
        return progressOverlay; 
    }
    
	/**
	 * Copied from SeriesGuide
	 * This is a helper class that implements the management of tabs and all
	 * details of connecting a ViewPager with associated TabHost. It relies on a
	 * trick. Normally a tab host has a simple API for supplying a View or
	 * Intent that each tab will show. This is not sufficient for switching
	 * between pages. So instead we make the content part of the tab host 0dp
	 * high (it is not shown) and the TabsAdapter supplies its own dummy view to
	 * show as the tab content. It listens to changes in tabs, and takes care of
	 * switch to the correct paged in the ViewPager whenever the selected tab
	 * changes.
	 */    
	public static class TabsAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener, ActionBar.TabListener {

		private final Context mContext;
		private final ActionBar mActionBar;
		private final ViewPager mViewPager;
		private final ArrayList<String> mTabs = new ArrayList<String>();
		private final ArrayList<Bundle> mArgs = new ArrayList<Bundle>();

		public TabsAdapter(FragmentActivity activity, ActionBar actionBar, ViewPager pager) {
			super(activity.getSupportFragmentManager());
			mContext = activity;
			mActionBar = actionBar;
			mViewPager = pager;
			mViewPager.setAdapter(this);
			mViewPager.setOnPageChangeListener(this);
			
			//increase this so the viewpager will hold all my tabs 
			mViewPager.setOffscreenPageLimit(2);
		}

		public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
			mTabs.add(clss.getName());
			mArgs.add(args);
			mActionBar.addTab(tab.setTabListener(this));
			notifyDataSetChanged();
		}
		
		public void addTab(ActionBar.Tab tab, Class<?> clss){
			addTab(tab, clss, null);
		}		

		@Override
		public int getCount() {
			return mTabs.size();
		}

		@Override
		public Fragment getItem(int position) {
			return Fragment.instantiate(mContext, mTabs.get(position), mArgs.get(position));
		}

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		}

		@Override
		public void onPageSelected(int position) {
			mActionBar.setSelectedNavigationItem(position);
		}

		@Override
		public void onPageScrollStateChanged(int state) {
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			mViewPager.setCurrentItem(tab.getPosition());
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}    
}
