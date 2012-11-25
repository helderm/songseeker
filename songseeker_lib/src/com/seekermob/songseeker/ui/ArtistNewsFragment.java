package com.seekermob.songseeker.ui;

import java.text.DateFormat;
import java.util.ArrayList;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.echonest.api.v4.News;
import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.EchoNestComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.data.NewsInfo;
import com.seekermob.songseeker.util.Util;

public class ArtistNewsFragment extends SherlockListFragment {
	
	private ArtistNewsAdapter mAdapter;
	private NewsTask mTask;
	
	private Bundle mSavedState;
	
	private static final String STATE_ADAPTER_DATA = "adapterData";
	private static final String STATE_TASK_RUNNING = "taskRunning";
	
	private static final int MAX_NUM_NEWS = 12;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		super.onActivityCreated(savedInstanceState);
		
		//set adapter				
		mAdapter = new ArtistNewsAdapter();		
		setListAdapter(mAdapter);

		//set empty view
		((TextView)(getListView().getEmptyView())).setText(R.string.artist_news_frag_empty);
		
		//restore state
		restoreLocalState(savedInstanceState);
		
		//if the adapter wasnt restored, fetch the adapter
		//but only if the task wasnt restored on restoreLocalState
		if(mAdapter.mNews == null && !isTaskRunning()){
			mTask = (NewsTask) new NewsTask().execute();
		}	
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		
		//save the adapter data
		if(mAdapter != null && mAdapter.mNews != null){
			outState.putParcelableArrayList(STATE_ADAPTER_DATA, new ArrayList<Parcelable>(mAdapter.mNews));			
		}
		
		//save the task state
		final NewsTask task = mTask;
        if(task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
        	task.cancel(true);
        	
        	outState.putBoolean(STATE_TASK_RUNNING, true);
        	mTask = null;
        }
		
		mSavedState = outState;
		
		super.onSaveInstanceState(outState);
	}
	
	/** Restores the saved instance of this fragment*/
	private void restoreLocalState(Bundle savedInstanceState){	
		if(savedInstanceState == null){
			return;
		}
		
		//restore the adapter		
		ArrayList<NewsInfo> adapterData;
		if((adapterData = savedInstanceState.getParcelableArrayList(STATE_ADAPTER_DATA)) != null){
			mAdapter.setAdapter(adapterData);			
		}
		
		//restore the top tracks task
		if(savedInstanceState.getBoolean(STATE_TASK_RUNNING)){
			mTask = (NewsTask) new NewsTask().execute();
		}
		
		mSavedState = null;
	}		
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(isTaskRunning())
			mTask.cancel(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
        if(mSavedState != null) {
            restoreLocalState(mSavedState);
        }
	}	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.listview_progress, container, false);
	}	
	
	private class ArtistNewsAdapter extends BaseAdapter{
		public ArrayList<NewsInfo> mNews;
	    private LayoutInflater mInflater;
	    
	    	
		public ArtistNewsAdapter() {
			mNews = null;
			mInflater = getActivity().getLayoutInflater();			
		}
		
		@Override
		public int getCount() {
			if(mNews == null)
				return 0;
			
			return mNews.size();
		}

		@Override
		public NewsInfo getItem(int pos) {			
			return mNews.get(pos);
		}

		@Override
		public long getItemId(int pos) {			
			return pos;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
	    	
			if (convertView == null) {			    
				convertView = mInflater.inflate(R.layout.list_item_3, null);
				
				holder = new ViewHolder();
				holder.firstLine = (TextView) convertView.findViewById(R.id.firstLine);
			    holder.secondLine = (TextView) convertView.findViewById(R.id.secondLine);
			    holder.thirdLine = (TextView) convertView.findViewById(R.id.thirdLine);
			    
			    convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			
			NewsInfo news = getItem(position);			
			holder.firstLine.setText(news.name);
			holder.secondLine.setText(news.summary);	
			holder.thirdLine.setText(news.date);
			
			return convertView;
		}
		
		public void setAdapter(ArrayList<NewsInfo> n){
			mNews = n;
			notifyDataSetChanged();
		}
		
		private class ViewHolder{
	    	public TextView firstLine;
	    	public TextView secondLine;
	    	public TextView thirdLine;
		}
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		NewsInfo news = mAdapter.getItem(position);
		
		if(news.url == null)
			return;
		
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(news.url));
		intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
	
	private class NewsTask extends AsyncTask<Void, Void, ArrayList<NewsInfo>>{
		String err = null;

		@Override
		protected void onPreExecute() {
			Util.setListShown(ArtistNewsFragment.this, false);
		}
		
		@Override
		protected ArrayList<NewsInfo> doInBackground(Void... params) {
			ArrayList<News> newsList;
			ArrayList<NewsInfo> newsInfoList = new ArrayList<NewsInfo>();
			
			if(isCancelled())
				return null;
			
			try {				
				ArtistInfo artist = getArguments().getParcelable(ArtistInfoFragment.BUNDLE_ARTIST);
				newsList = EchoNestComm.getComm().getArtistNewsFromBucket(artist.id, MAX_NUM_NEWS, true);
				if(newsList.size() < MAX_NUM_NEWS/4){
					//fetch low relevance news
					newsList = EchoNestComm.getComm().getArtistNewsFromBucket(artist.id, MAX_NUM_NEWS, false);
				}
			} catch (ServiceCommException e) {
				err = e.getMessage(getActivity());
				return null;
			}
			
			DateFormat df = DateFormat.getDateInstance();
			for(News news : newsList){
				if(isCancelled())
					return null;
				
				NewsInfo newsInfo = new NewsInfo(news, df);
				newsInfoList.add(newsInfo);
			}
			
			return newsInfoList;
		}
		
		@Override
		protected void onPostExecute(ArrayList<NewsInfo> news) {
			
			Util.setListShown(ArtistNewsFragment.this, true);
			
			if(err != null){
				Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();
				return;
			}
			
			mAdapter.setAdapter(news);			
		}
	}
	
	private boolean isTaskRunning(){				
		if(mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED){
			return true;
		}
		
		return false;
	}
}
