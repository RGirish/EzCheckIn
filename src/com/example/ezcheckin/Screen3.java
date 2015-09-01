
package com.example.ezcheckin;

import jim.h.common.android.zxinglib.integrator.IntentIntegrator;
import jim.h.common.android.zxinglib.integrator.IntentResult;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class Screen3 extends Activity implements FetchDataListener
{
	public int POSITION;
	public SQLiteDatabase db,syncdb;
	public String IDTRADESHOW;
	public String IDVISITOR;
	public String accepted;
	public static String SendResult="";
	public int Accepted=0,Pending=0;
	public Button save;
	public String LS="";
	public ProgressDialog dialog;
	public String fromcamera;
	public boolean from_go_button=false;
	public boolean allIsWell=true;
	public String alreadyGivenList="";
	public String[] givenList;
	public MediaPlayer mp;
	public boolean bulkmode;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		clearTempTable();
		setContentView(R.layout.screen3);
		setCustomActionBar();
		
		Intent i=getIntent();
		POSITION=i.getIntExtra("position",0);
		from_go_button=i.getBooleanExtra("from_go_button", false);
		IDVISITOR=Integer.toString(POSITION);
		fromcamera=i.getStringExtra("fromcamera");
		bulkmode=i.getBooleanExtra("bulkmode",false);
		if(bulkmode)
		{
			String bulkmodemessage=i.getStringExtra("bulkmodemessage");
			givenList=bulkmodemessage.split(";");
		}
		
		Log.i(IDVISITOR,IDVISITOR);
		IDTRADESHOW=i.getStringExtra("idtradeshow");
		
		db = openOrCreateDatabase("ezcheckin"+IDTRADESHOW+".db", SQLiteDatabase.CREATE_IF_NECESSARY, null);
		db.setVersion(1);
		db.setLocale(Locale.getDefault());
		
		syncdb = openOrCreateDatabase("syncdata.db", SQLiteDatabase.CREATE_IF_NECESSARY, null);
		syncdb.setVersion(1);
		syncdb.setLocale(Locale.getDefault());
		
		FontsOverride.setDefaultFont(this, "SANS_SERIF", "robotoregular.ttf");
		
		displayDetails();
		addListOfItems();
		
		if(fromcamera.equals("yes")||from_go_button) setCheckIn();
		
		if(fromcamera.equals("yes")||from_go_button) addSaveButton();
		else addEditButton();
		
	}
	
	public void reOpenScreen3(int p)
	{
		Intent intent=new Intent(this,Screen3.class);
		intent.putExtra("position",p);
		intent.putExtra("idtradeshow",IDTRADESHOW);
		intent.putExtra("fromcamera","no");
		startActivity(intent);
		dialog.dismiss();
		finish();
	}
	
	private int CheckConnection() 
	{
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo == null ) return 0;
        else return 1;
    }
	
	public void onBackPressed()
	{
		clearTempTable();
		super.onBackPressed();
	}
	
	public void clearTempTable()
	{
		try
		{
			db.execSQL("DELETE FROM temp;");
		}
		catch(Exception e){}
	}
	
	public void setLastScan()
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
		
		db.execSQL("UPDATE visitors SET lastscan='"+lastscan+"' WHERE id='"+IDVISITOR+"'");
		
		if(CheckConnection()==1)
		{
			Screen1.TOTAL_ROWS=1;
			Screen1.COUNT=0;
			SendSyncDataTask task1 = new SendSyncDataTask(this,db);
			task1.execute("http://cardleadr.com/webservices/ez/updatelastscan.php?visitorscan="+IDVISITOR+"@"+lastscan);
		}
		else
		{
			syncdb.execSQL("INSERT INTO lastscandata VALUES('"+IDVISITOR+"','"+lastscan+"');");
		}
	
	}
	
	public void addListOfItems()
	{
		Cursor c=db.rawQuery("SELECT name FROM products WHERE id='"+IDTRADESHOW+"'", null);
		c.moveToFirst();
		while(true)
		{
			String pname=c.getString(0);
			Log.i(pname,pname);
			addItem(pname);
			c.moveToNext();
			if(c.isAfterLast())break;
		}
	}
	
	public void setCheckIn()
	{
		boolean alreadyCheckedIn=false;
		Cursor c=db.rawQuery("SELECT idproduct FROM visitorproducts WHERE idvisitor='"+IDVISITOR+"' AND idproduct='999999999'", null);
		try
		{
			c.moveToFirst();
			c.getString(0);
			alreadyCheckedIn=true;
		}
		catch(Exception e){}
		
		if(!alreadyCheckedIn)
		{
			db.execSQL("INSERT INTO visitorproducts VALUES('"+IDVISITOR+"','999999999');");
			syncdb.execSQL("INSERT INTO visitorproducts_new VALUES('"+IDVISITOR+"','999999999','"+IDTRADESHOW+"');");
			if(checkConnection())
			{
				String visitorproductdtls=IDVISITOR+"@999999999";
				SendVisitorProductDetailsTask task = new SendVisitorProductDetailsTask(this,db);
				task.execute("http://cardleadr.com/webservices/ez/addvisitorproduct.php?visitorproductdtls="+visitorproductdtls+"&idtradeshow="+IDTRADESHOW);
			}
		}
	}
	
	
	public void displayDetails()
	{
		Cursor c=db.rawQuery("SELECT fn,ln,org,vid,lastscan FROM visitors WHERE id='"+Integer.toString(POSITION)+"'",null);
		c.moveToFirst();
		TextView name_tv=(TextView)findViewById(R.id.screen3_name);
		TextView work_tv=(TextView)findViewById(R.id.screen3_work);
		TextView vid_tv=(TextView)findViewById(R.id.screen3_vid);
		TextView lastscan_tv=(TextView)findViewById(R.id.screen3_lastscan);
		
		String fn=c.getString(0);
		String ln=c.getString(1);
		String work=c.getString(2);
		String vid=c.getString(3);
		String lastscan=c.getString(4);
		
		String name=fn+" "+ln;
		if(ln==null||ln.equals("null")||ln.length()==0)
		{
			name=fn;
		}
		if(fn==null||fn.equals("null")||fn.length()==0)
		{
			name=ln;
		}
		if((ln==null||ln.equals("null")||ln.length()==0)&&(fn==null||fn.equals("null")||fn.length()==0))
		{
			name="-";
		}
		if(work==null||work.equals("null")||work.length()==0)
		{
			work="-";
		}
		if(vid==null||vid.equals("null")||vid.length()==0)
		{
			vid="-";
		}
		
		name_tv.setText(name);
		work_tv.setText(work);
		vid_tv.setText(vid);
		if(lastscan.equals(null)||lastscan.equals("null")||lastscan.equals("")||lastscan.length()<=0)
		{
			lastscan_tv.setText("--:--");
			LS=lastscan;
		}
		else
		{
			String[] ls=lastscan.split("_");
			String time=ls[0]+" "+ls[1]+" "+ls[2]+", "+ls[3]+" ";
			lastscan_tv.setText(time);
		}
	}
	
	public void scan(View v)
	{
		IntentIntegrator.initiateScan(this, R.layout.capture, R.id.viewfinder_view, R.id.preview_view, true);
	}
	
	private boolean checkConnection() 
	{
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo == null ) return false;
        else return true;
    }
	
	public void onClickSave()
	{
		if(fromcamera.equals("yes")||from_go_button)setLastScan();
		Cursor cc=db.rawQuery("SELECT * FROM temp", null);
		cc.moveToFirst();
		try
		{
			while(true)
			{
				String idproduct=cc.getString(1);
				db.execSQL("INSERT INTO visitorproducts VALUES('"+IDVISITOR+"','"+idproduct+"');");
				syncdb.execSQL("INSERT INTO visitorproducts_new VALUES('"+IDVISITOR+"','"+idproduct+"','"+IDTRADESHOW+"');");
				cc.moveToNext();
				if(cc.isAfterLast())break;			
			}
			db.execSQL("DELETE FROM temp;");
		}
		catch(Exception e){}
		
		if(checkConnection())
		{
			Toast.makeText(this, "Successfully saved!", Toast.LENGTH_LONG).show();
			Cursor c=syncdb.rawQuery("SELECT * FROM visitorproducts_new", null);
			c.moveToFirst();
			String idvisitor=IDVISITOR;
			String visitorproductdtls=idvisitor+"@";
			try
			{
				while(true)
				{
					String idproduct=c.getString(1);
					visitorproductdtls=visitorproductdtls+idproduct+":";
					c.moveToNext();
					if(c.isAfterLast())break;
				}
			}
			catch(Exception e){}
			
			SendVisitorProductDetailsTask task = new SendVisitorProductDetailsTask(this,db);
			task.execute("http://cardleadr.com/webservices/ez/addvisitorproduct.php?visitorproductdtls="+visitorproductdtls+"&idtradeshow="+IDTRADESHOW);
			Log.i("visitorproductdtls",visitorproductdtls.substring(0, visitorproductdtls.length()-1));
		}
		else
		{
			Toast.makeText(this, "Saved to device!", Toast.LENGTH_LONG).show();
		}
		dialog = ProgressDialog.show(this,"","Refreshing...");
		reOpenScreen3(POSITION);
	}
	
	public void screen23_back(View v)
	{
		finish();
	}
	
	private void setCustomActionBar()
	{
		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getActionBar().setCustomView(R.layout.actionbar_screen23);
	}
	
	public void addItem(String TEXT)
	{
		
		boolean AlreadyAccepted=false;
		Cursor c=db.rawQuery("SELECT prodid FROM products WHERE name='"+TEXT+"'", null);
		c.moveToFirst();
		final String idproduct=c.getString(0);
		Log.i(idproduct,idproduct);
		Cursor c2=db.rawQuery("SELECT * FROM visitorproducts WHERE idvisitor='"+IDVISITOR+"' AND idproduct='"+idproduct+"'", null);
		try
		{
			c2.moveToFirst();
			String wtf="nothing";
			wtf=c2.getString(0);
			Log.i(wtf,wtf);
			Log.i("already accepted","inside try{}");
			AlreadyAccepted=true;
		}
		catch(Exception e)
		{
			Log.i("already accepted","inside catch()");
			AlreadyAccepted=false;
		}
		
		
		
		Log.i("wth","wth");
		int n40 = (int)getResources().getDimension(R.dimen.n40);
		int n8 = (int)getResources().getDimension(R.dimen.n8);
		int n2 = (int)getResources().getDimension(R.dimen.n2);
		int n30 = (int)getResources().getDimension(R.dimen.n30);
		int n20 = (int)getResources().getDimension(R.dimen.n20);
		
		LinearLayout items = (LinearLayout)findViewById(R.id.items);
		
		LinearLayout item = new LinearLayout(items.getContext());
		LayoutParams for_item = new LayoutParams(LayoutParams.MATCH_PARENT,n40);
		item.setBackgroundResource(R.drawable.list2);
		for_item.setMargins(n8,n2,n8,n2);
		item.setGravity(Gravity.CENTER|Gravity.LEFT);
		item.setLayoutParams(for_item);
		if(!AlreadyAccepted)
		{
			item.setTag("no");
			Pending++;
		}
		else
		{
			item.setTag("yes");
			Accepted++;
		}
		
		TextView acc=(TextView)findViewById(R.id.screen3_accepted);
		TextView tot=(TextView)findViewById(R.id.screen3_pending);
		acc.setText(Integer.toString(Pending));
		tot.setText(Integer.toString(Pending+Accepted));
		
		item.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View v) 
			{
				if(v.getTag().toString().equals("no"))
				{
					v.findViewWithTag("yesno").setBackgroundResource(R.drawable.yes);				
					TextView tv=(TextView)v.findViewWithTag("itemname");
					tv.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
					tv.setTextColor(getResources().getColor(R.color.green));
					v.setTag("yes");
					db.execSQL("INSERT INTO temp VALUES('"+IDVISITOR+"','"+idproduct+"');");
				}
				else
				{
					v.findViewWithTag("yesno").setBackgroundResource(R.drawable.no);
					TextView tv=(TextView)v.findViewWithTag("itemname");
					tv.setPaintFlags(tv.getPaintFlags()&(~Paint.STRIKE_THRU_TEXT_FLAG));
					tv.setTextColor(getResources().getColor(R.color.blue1));
					v.setTag("no");
					Cursor csr=db.rawQuery("SELECT idvisitor FROM temp WHERE idproduct='"+idproduct+"' AND idvisitor='"+IDVISITOR+"'", null);
					csr.moveToFirst();
					try
					{
						csr.getString(0);
						db.execSQL("DELETE FROM temp WHERE idvisitor='"+IDVISITOR+"' AND idproduct='"+idproduct+"';");
					}
					catch(Exception e)
					{
						db.execSQL("DELETE FROM visitorproducts WHERE idvisitor='"+IDVISITOR+"' AND idproduct='"+idproduct+"';");
						syncdb.execSQL("DELETE FROM visitorproducts_new WHERE idvisitor='"+IDVISITOR+"' AND idproduct='"+idproduct+"';");
						syncdb.execSQL("INSERT INTO deletedata VALUES('"+IDVISITOR+"','"+idproduct+"','"+IDTRADESHOW+"');");
						if(checkConnection())
						{
							String visitorproductdtls=IDVISITOR+"@"+idproduct+"@"+IDTRADESHOW;
							DeleteVisitorProductTask task = new DeleteVisitorProductTask(Screen3.this,db);
							task.execute("http://cardleadr.com/webservices/ez/deletevisitorproduct.php?visitortradeshowdetails="+visitorproductdtls);
						}	
					}
				}
			}
		});
		item.setEnabled(false);
		if(fromcamera.equals("yes")||from_go_button)
		{
			item.setEnabled(true);
		}
		items.addView(item);
		
		
		ImageView yesno = new ImageView(items.getContext());
		LayoutParams for_yesno = new LayoutParams(n30,n30);
		for_yesno.setMargins(2,0,0,0);
		yesno.setLayoutParams(for_yesno);
		yesno.setTag("yesno");
		item.addView(yesno);
		
		TextView itemname = new TextView(items.getContext());
		itemname.setText(TEXT);
		itemname.setTypeface(Typeface.SANS_SERIF ,Typeface.NORMAL);
		itemname.setTextSize(17);
		itemname.setTextColor(getResources().getColor(R.color.blue1));
		
		LayoutParams for_itemname = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		for_itemname.setMargins(n20,0,0,0);
		itemname.setLayoutParams(for_itemname);
		itemname.setTag("itemname");
		item.addView(itemname);
		
		
		
		if(!AlreadyAccepted)yesno.setBackgroundResource(R.drawable.no);
		else
		{
			yesno.setBackgroundResource(R.drawable.yes);
			TextView tv=(TextView)item.findViewWithTag("itemname");
			tv.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
			tv.setTextColor(getResources().getColor(R.color.green));
		}
		
		if(bulkmode)
		{
			Log.e("yolo","yolo");
			for(String s: givenList)
			{
				Log.e("yolowwwwww","yolowwwwww");
				if(s.equals(TEXT))
				{
					itemname.setTextColor(Color.RED);
					yesno.setBackgroundResource(R.drawable.yesred);
					Log.e("Its going in there. O.o","Its going in there. O.o");
				}
			}
		}
		
		items.invalidate();
		
	}
	
	public void addSaveButton()
	{
		int n275 = (int)getResources().getDimension(R.dimen.n275);
		int n45 = (int)getResources().getDimension(R.dimen.n45);
		int n10 = (int)getResources().getDimension(R.dimen.n10);
		LinearLayout items = (LinearLayout)findViewById(R.id.items);
		save=new Button(items.getContext());
		save.setBackgroundResource(R.drawable.buttonselector);
		LayoutParams bparams=new LayoutParams(n275,n45);
		bparams.setMargins(0, n10, 0, n10);
		save.setLayoutParams(bparams);
		save.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View arg0) 
			{
				onClickSave();
			}
		});
		items.addView(save);
	}
	
	public void addEditButton()
	{
		int n275 = (int)getResources().getDimension(R.dimen.n275);
		int n45 = (int)getResources().getDimension(R.dimen.n45);
		int n10 = (int)getResources().getDimension(R.dimen.n10);
		
		LinearLayout items = (LinearLayout)findViewById(R.id.items);
		save=new Button(items.getContext());
		save.setBackgroundResource(R.drawable.editbuttonselector);
		LayoutParams bparams=new LayoutParams(n275,n45);
		bparams.setMargins(0, n10, 0, n10);
		save.setLayoutParams(bparams);
		save.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View arg0) 
			{
				Intent intent=new Intent(Screen3.this,Screen3.class);
    			intent.putExtra("from_go_button",false);
    			intent.putExtra("position",Integer.parseInt(IDVISITOR));
    			intent.putExtra("fromcamera","yes");
    			intent.putExtra("bulkmode",false);
    			intent.putExtra("idtradeshow",IDTRADESHOW);
    			startActivity(intent);
    			finish();
			}
		});
		items.addView(save);
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
                if(Screen1.BULK_MODE)
                {
                	try
                	{
                		Log.e(result,result);
                	}
                	catch(Exception e)
                	{
                		finish();
                	}
                }
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
                			Intent intent=new Intent(this,Screen3.class);
                			intent.putExtra("from_go_button",false);
                			intent.putExtra("position",Integer.parseInt(id));
                			intent.putExtra("fromcamera","yes");
                			intent.putExtra("bulkmode",false);
                			intent.putExtra("idtradeshow",IDTRADESHOW);
                			startActivity(intent);
                			finish();
                		}
                		else
                		{
                			String returnString=fromBulkMode(id);
                			
                			if(returnString.equals("alliswell"))
                			{	
                				IntentIntegrator.initiateScan(this, R.layout.capture, R.id.viewfinder_view, R.id.preview_view, true);
                			}
                			else
                			{
                				Intent intent=new Intent(this,Screen3.class);
                    			intent.putExtra("from_go_button",false);
                    			intent.putExtra("position",Integer.parseInt(id));
                    			intent.putExtra("fromcamera","yes");
                    			intent.putExtra("idtradeshow",IDTRADESHOW);
                    			intent.putExtra("bulkmode",true);
                    			intent.putExtra("bulkmodemessage",returnString);
                    			startActivity(intent);
                    			finish();
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
	
	public String fromBulkMode(String id)
	{
		List<String> alreadyGiven = new ArrayList<String>();
		alreadyGiven.clear();
		allIsWell=true;
		alreadyGivenList="";
		
		setLastScan();
		setCheckIn();
		
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
	

	@Override
	public void onFetchComplete(){}

	@Override
	public void onFetchFailure(String msg){}

	@Override
	public void onFetchComplete(String string)
	{
		if(string.startsWith("done"))
		{
			String id=string.substring(string.indexOf(":")+1);
			syncdb.execSQL("DELETE FROM visitorproducts_new WHERE idvisitor='"+id+"';");
		}
		else if(string.startsWith("delete:"))
		{
			String s=string.substring(string.lastIndexOf(":")+1);
			String[] vid_pid=s.split("@");
			syncdb.execSQL("DELETE FROM deletedata WHERE idvisitor='"+vid_pid[0]+"' AND idproduct='"+vid_pid[1]+"';");
		}
	}
	
}
