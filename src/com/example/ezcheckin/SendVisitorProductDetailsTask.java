
package com.example.ezcheckin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
 
public class SendVisitorProductDetailsTask extends AsyncTask<String, Void, String>
{
    private final FetchDataListener listener;
    private String msg;
    public SQLiteDatabase db;
    public String Email=null,Password=null,idtradeshow=null;
    
    public SendVisitorProductDetailsTask(FetchDataListener listener,SQLiteDatabase database) 
    {
        this.listener = listener;
        db=database;
    }
     
    @Override
    protected String doInBackground(String... params)
    {
        if(params == null) return null;
        String url = params[0];        
        try 
        {
            HttpClient client = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            HttpResponse response = client.execute(httpget);
            HttpEntity entity = response.getEntity();

            if(entity == null) 
            {
                msg = "No response from the Server!";
                return null;        
            }
            else
            {
            	Log.i("asd","asd");
            }
            InputStream is = entity.getContent();
            return streamToString(is);
		}
        catch(IOException e)
        {
            msg = "Check your Network Connection!";
        }
         
        return null;
    }
     
    @Override
    protected void onPostExecute(String s) 
    {
    	if(s == null) 
    	{
            if(listener != null) listener.onFetchFailure(msg);
            return;
        }
        if(listener != null)
        {
        	listener.onFetchComplete(s);
        	return;
        }
    }
    
    public String streamToString(final InputStream inputStream)
    {
    	if (inputStream != null) 
		{
			Writer writer = new StringWriter();
			char[] buffer = new char[1024];

			try 
			{
				Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"),1024);
				int n;

				while ((n = reader.read(buffer)) != -1) 
				{
					writer.write(buffer, 0, n);
				}
				inputStream.close();
			}
			catch(Exception e)
			{
				return "inputstreamnull";
			}

			return writer.toString();
		}
		else 
		{
			return "inputstreamnull";
		}
    }
}