package com.android.songseeker.task;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import android.util.Log;

public class TaskExecutor {
	private static TaskExecutor taskExecutor = new TaskExecutor();
	private static ExecutorService threadsPool;
	private static HashMap<Long, Future<Object>> futureMap;
	private static long id = 0;
	
	private TaskExecutor(){
		threadsPool = Executors.newCachedThreadPool();
		futureMap = new HashMap<Long, Future<Object>>();
	}
	
	public TaskExecutor getExec(){
		return taskExecutor;
	}
	
	public static long scheduleTask(Task task){
		Future<Object> future = null;
		
		try{
			future = threadsPool.submit(task);
		}catch(RejectedExecutionException e){
			Log.e("SongSeeker", "Unable to submit task to the threads pool!", e);
			return -1;
		}
		
		futureMap.put(id++, future);
			
		return id-1;
	}
	
	public static Object getResult(long taskId) throws Exception{
		
		Future<Object> future = futureMap.get(taskId);
		Object ret = null;		
		
		futureMap.remove(taskId);
		ret = future.get();		
		
		return ret;		
	}
}
