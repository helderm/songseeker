package com.seekermob.songseeker.activity;

import java.util.ArrayList;

import com.seekermob.songseeker.R;
import com.seekermob.songseeker.comm.EchoNestComm;
import com.seekermob.songseeker.comm.ServiceCommException;
import com.seekermob.songseeker.data.ArtistInfo;
import com.seekermob.songseeker.util.Util;
import com.echonest.api.v4.News;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ArtistNewsActivity extends ListActivity {
	private ArtistNewsAdapter adapter;

	private static final int ARTIST_NEWS_DIAG = 0;	
	private static final int MAX_NUM_NEWS = 12;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

        setContentView(R.layout.listview);
        
        // Tell the list view which view to display when the list is empty
        getListView().setEmptyView(findViewById(R.id.empty));	    
	    
		//set EN logo at the end of the listview
		setLogoFooter();
		        
        adapter = new ArtistNewsAdapter();
        setListAdapter(adapter);
	    
        //check for orientation change
        @SuppressWarnings("unchecked")
		ArrayList<News> savedNews = (ArrayList<News>) getLastNonConfigurationInstance();
        
        if(savedNews == null)
        	new GetArtistNews().execute();
        else
        	adapter.setArtistNews(savedNews);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case ARTIST_NEWS_DIAG:
			ProgressDialog pd = new ProgressDialog(this);
			pd.setMessage("Fetching artist news...");
			pd.setIndeterminate(true);
			pd.setCancelable(true);
			return pd;
		default:
			return null;		    	
		}
	}
	
	private class GetArtistNews extends AsyncTask<Void, Void, ArrayList<News>>{
		String err = null;

		@Override
		protected void onPreExecute() {
			showDialog(ARTIST_NEWS_DIAG);
		}
		
		@Override
		protected ArrayList<News> doInBackground(Void... params) {
			ArrayList<News> news;
			
			try {				
				ArtistInfo artist = getIntent().getExtras().getParcelable("artistParcel");
				news = EchoNestComm.getComm().getArtistNewsFromBucket(artist.id, MAX_NUM_NEWS, true);
				if(news.size() < MAX_NUM_NEWS/4){
					//fetch low relevance news
					news = EchoNestComm.getComm().getArtistNewsFromBucket(artist.id, MAX_NUM_NEWS, false);
				}
			} catch (ServiceCommException e) {
				err = e.getMessage();
				return null;
			}
			
			return news;
		}
		
		@Override
		protected void onPostExecute(ArrayList<News> news) {
			
			removeDialog(ARTIST_NEWS_DIAG);
			
			if(err != null){
				Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				ArtistNewsActivity.this.finish();
				return;
			}
			
			adapter.setArtistNews(news);			
		}
	}
	
	private class ArtistNewsAdapter extends BaseAdapter{
		public ArrayList<News> news;
	    private LayoutInflater inflater;
	    	
		public ArtistNewsAdapter() {
			news = null;
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		@Override
		public int getCount() {
			if(news == null)
				return 0;
			
			return news.size();
		}

		@Override
		public News getItem(int pos) {			
			return news.get(pos);
		}

		@Override
		public long getItemId(int pos) {			
			return pos;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
	    	
			if (convertView == null) {
			    //LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.artist_news, null);
				
				holder = new ViewHolder();
				holder.firstLine = (TextView) convertView.findViewById(R.id.artistnews_firstLine);
			    holder.secondLine = (TextView) convertView.findViewById(R.id.artistnews_secondLine);
			    holder.thirdLine = (TextView) convertView.findViewById(R.id.artistnews_thirdLine);
			    
			    convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			
			News news = adapter.getItem(position);			
			holder.firstLine.setText(news.getName());
			holder.secondLine.setText(news.getSummary().replaceAll("\\<.*?>",""));	
			holder.thirdLine.setText(news.getDatePosted().toLocaleString());
			
			return convertView;
		}
		
		public void setArtistNews(ArrayList<News> n){
			news = n;
			notifyDataSetChanged();
		}
		
		private class ViewHolder{
	    	public TextView firstLine;
	    	public TextView secondLine;
	    	public TextView thirdLine;
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		News news = adapter.getItem(position);
		
		if(news.getURL() == null)
			return;
		
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(news.getURL()));
		intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
	
	private void setLogoFooter() {		
		
		//set logo footer
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout footer = (LinearLayout)inflater.inflate(R.layout.logo, null);
		
		ImageView logo = (ImageView)footer.findViewById(R.id.logo);
		logo.setImageResource(R.drawable.echonest_logo_pwd);
		
		logo.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Util.ECHONEST_URL));
				intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		});
		
		getListView().addFooterView(footer);		
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return adapter.news;
	}
}
