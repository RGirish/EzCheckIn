package com.example.ezcheckin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject; 
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
 
public class FetchDataTaskTradeshow extends AsyncTask<String, Void, String>
{
    private final FetchDataListener listener;
    private String msg;
    public SQLiteDatabase db;
    public String Tname=null,idtradeshow=null;
    
    public FetchDataTaskTradeshow(FetchDataListener listener,SQLiteDatabase database) 
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
    protected void onPostExecute(String sJson) 
    {
    	if(sJson == null) 
        {
            if(listener != null) listener.onFetchFailure(msg);
            return;
        }
        try 
        {
           clearTradeshowTable();
           JSONArray aJson = new JSONArray(sJson);
           for(int i=0; i<aJson.length(); i++) 
           {
        	   JSONObject json = aJson.getJSONObject(i);
               Tname=json.getString("Tradeshowname");
               idtradeshow=json.getString("idtradeshow");
               db.execSQL("INSERT INTO tradeshows VALUES('"+idtradeshow+"','"+Tname+"');");
            }
            if(listener != null) listener.onFetchComplete("tradeshows");
        }
        catch (JSONException e) 
        {
            msg = "Invalid response";
            if(listener != null) listener.onFetchFailure(msg);
            return;
        }       
    }
    
    public void clearTradeshowTable()
    {
    	db.execSQL("DELETE FROM tradeshows;");
    }
        
    public String streamToString(final InputStream is) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder(); 
        String line = null;
         
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } 
        catch (IOException e) {
            throw e;
        } 
        finally {           
            try {
                is.close();
            } 
            catch (IOException e) {
                throw e;
            }
        }
        return sb.toString();
    }
    
}