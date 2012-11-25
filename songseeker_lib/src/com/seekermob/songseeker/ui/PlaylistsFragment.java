package com.seekermob.songseeker.ui;

import java.util.ArrayList;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.data.PlaylistInfo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class PlaylistsFragment extends SherlockListFragment {
	
	private PlaylistsAdapter mAdapter;
	
	public static final int TAB_ID = 2;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
	    //populate the optionsMenu
	    setHasOptionsMenu(true);
	    
	    //set adapter
	    mAdapter = new PlaylistsAdapter();
	    setListAdapter(mAdapter);
		
	    //check if we are recovering the state	    
	    //restoreLocalState(savedInstanceState);
	    
	    //set empty view text
	    ((TextView)(getListView().getEmptyView())).setText(R.string.playlists_frag_empty_list);
	    
	    //context menu
	    registerForContextMenu(getListView());	
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.listview_progress, null);		
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.playlists_menu, menu);		
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	private class PlaylistsAdapter extends BaseAdapter{
		private ArrayList<PlaylistInfo> mPlaylists;
		private LayoutInflater mInflater;
		
		public PlaylistsAdapter() {
			mInflater = getActivity().getLayoutInflater();
		}
		
		public int getCount() {
			if(mPlaylists == null)
				return 0;
			
			return mPlaylists.size();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return position;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
	    	
			if (convertView == null) {
			   	convertView = mInflater.inflate(R.layout.list_item_2_image_media, null);
				
				holder = new ViewHolder();
				holder.topText = (TextView) convertView.findViewById(R.id.firstLine);
			    holder.botText = (TextView) convertView.findViewById(R.id.secondLine);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}				
						
		    holder.topText.setText(mPlaylists.get(position).name);
		    holder.botText.setText(mPlaylists.get(position).numSongs + " " + getString(R.string.songs));
		    return convertView;
		}
		
		/*public void setAdapter(ArrayList<PlaylistInfo> pls){
			mPlaylists = pls;
			notifyDataSetChanged();
		}*/
		
	    private class ViewHolder{
	    	public TextView topText;
	    	public TextView botText;
	    }
	}
}
