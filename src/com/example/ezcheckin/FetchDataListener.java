package com.example.ezcheckin;

public interface FetchDataListener 
{
	public void onFetchComplete();
    public void onFetchFailure(String msg);
	public void onFetchComplete(String string);
}