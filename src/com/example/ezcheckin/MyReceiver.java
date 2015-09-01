
package com.example.ezcheckin;

import java.util.Calendar;
import java.util.Locale;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver
{
	private static NotificationManager mNotifyManager;
	public NotificationCompat.Builder mBuilder;
	private static int NOTIFICATION = 10002;
	public SharedPreferences session_info;
	public SharedPreferences.Editor editor;
	public String EzCheckInPrefs="ezcheckinprefs";
	public static SQLiteDatabase syncdb;
	public Context cc;
	public static int TOTAL_ROWS=0,COUNT=0;
	
	
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		cc=context;
		syncdb = context.openOrCreateDatabase("syncdata.db", SQLiteDatabase.CREATE_IF_NECESSARY, null);
		syncdb.setVersion(1);
		syncdb.setLocale(Locale.getDefault());
		
		ConnectivityManager connMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if(activeInfo!=null && activeInfo.isConnectedOrConnecting())
        {
        	syncNow();
        }
	}
	
	public void syncNow()
    {
		deleteRowsInServer();
		try
		{
			Cursor cc=syncdb.rawQuery("SELECT * FROM visitorproducts_new", null);
			cc.moveToFirst();
			cc.getString(0);
		}
		catch(Exception e)
		{
			return;
		}
		
		setLastSync();
		showNotification();
		
		//Get count of number of rows
		Cursor c10=syncdb.rawQuery("SELECT COUNT(idvisitor) FROM visitorproducts_new", null);
    	c10.moveToFirst();
		try
		{
			TOTAL_ROWS=Integer.parseInt(c10.getString(0));
		}
		catch(Exception e)
		{
			Log.i("Excep",e.toString());
			TOTAL_ROWS=0;
		}
		Cursor c11=syncdb.rawQuery("SELECT COUNT(lastscan) FROM lastscandata", null);
    	c11.moveToFirst();
		try
		{
			TOTAL_ROWS+=Integer.parseInt(c11.getString(0));
		}
		catch(Exception e){Log.i("Excep",e.toString());}
    	
		Log.e(Integer.toString(TOTAL_ROWS),Integer.toString(TOTAL_ROWS));
		
    	COUNT=0;
    	
    	
    	if(TOTAL_ROWS==0)
    	{
    		onFetchComplete("synccomplete");
    		return;
    	}
		
		//sync visitor product details
		Cursor c1=syncdb.rawQuery("SELECT * FROM visitorproducts_new", null);
		c1.moveToFirst();
		
		try
		{
			while(true)
			{
				String visitorid=c1.getString(0);
				String prodid=c1.getString(1);
				String idtradeshow=c1.getString(2);
				Log.e(visitorid,visitorid);
				Log.e(prodid,prodid);
				Log.e(idtradeshow,idtradeshow);
				BackgroundSyncDataTask task = new BackgroundSyncDataTask();
				task.execute("http://cardleadr.com/webservices/ez/addvisitortradeshowdetails.php?visitortradeshowdetails="+visitorid+"@"+prodid+"@"+idtradeshow);
				c1.moveToNext();
				if(c1.isAfterLast())break;
			}
		}
		catch(Exception e){}
        	
			
		//sync lastscan data
		Cursor c2=syncdb.rawQuery("SELECT * FROM lastscandata", null);
		c2.moveToFirst();
		try
		{
			while(true)
			{
        		String visitorid=c2.getString(0);
        		String lastscan=c2.getString(1);
        		BackgroundSyncDataTask task1 = new BackgroundSyncDataTask();
        		task1.execute("http://cardleadr.com/webservices/ez/updatelastscan.php?visitorscan="+visitorid+"@"+lastscan);
        		c2.moveToNext();
        		if(c2.isAfterLast())break;
        	}
        }
        catch(Exception e){}
    }
	
	public void deleteRowsInServer()
    {
    	try
    	{
    		Cursor c=syncdb.rawQuery("SELECT * FROM deletedata", null);
    		c.moveToFirst();
    		while(true)
    		{
    			String idvisitor=c.getString(0);
    			String idproduct=c.getString(1);
    			String idtradeshow=c.getString(2);
    			String visitorproductdtls=idvisitor+"@"+idproduct+"@"+idtradeshow;
				BackgroundDeleteVisitorProductTask task = new BackgroundDeleteVisitorProductTask();
				task.execute("http://cardleadr.com/webservices/ez/deletevisitorproduct.php?visitortradeshowdetails="+visitorproductdtls);
    			c.moveToNext();
    			if(c.isAfterLast()) break;
    		}
    	}
    	catch(Exception e){}
    }
	
	public void showNotification()
	{
		mNotifyManager = (NotificationManager) cc.getSystemService(Context.NOTIFICATION_SERVICE);
		mBuilder = new NotificationCompat.Builder(cc);
		mBuilder.setOngoing(true).setContentTitle("EzCheckIn").setContentText("Syncing in progress").setSmallIcon(R.drawable.notification_sync);
		mNotifyManager.notify(NOTIFICATION, mBuilder.build());
	}
	
	public static void stopNotification()
	{
		mNotifyManager.cancel(NOTIFICATION);
	}
	
	public static void onFetchComplete(String msg)
	{
		if(msg.equals("synccomplete"))
		{
			syncdb.execSQL("DELETE FROM visitorproducts_new;");
			syncdb.execSQL("DELETE FROM lastscandata;");
			COUNT=TOTAL_ROWS=0;
			stopNotification();
		}
		else if(msg.startsWith("delete:"))
		{
			String s=msg.substring(msg.lastIndexOf(":")+1);
			String[] vid_pid=s.split("@");
			syncdb.execSQL("DELETE FROM deletedata WHERE idvisitor='"+vid_pid[0]+"' AND idproduct='"+vid_pid[1]+"';");
		}
	}
	
	public static void onFetchFailure(String msg){}
	
	public void setLastSync()
    {
    	session_info = cc.getSharedPreferences(EzCheckInPrefs, 0);
		editor = session_info.edit();
		Calendar c=Calendar.getInstance();
		String lastsync="";
		
		int hours=c.get(Calendar.HOUR_OF_DAY);
		String hrs="";
		if(Integer.toString(hours).length()==1)
		{
			hrs="0"+Integer.toString(hours);
		}
		else
		{
			hrs=Integer.toString(hours);	
		}
		
		int minutes=c.get(Calendar.MINUTE);
		String mins="";
		if(Integer.toString(minutes).length()==1)
		{
			mins="0"+Integer.toString(minutes);
		}
		else
		{
			mins=Integer.toString(minutes);
		}
		
		int date=c.get(Calendar.DATE);
		int month=c.get(Calendar.MONTH);
		String Date=Integer.toString(date);
		
		lastsync+=Date;
		switch(month)
		{
		case 0:
			lastsync+=" Jan "+c.get(Calendar.YEAR)+", ";
			break;
		case 1:
			lastsync+=" Feb "+c.get(Calendar.YEAR)+", ";
			break;
		case 2:
			lastsync+=" Mar "+c.get(Calendar.YEAR)+", ";
			break;
		case 3:
			lastsync+=" Apr "+c.get(Calendar.YEAR)+", ";
			break;
		case 4:
			lastsync+=" May "+c.get(Calendar.YEAR)+", ";
			break;
		case 5:
			lastsync+=" Jun "+c.get(Calendar.YEAR)+", ";
			break;
		case 6:
			lastsync+=" Jul "+c.get(Calendar.YEAR)+", ";
			break;
		case 7:
			lastsync+=" Aug "+c.get(Calendar.YEAR)+", ";
			break;
		case 8:
			lastsync+=" Sep "+c.get(Calendar.YEAR)+", ";
			break;
		case 9:
			lastsync+=" Oct "+c.get(Calendar.YEAR)+", ";
			break;
		case 10:
			lastsync+=" Nov "+c.get(Calendar.YEAR)+", ";
			break;
		case 11:
			lastsync+=" Dec "+c.get(Calendar.YEAR)+", ";
			break;
		}
		
		lastsync+=hrs+":"+mins;
		editor.putString("lastsync", lastsync);
		Log.w(lastsync,lastsync);
		editor.commit();
    }
}
