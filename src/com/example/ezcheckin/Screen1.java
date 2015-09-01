
package com.example.ezcheckin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import jim.h.common.android.zxinglib.integrator.IntentIntegrator;
import jim.h.common.android.zxinglib.integrator.IntentResult;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;

public class Screen1 extends Activity implements FetchDataListener
{
	public static List<String> bulkModeList;
	public static int ITEM_NAME_ID=100;
	public static int COUNT=0,TOTAL_ROWS=0;
	public static boolean BULK_MODE=false;
	public SharedPreferences session_info;
	public SharedPreferences.Editor editor;
	public ProgressDialog dialog;
	public static ProgressDialog loading;
	public String IDTRADESHOW;
	public String SELECTED_EVENT;
	public SQLiteDatabase db,syncdb;
	public String no_present,no_reg;
	public String TOAST_MSG="";
	public String EzCheckInPrefs="ezcheckinprefs";
	public boolean allIsWell=true;
	public String alreadyGivenList="";
	public MediaPlayer mp;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.screen1);
		
		Intent i=getIntent();
		IDTRADESHOW=i.getStringExtra("idtradeshow");
		
		setSession();
		
		bulkModeList=new ArrayList<String>();
		
		//dialog = ProgressDialog.show(this,"","Please Wait..");
		
		createDatabases();
		
		FontsOverride.setDefaultFont(this, "SANS_SERIF", "robotoregular.ttf");
		setCustomActionBar();
		
		//syncNow();
		session_info = getSharedPreferences(EzCheckInPrefs, 0);
		
		if(session_info.getBoolean("firsttime", true))
		{
			session_info.edit().putBoolean("firsttime",false).commit();
			if(checkConnection())
			{
				dialog=ProgressDialog.show(this,null,"Please Wait..");
				downloadAllData();
			}
			else
			{
				Toast.makeText(this, "Check your network Connection!", Toast.LENGTH_LONG).show();
			}
		}
		else
		{
			displayAllData();
		}
		
		displayLastSync();
    	setBulkMode();
		
	}
	
	public void displayLastSync()
	{
		TextView ls=(TextView)findViewById(R.id.lastsync);
    	ls.setText("Last Sync was done on "+getLastSync());
	}
	
	public void setSession()
	{
		session_info = getSharedPreferences(EzCheckInPrefs, 0);
		editor = session_info.edit();
		editor.putString("idtradeshow", IDTRADESHOW);
		editor.commit();
	}
	
	public boolean checkConnection() 
	{
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo == null ) return false;
        else return true;
    }
	
	public void createDatabases()
	{
		db = openOrCreateDatabase("ezcheckin"+IDTRADESHOW+".db", SQLiteDatabase.CREATE_IF_NECESSARY, null);
		db.setVersion(1);
		db.setLocale(Locale.getDefault());
		
		syncdb = openOrCreateDatabase("syncdata.db", SQLiteDatabase.CREATE_IF_NECESSARY, null);
		syncdb.setVersion(1);
		syncdb.setLocale(Locale.getDefault());
		
		try
		{
			db.execSQL("CREATE TABLE tradeshows(id TEXT,name TEXT);");
		}
		catch(Exception e){Log.i("error","Login.java, createDatabases()");}
		try
		{
			db.execSQL("CREATE TABLE products(prodid TEXT,name TEXT, id TEXT);");
		}
		catch(Exception e){Log.i("error","Login.java, createDatabases()");}
		try
		{
			db.execSQL("CREATE TABLE visitors(id TEXT,fn TEXT, ln TEXT, org TEXT, vid TEXT, idtradeshow TEXT, lastscan TEXT);");
		}
		catch(Exception e){Log.e("error","Login.java, createDatabases()");}
		try
		{
			db.execSQL("CREATE TABLE visitorproducts(idvisitor TEXT,idproduct TEXT);");
		}
		catch(Exception e){Log.i("error","Login.java, createDatabases()");}
		try
		{
			syncdb.execSQL("CREATE TABLE visitorproducts_new(idvisitor TEXT,idproduct TEXT,idtradeshow TEXT);");
		}
		catch(Exception e){Log.i("error","Login.java, createDatabases()");}
		try
		{
			db.execSQL("CREATE TABLE temp(idvisitor TEXT,idproduct TEXT);");
		}
		catch(Exception e){Log.i("error","Login.java, createDatabases()");}
		try
		{
			syncdb.execSQL("CREATE TABLE lastscandata(idvisitor TEXT, lastscan TEXT);");
		}
		catch(Exception e){Log.i("error","Login.java, createDatabases()");}
		try
		{
			syncdb.execSQL("CREATE TABLE deletedata(idvisitor TEXT, idproduct TEXT,idtradeshow TEXT);");
		}
		catch(Exception e){Log.i("error","Login.java, createDatabases()");}
		
	}
	
	
	public void displayAllData()
	{
		//tradehsowdetails
		Cursor c=db.rawQuery("SELECT name FROM tradeshows WHERE id='"+IDTRADESHOW+"'", null);
		c.moveToFirst();
		String Tname=c.getString(0);
		TextView tv=(TextView) findViewById(R.id.my_event);
		tv.setText(Tname);
		
		//productdetails
		Cursor c3=db.rawQuery("SELECT name FROM products WHERE id='"+IDTRADESHOW+"'", null);
		c3.moveToFirst();
		while(true)
		{
			String pname=c3.getString(0);
			Log.i(pname,pname);
			
			
			Cursor c1=db.rawQuery("SELECT prodid FROM products WHERE name='"+pname+"'", null);
			c1.moveToFirst();
			String prodid=c1.getString(0);
			
			Cursor c2=db.rawQuery("SELECT COUNT(idvisitor) FROM visitorproducts WHERE idproduct='"+prodid+"'", null);
			c2.moveToFirst();
			
			String count="";
			try
			{
				count=c2.getString(0);
			}
			catch(Exception e)
			{
				count="0";
			}
			
			addAnItem(pname,prodid,count,false);
			c3.moveToNext();
			if(c3.isAfterLast())break;
		}
		
		//visitordetails
		
		Cursor c4=db.rawQuery("SELECT COUNT(id) FROM visitors WHERE idtradeshow='"+IDTRADESHOW+"'", null);
		c4.moveToFirst();
		no_reg=c4.getString(0);
		TextView reg=(TextView)findViewById(R.id.no_reg);
		reg.setText(no_reg);
		
		//visitorproductdetails
		
		Cursor c5=db.rawQuery("SELECT COUNT(idvisitor) FROM visitorproducts WHERE idproduct='999999999'",null);
		c5.moveToFirst();
		no_present=c5.getString(0);
		TextView chkdin=(TextView)findViewById(R.id.no_present);
		chkdin.setText(no_present);
		TextView absent=(TextView)findViewById(R.id.no_absent);
		absent.setText(Integer.toString(Integer.parseInt(no_reg)-Integer.parseInt(no_present)));
		setAttendaceBar(Integer.parseInt(no_reg),Integer.parseInt(no_present));
		
	}
	
	
	public void downloadAllData()
	{
		if(checkConnection())
		{
			FetchDataTaskTradeshow task1 = new FetchDataTaskTradeshow(this,db);
			task1.execute("http://cardleadr.com/webservices/ez/tradeshow.php?idtradeshow="+IDTRADESHOW);
		}
		else
		{
			//tradehsowdetails
			Cursor c=db.rawQuery("SELECT name FROM tradeshows WHERE id='"+IDTRADESHOW+"'", null);
    		c.moveToFirst();
    		String Tname=c.getString(0);
    		TextView tv=(TextView) findViewById(R.id.my_event);
    		tv.setText(Tname);
			
    		//productdetails
    		Cursor c3=db.rawQuery("SELECT name FROM products WHERE id='"+IDTRADESHOW+"'", null);
    		c3.moveToFirst();
    		while(true)
    		{
    			String pname=c3.getString(0);
    			Log.i(pname,pname);
    			
    			
    			Cursor c1=db.rawQuery("SELECT prodid FROM products WHERE name='"+pname+"'", null);
    			c1.moveToFirst();
    			String prodid=c1.getString(0);
    			
    			Cursor c2=db.rawQuery("SELECT COUNT(idvisitor) FROM visitorproducts WHERE idproduct='"+prodid+"'", null);
    			c2.moveToFirst();
    			
    			String count="";
    			try
    			{
    				count=c2.getString(0);
    			}
    			catch(Exception e)
    			{
    				count="0";
    			}
    			
    			addAnItem(pname,prodid,count,false);
    			c3.moveToNext();
    			if(c3.isAfterLast())break;
    		}
    		
    		//visitordetails
    		
    		Cursor c4=db.rawQuery("SELECT COUNT(id) FROM visitors WHERE idtradeshow='"+IDTRADESHOW+"'", null);
    		c4.moveToFirst();
    		no_reg=c4.getString(0);
    		TextView reg=(TextView)findViewById(R.id.no_reg);
    		reg.setText(no_reg);
    		
    		//visitorproductdetails
    		
    		Cursor c5=db.rawQuery("SELECT COUNT(idvisitor) FROM visitorproducts WHERE idproduct='999999999'",null);
    		c5.moveToFirst();
    		no_present=c5.getString(0);
    		TextView chkdin=(TextView)findViewById(R.id.no_present);
    		chkdin.setText(no_present);
    		TextView absent=(TextView)findViewById(R.id.no_absent);
    		absent.setText(Integer.toString(Integer.parseInt(no_reg)-Integer.parseInt(no_present)));
    		setAttendaceBar(Integer.parseInt(no_reg),Integer.parseInt(no_present));
    		dialog.dismiss();
			
		}
	}
	
	
	public void onClickLogout(View v)
	{
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		dialog.setMessage("Are you sure you want to Logout?");
		dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes",
		new DialogInterface.OnClickListener()
		{		
			public void onClick(DialogInterface dialog, int which) 
			{
				session_info = getSharedPreferences(EzCheckInPrefs, 0);
				editor = session_info.edit();
				editor.putString("idtradeshow", "NONE");
				editor.commit();
				finish();
			}
		});
		
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No",
		new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		});
		dialog.show();
	}
	
	public void setCustomActionBar()
	{
		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getActionBar().setCustomView(R.layout.actionbar_screen1);
	}
	
	public void setBulkMode()
	{
		if(bulkModeList.isEmpty()) BULK_MODE=false;
		else BULK_MODE=true;
	}
	
	public void addAnItem(String text,String prodid,String count, boolean IS_LAST)
	{
		int n42 = (int)getResources().getDimension(R.dimen.n42);
		int n5 = (int)getResources().getDimension(R.dimen.n5);
		int n2 = (int)getResources().getDimension(R.dimen.n2);
		int n10 = (int)getResources().getDimension(R.dimen.n10);
		int n30 = (int)getResources().getDimension(R.dimen.n30);
		int n3 = (int)getResources().getDimension(R.dimen.n3);
		int n35 = (int)getResources().getDimension(R.dimen.n35);
		int n4 = (int)getResources().getDimension(R.dimen.n4);
		int n230 = (int)getResources().getDimension(R.dimen.n230);

		LinearLayout list=(LinearLayout)findViewById(R.id.the_list);
		
		RelativeLayout item=new RelativeLayout(list.getContext());
		item.setBackgroundResource(R.drawable.list);
		item.setGravity(Gravity.CENTER_VERTICAL);
		LayoutParams params=new LayoutParams(LayoutParams.MATCH_PARENT,n42);
		item.setLayoutParams(params);
		item.setGravity(Gravity.CENTER);
		item.setTag("no");
		
		item.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View v) 
			{
				if(v.getTag().toString().equals("no"))
				{
					v.findViewWithTag("yesno").setBackgroundResource(R.drawable.yes);				
					v.setTag("yes");
					TextView id=(TextView) v.findViewWithTag("prodid");
					bulkModeList.add(id.getText().toString());
				}
				else
				{
					v.findViewWithTag("yesno").setBackgroundResource(R.drawable.no);				
					v.setTag("no");
					TextView id=(TextView) v.findViewWithTag("prodid");
					bulkModeList.remove(id.getText().toString());
				}
				setBulkMode();			
			}
		});
		
		list.addView(item);
		
		ImageView yesno = new ImageView(list.getContext());
		LayoutParams for_yesno = new LayoutParams(n30,n30);
		for_yesno.setMargins(n2,0,n5,0);
		yesno.setLayoutParams(for_yesno);
		yesno.setTag("yesno");
		yesno.setBackgroundResource(R.drawable.no);
		item.addView(yesno);
		
		TextView tv1=new TextView(item.getContext());
		tv1.setText(text);
		tv1.setTextSize(17);
		tv1.setMaxLines(1);
		tv1.setEllipsize(TruncateAt.END);
		tv1.setTextColor(getResources().getColor(R.color.blue1));
		LayoutParams tv1p=new LayoutParams(n230,LayoutParams.WRAP_CONTENT);
		tv1p.setMargins(n35,n4,0,0);
		tv1.setLayoutParams(tv1p);
		tv1.setId(Screen1.ITEM_NAME_ID);
		tv1.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
		item.addView(tv1);
		
		TextView idtv=new TextView(item.getContext());
		idtv.setText(prodid);
		idtv.setTag("prodid");
		idtv.setVisibility(View.GONE);
		item.addView(idtv);
		
		TextView tv2=new TextView(item.getContext());
		tv2.setText(count);
		tv2.setTextSize(16);
		tv2.setTextColor(getResources().getColor(R.color.blue1));
		tv2.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
		LayoutParams tv2p=new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		tv2p.setMargins(0,n4,n10,0);
		tv2p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		tv2p.addRule(RelativeLayout.ALIGN_BOTTOM, ITEM_NAME_ID);
		tv2p.addRule(RelativeLayout.ALIGN_BASELINE, ITEM_NAME_ID);
		tv2.setLayoutParams(tv2p);
		item.addView(tv2);
		
		ITEM_NAME_ID++;
		
		android.view.ViewGroup.LayoutParams params1=item.getLayoutParams();
		if(!IS_LAST) ((MarginLayoutParams) params1).setMargins(n5,n3,n5,n3);
		else ((MarginLayoutParams) params1).setMargins(n5,n2,n5,n10);
		item.setLayoutParams(params1);
		
	}
	
	public void setAttendaceBar(int tot,int pre)
	{
		Button bar = (Button)findViewById(R.id.bar);
		float percent = (pre*100)/tot;
		TextView percentage = (TextView)findViewById(R.id.atten_per);
		percentage.setText(Integer.toString((int)percent)+"%");
		float pixels = getResources().getDimension(R.dimen.n150);
		pixels = (float)(pixels * percent * 0.01);
		LinearLayout parent_of_bar=(LinearLayout)findViewById(R.id.parent_of_bar);
		LinearLayout.LayoutParams pob_params=(LinearLayout.LayoutParams) parent_of_bar.getLayoutParams();
		LinearLayout.LayoutParams buttnbar = new LinearLayout.LayoutParams((int)pixels,pob_params.height-2);
		buttnbar.setMargins(1, 0, 0, 0);
		bar.setLayoutParams(buttnbar);
	}

	public void screen2(View v)
	{
		loading = ProgressDialog.show(this,"","Loading..");
		Intent intent=new Intent(this,Screen2.class);
		intent.putExtra("selected event",SELECTED_EVENT);
		intent.putExtra("idtradeshow",IDTRADESHOW);
		startActivity(intent);
	}
	
	
	
	public void onFetchComplete(){}
	
	public void onFetchComplete(String msg)
    {
    	if(msg.equals("tradeshows"))
    	{
    		Cursor c=db.rawQuery("SELECT name FROM tradeshows WHERE id='"+IDTRADESHOW+"'", null);
    		c.moveToFirst();
    		String Tname=c.getString(0);
    		TextView tv=(TextView) findViewById(R.id.my_event);
    		tv.setText(Tname);
    		
    		FetchDataTaskVisitors task3 = new FetchDataTaskVisitors(this,db);
			task3.execute("http://cardleadr.com/webservices/ez/visitordetails.php?idtradeshow="+IDTRADESHOW);
			
    		
    	}
    	else if(msg.equals("products"))
    	{
    		Cursor c=db.rawQuery("SELECT name FROM products WHERE id='"+IDTRADESHOW+"'", null);
    		c.moveToFirst();
    		LinearLayout list=(LinearLayout)findViewById(R.id.the_list);
			list.removeAllViews();
    		while(true)
    		{
    			String pname=c.getString(0);
    			Log.i(pname,pname);
    			
    			
    			Cursor c1=db.rawQuery("SELECT prodid FROM products WHERE name='"+pname+"'", null);
    			c1.moveToFirst();
    			String prodid=c1.getString(0);
    			
    			Cursor c2=db.rawQuery("SELECT COUNT(idvisitor) FROM visitorproducts WHERE idproduct='"+prodid+"'", null);
    			c2.moveToFirst();
    			
    			String count="";
    			try
    			{
    				count=c2.getString(0);
    			}
    			catch(Exception e)
    			{
    				count="0";
    			}
    			addAnItem(pname,prodid,count,false);
    			c.moveToNext();
    			if(c.isAfterLast())break;
    		}
    		dialog.dismiss();
    		setLastSync();
    		displayLastSync();
    		Toast.makeText(this, "Sync Complete!", Toast.LENGTH_LONG).show();
   		}
    	else if(msg.equals("visitors"))
    	{
    		Cursor c4=db.rawQuery("SELECT COUNT(id) FROM visitors WHERE idtradeshow='"+IDTRADESHOW+"'", null);
    		c4.moveToFirst();
    		no_reg=c4.getString(0);
    		TextView reg=(TextView)findViewById(R.id.no_reg);
    		reg.setText(no_reg);
    	
    		FetchDataTaskVisitorProducts task4 = new FetchDataTaskVisitorProducts(this,db);
			task4.execute("http://cardleadr.com/webservices/ez/visitorproductdetails.php?idtradeshow="+IDTRADESHOW);
    		
    	}
    	else if(msg.equals("visitorproducts"))
    	{
    		Cursor c5=db.rawQuery("SELECT COUNT(idvisitor) FROM visitorproducts WHERE idproduct='999999999'",null);
    		c5.moveToFirst();
    		no_present=c5.getString(0);
    		TextView chkdin=(TextView)findViewById(R.id.no_present);
    		chkdin.setText(no_present);
    		TextView absent=(TextView)findViewById(R.id.no_absent);
    		absent.setText(Integer.toString(Integer.parseInt(no_reg)-Integer.parseInt(no_present)));
    		setAttendaceBar(Integer.parseInt(no_reg),Integer.parseInt(no_present));
    		
    		FetchDataTaskProducts task2 = new FetchDataTaskProducts(this,db);
			task2.execute("http://cardleadr.com/webservices/ez/tradeshowproductdetails.php?idtradeshow="+IDTRADESHOW);
    	
    	}
    	else if(msg.equals("synccomplete"))
    	{
    		Log.i("SyncData","inside onFetchComplete for synccomplete");
    		syncdb.execSQL("DELETE FROM visitorproducts_new;");
    		syncdb.execSQL("DELETE FROM lastscandata;");
    		COUNT=TOTAL_ROWS=0;
    		downloadAllData();
    	}
    	else if(msg.startsWith("delete:"))
		{
			String s=msg.substring(msg.lastIndexOf(":")+1);
			String[] vid_pid=s.split("@");
			syncdb.execSQL("DELETE FROM deletedata WHERE idvisitor='"+vid_pid[0]+"' AND idproduct='"+vid_pid[1]+"';");
		}
    	else
    	{
    		dialog.dismiss();
    		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    	}
    }
	
    public void onFetchFailure(String msg)
    {
    	Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    	Log.i("onfetchfailure","onfetchfailure");
    	dialog.dismiss();
    }
    
    
    public String getLastSync()
    {
    	session_info = getSharedPreferences(EzCheckInPrefs, 0);
    	return session_info.getString("lastsync", "----");
    }
    
    public void setLastSync()
    {
    	session_info = getSharedPreferences(EzCheckInPrefs, 0);
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
    
    public void onClickSyncNow(View v)
    {
    	dialog = ProgressDialog.show(this,"","Please Wait..");
    	syncNow();
    }
    
    
    public void syncNow()
    {
    	if(checkConnection())
    	{
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
        	
        	setLastSync();
        	deleteRowsInServer();
        	
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
        			SendSyncDataTask task = new SendSyncDataTask(this,db);
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
        			SendSyncDataTask task1 = new SendSyncDataTask(this,db);
        			task1.execute("http://cardleadr.com/webservices/ez/updatelastscan.php?visitorscan="+visitorid+"@"+lastscan);
        			c2.moveToNext();
        			if(c2.isAfterLast())break;
        		}
        	}
        	catch(Exception e){}
        	
    	}
    	else
    	{
    		Toast.makeText(this, "Check your Network Connection!", Toast.LENGTH_LONG).show();
    		downloadAllData();
    	}
    }
    
    public void deleteRowsInServer()
    {
    	Cursor c=syncdb.rawQuery("SELECT * FROM deletedata", null);
    	try
    	{
    		c.moveToFirst();
    		while(true)
    		{
    			String idvisitor=c.getString(0);
    			String idproduct=c.getString(1);
    			String idtradeshow=c.getString(2);
    			String visitorproductdtls=idvisitor+"@"+idproduct+"@"+idtradeshow;
				DeleteVisitorProductTask task = new DeleteVisitorProductTask(this,db);
				task.execute("http://cardleadr.com/webservices/ez/deletevisitorproduct.php?visitortradeshowdetails="+visitorproductdtls);
    			c.moveToNext();
    			if(c.isAfterLast()) break;
    		}
    	}
    	catch(Exception e){}
    }
    
    public void onClickScan(View v)
    {
    	IntentIntegrator.initiateScan(this, R.layout.capture, R.id.viewfinder_view, R.id.preview_view, true);
    }
    
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) 
        {
            case IntentIntegrator.REQUEST_CODE:
                
            	IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                if (scanResult == null) {return;}
                
                final String result = scanResult.getContents();
                if (result != null) 
                {
                	Cursor c=db.rawQuery("SELECT id FROM visitors WHERE vid='"+result+"'", null);
                	c.moveToFirst();
                	
                	try
                	{
                		String id=c.getString(0);
                		
                		if(!Screen1.BULK_MODE)
                		{
                			playAllIsWellTone();
                			Intent intent=new Intent(Screen1.this,Screen3.class);
                			intent.putExtra("from_go_button",false);
                			intent.putExtra("position",Integer.parseInt(id));
                			intent.putExtra("fromcamera","yes");
                			intent.putExtra("bulkmode",false);
                			intent.putExtra("idtradeshow",IDTRADESHOW);
                			startActivity(intent);
                		}
                		else
                		{
                			String returnString=fromBulkMode(id);
                			
                			if(returnString.equals("alliswell"))
                			{	
                				IntentIntegrator.initiateScan(Screen1.this, R.layout.capture, R.id.viewfinder_view, R.id.preview_view, true);
                			}
                			else
                			{
                				Intent intent=new Intent(Screen1.this,Screen3.class);
                    			intent.putExtra("from_go_button",false);
                    			intent.putExtra("position",Integer.parseInt(id));
                    			intent.putExtra("fromcamera","yes");
                    			intent.putExtra("idtradeshow",IDTRADESHOW);
                    			intent.putExtra("bulkmode",true);
                    			intent.putExtra("bulkmodemessage",returnString);
                    			startActivity(intent);
                			}
                		}
                	}
                	catch(Exception e)
                	{
                		playAllIsNotWellTone();
                		Toast.makeText(this, "Show a valid Visitor Badge!", Toast.LENGTH_LONG).show();
                	}
                }
                break;
            default:
        }
    }
    
    
    public void setLastScan(String id)
	{
		Calendar c=Calendar.getInstance();
		String firstpart="",secondpart="",lastscan="";
		
		int date=c.get(Calendar.DATE);
		int month=c.get(Calendar.MONTH);
		String Date=Integer.toString(date);
		
		firstpart+=Date;
		switch(month)
		{
		case 0:
			firstpart+="_Jan_"+c.get(Calendar.YEAR);
			break;
		case 1:
			firstpart+="_Feb_"+c.get(Calendar.YEAR);
			break;
		case 2:
			firstpart+="_Mar_"+c.get(Calendar.YEAR);
			break;
		case 3:
			firstpart+="_Apr_"+c.get(Calendar.YEAR);
			break;
		case 4:
			firstpart+="_May_"+c.get(Calendar.YEAR);
			break;
		case 5:
			firstpart+="_Jun_"+c.get(Calendar.YEAR);
			break;
		case 6:
			firstpart+="_Jul_"+c.get(Calendar.YEAR);
			break;
		case 7:
			firstpart+="_Aug_"+c.get(Calendar.YEAR);
			break;
		case 8:
			firstpart+="_Sep_"+c.get(Calendar.YEAR);
			break;
		case 9:
			firstpart+="_Oct_"+c.get(Calendar.YEAR);
			break;
		case 10:
			firstpart+="_Nov_"+c.get(Calendar.YEAR);
			break;
		case 11:
			firstpart+="_Dec_"+c.get(Calendar.YEAR);
			break;
		}
		
		int hours=c.get(Calendar.HOUR_OF_DAY);
		int minutes=c.get(Calendar.MINUTE);
		String hrs=Integer.toString(hours);
		if(hrs.length()==1)hrs="0"+hrs;
		String mins=Integer.toString(minutes);
		if(mins.length()==1)mins="0"+mins;
		secondpart+=hrs+":"+mins;
		
		lastscan=firstpart+"_"+secondpart;
		
		db.execSQL("UPDATE visitors SET lastscan='"+lastscan+"' WHERE id='"+id+"'");
		
		if(checkConnection())
		{
			Screen1.TOTAL_ROWS=1;
			Screen1.COUNT=0;
			SendSyncDataTask task1 = new SendSyncDataTask(this,db);
			task1.execute("http://cardleadr.com/webservices/ez/updatelastscan.php?visitorscan="+id+"@"+lastscan);
		}
		else
		{
			syncdb.execSQL("INSERT INTO lastscandata VALUES('"+id+"','"+lastscan+"');");
		}
	
	}
	
	public void setCheckIn(String id)
	{
		boolean alreadyCheckedIn=false;
		Cursor c=db.rawQuery("SELECT idproduct FROM visitorproducts WHERE idvisitor='"+id+"' AND idproduct='999999999'", null);
		try
		{
			c.moveToFirst();
			c.getString(0);
			alreadyCheckedIn=true;
		}
		catch(Exception e){}
		
		if(!alreadyCheckedIn)
		{
			db.execSQL("INSERT INTO visitorproducts VALUES('"+id+"','999999999');");
			syncdb.execSQL("INSERT INTO visitorproducts_new VALUES('"+id+"','999999999','"+IDTRADESHOW+"');");
			if(checkConnection())
			{
				String visitorproductdtls=id+"@999999999";
				SendVisitorProductDetailsTask task = new SendVisitorProductDetailsTask(this,db);
				task.execute("http://cardleadr.com/webservices/ez/addvisitorproduct.php?visitorproductdtls="+visitorproductdtls+"&idtradeshow="+IDTRADESHOW);
			}
		}
	}
    
    
    public String fromBulkMode(String id)
	{
		List<String> alreadyGiven = new ArrayList<String>();
		alreadyGiven.clear();
		allIsWell=true;
		alreadyGivenList="";
		
		setLastScan(id);
		setCheckIn(id);
		
		try
		{
			//c has the list of all the prodid's got by person id
			Cursor c=db.rawQuery("SELECT idproduct FROM visitorproducts WHERE idvisitor="+id, null);
			
			for(String prodid : Screen1.bulkModeList)
			{
				boolean flag=false;
				c.moveToFirst();
				do
				{
					String s=c.getString(0);
					if(s.equals(prodid))
					{
						flag=true;
						break;
					}
					c.moveToNext();
					if(c.isAfterLast())break;
				}while(true);
				
				//prodid was already given
				if(flag==true)
				{
					allIsWell=false;
					alreadyGiven.add(prodid);
				}
				else //prodid was not given already
				{
					//here, alliswell is still true(it was set as true at the start of this function)
					db.execSQL("INSERT INTO visitorproducts VALUES('"+id+"','"+prodid+"');");
					syncdb.execSQL("INSERT INTO visitorproducts_new VALUES('"+id+"','"+prodid+"','"+IDTRADESHOW+"');");
				}
			}
			
			if(checkConnection())
			{
				Cursor cr=syncdb.rawQuery("SELECT * FROM visitorproducts_new", null);
				cr.moveToFirst();
				String idvisitor=id;
				String visitorproductdtls=idvisitor+"@";
				try
				{
					while(true)
					{
						String idproduct=cr.getString(1);
						visitorproductdtls=visitorproductdtls+idproduct+":";
						cr.moveToNext();
						if(cr.isAfterLast())break;
					}
				}
				catch(Exception e){}
				
				SendVisitorProductDetailsTask task = new SendVisitorProductDetailsTask(this,db);
				task.execute("http://cardleadr.com/webservices/ez/addvisitorproduct.php?visitorproductdtls="+visitorproductdtls+"&idtradeshow="+IDTRADESHOW);
			}
			
		}
		catch(Exception e)
		{
			Log.e("ERROR",e.toString());
		}
		
		String returnString;
		if(allIsWell)
		{
			playAllIsWellTone();
			returnString="alliswell";
		}
		else
		{
			playAllIsNotWellTone();
			for(String prodid:alreadyGiven)
			{
				Cursor c2=db.rawQuery("SELECT name FROM products WHERE prodid='"+prodid+"'", null);
				c2.moveToFirst();
				alreadyGivenList+=c2.getString(0)+";";
			}
			returnString=alreadyGivenList.substring(0, alreadyGivenList.length()-1);
		}
		
		return returnString;
	}
	
	public void playAllIsWellTone()
	{
		mp=MediaPlayer.create(this, R.raw.accept);
		mp.start();
	}
	
	public void playAllIsNotWellTone()
	{
		mp=MediaPlayer.create(this, R.raw.reject);
		mp.start();
	}
    
    
    
    
    
}
