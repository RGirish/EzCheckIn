
package com.example.ezcheckin;

import jim.h.common.android.zxinglib.integrator.IntentIntegrator;
import jim.h.common.android.zxinglib.integrator.IntentResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class Screen2 extends Activity implements FetchDataListener
{
	public SQLiteDatabase db,syncdb;
	public String IDTRADESHOW;
	public EditText searchbar;
	public Context cc;
	public static String EVENT_NAME;
	public int POSITION=1;
	public boolean allIsWell=true;
	public String alreadyGivenList="";
	public MediaPlayer mp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.screen2);
		setCustomActionBar();
		searchbar=(EditText)findViewById(R.id.search);
		searchbar.setImeOptions(EditorInfo.IME_ACTION_GO);
		searchbar.setOnEditorActionListener(new OnEditorActionListener() 
		{   
			@Override
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2)
			{
				onClickGo();
				return true;
			}
		});
		cc=this;
		
		Intent i=getIntent();
		IDTRADESHOW=i.getStringExtra("idtradeshow");
		
		db = openOrCreateDatabase("ezcheckin"+IDTRADESHOW+".db", SQLiteDatabase.CREATE_IF_NECESSARY, null);
		db.setVersion(1);
		db.setLocale(Locale.getDefault());
		
		syncdb = openOrCreateDatabase("syncdata.db", SQLiteDatabase.CREATE_IF_NECESSARY, null);
		syncdb.setVersion(1);
		syncdb.setLocale(Locale.getDefault());
		
		FontsOverride.setDefaultFont(this, "MONOSPACE", "robotomedium.ttf");
		FontsOverride.setDefaultFont(this, "SANS_SERIF", "arial.ttf");
		
		Intent thisintent=getIntent();
		EVENT_NAME=thisintent.getStringExtra("selected event");
				
		displayNameList();
		
		Screen1.loading.dismiss();
		
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		ViewGroup mainLayout=(LinearLayout)findViewById(R.id.name_list);
		mainLayout.invalidate();
		mainLayout.requestLayout();
	}
	
	public void displayNameList()
	{
		Cursor c=db.rawQuery("SELECT * FROM visitors", null);
		c.moveToFirst();
		while(true)
		{
			String id=c.getString(0);
			String fn=c.getString(1);
			String ln=c.getString(2);
			String org=c.getString(3);
			String vid=c.getString(4);
			String lastscan=c.getString(6);
			String[] ls=null;
			String time="";
			if( lastscan!=null && lastscan.length()>0 && !lastscan.equals("") )
			{
				try
				{
				ls=lastscan.split("_");
				time=ls[3];
				}catch(Exception e){lastscan=null;ls=null;}
			}
		
			String name="";
			name=fn+" "+ln;
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
			if(org==null||org.equals("null")||org.length()==0)
			{
				org="-";
			}
			if(ls==null||ls.length<=1) addAnItem(name,org,null,vid,Integer.parseInt(id));
			else addAnItem(name,org,time,vid,Integer.parseInt(id));
			c.moveToNext();
			if(c.isAfterLast())break;
		}
	}
	
	public void screen23_back(View v)
	{
		finish();
	}
	
	public void onClickGo()
	{ 
		String vid=searchbar.getText().toString();
		
		if(vid.length()<=0)
		{
			searchbar.setText("");
			Toast.makeText(this, "Enter a valid Visitor Id!", Toast.LENGTH_LONG).show();
			return;
		}
		
		try
		{
			Cursor c=db.rawQuery("SELECT id FROM visitors WHERE vid='"+vid+"'", null);
			c.moveToFirst();
			String idvisitor=c.getString(0);
			searchbar.setText("");
			openScreen3(Integer.parseInt(idvisitor),true);
		}
		catch(Exception e)
		{
			searchbar.setText("");
			Toast.makeText(this, "Enter a valid Visitor Id!", Toast.LENGTH_LONG).show();
		}
	}
	
	public void onClickGo(View v)
	{
		searchbar=(EditText)findViewById(R.id.search); 
		String vid=searchbar.getText().toString();
		
		if(vid.length()<=0)
		{
			searchbar.setText("");
			Toast.makeText(this, "Enter a valid Visitor Id!", Toast.LENGTH_LONG).show();
			return;
		}
		
		try
		{
			Cursor c=db.rawQuery("SELECT id FROM visitors WHERE vid='"+vid+"'", null);
			c.moveToFirst();
			String idvisitor=c.getString(0);
			searchbar.setText("");
			openScreen3(Integer.parseInt(idvisitor),true);
		}
		catch(Exception e)
		{
			searchbar.setText("");
			Toast.makeText(this, "Enter a valid Visitor Id!", Toast.LENGTH_LONG).show();
		}
	}
	
	public void scan(View v)
	{
		IntentIntegrator.initiateScan(Screen2.this, R.layout.capture, R.id.viewfinder_view, R.id.preview_view, true);
	}
	
	private void setCustomActionBar()
	{
		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getActionBar().setCustomView(R.layout.actionbar_screen23);
	}
	
	private void addLine()
	{
		LinearLayout ll=(LinearLayout)findViewById(R.id.name_list);
		Button line=new Button(ll.getContext());
		line.setBackgroundColor(getResources().getColor(R.color.gray2));
		LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,1);
		lp.setMargins(5, 0, 5, 0);
		line.setLayoutParams(lp);
		ll.addView(line);
	}
	
	public void openScreen3(int p, boolean from_go_button)
	{
		Intent intent=new Intent(cc,Screen3.class);
		if(from_go_button) intent.putExtra("from_go_button",true);
		else intent.putExtra("from_go_button",false);
		intent.putExtra("position",p);
		intent.putExtra("idtradeshow",IDTRADESHOW);
		intent.putExtra("bulkmode",false);
		intent.putExtra("fromcamera","no");
		startActivity(intent);
	}
	
	@SuppressWarnings("unused")
	public boolean nothingsThere(String idvisitor)
	{
		Cursor c=db.rawQuery("SELECT idproduct FROM visitorproducts WHERE idvisitor='"+idvisitor+"'",null);
		try
		{
			c.moveToFirst();
			String s=c.getString(0);
		}
		catch(Exception e)
		{
			return true;
		}
		return false;
	}
	
	public void addAnItem(String Name,String work,String time,String id, final int position)
	{
		
		LinearLayout list = (LinearLayout)findViewById(R.id.name_list);
		
		int n5 = (int)getResources().getDimension(R.dimen.n5);
		int n170 = (int)getResources().getDimension(R.dimen.n170);
		int n20 = (int)getResources().getDimension(R.dimen.n20);
		int n30 = (int)getResources().getDimension(R.dimen.n30);
		int n35 = (int)getResources().getDimension(R.dimen.n35);
		int n60 = (int)getResources().getDimension(R.dimen.n60);
		int n15 = (int)getResources().getDimension(R.dimen.n15);
		
		RelativeLayout item = new RelativeLayout(list.getContext());
		item.setBackgroundResource(R.drawable.listselector);
		item.setGravity(Gravity.CENTER_VERTICAL);
		item.setPadding(n5 ,0, n20, 0);
		RelativeLayout.LayoutParams for_item = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,n60);
		item.setLayoutParams(for_item);
		item.setOnClickListener(new View.OnClickListener(){
			
			@Override
			public void onClick(View v)
			{
				openScreen3(position,false);
			}
			
		});
		
		ImageView yesno=new ImageView(item.getContext());
		if(nothingsThere(Integer.toString(position))) yesno.setBackgroundResource(R.drawable.no);
		else yesno.setBackgroundResource(R.drawable.yes);
		RelativeLayout.LayoutParams lpp=new RelativeLayout.LayoutParams(n30,n30);
		lpp.addRule(RelativeLayout.CENTER_VERTICAL);
		yesno.setLayoutParams(lpp);
		item.addView(yesno);
		
		LinearLayout Name_holder = new LinearLayout(item.getContext());
		RelativeLayout.LayoutParams for_nameholder = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT);
		Name_holder.setOrientation(LinearLayout.VERTICAL);
		Name_holder.setGravity(Gravity.CENTER);
		for_nameholder.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		for_nameholder.setMargins(n35, 0, 0, 0);
		Name_holder.setLayoutParams(for_nameholder);
		item.addView(Name_holder);
	
		
		TextView name = new TextView(Name_holder.getContext());
		name.setTypeface(Typeface.MONOSPACE ,Typeface.BOLD);
		name.setText(Name);
		name.setEllipsize(TruncateAt.END);
		name.setMaxLines(1);
		name.setTextColor(getResources().getColor(R.color.gray1));
		name.setTextSize(17);
		LayoutParams for_name= new LayoutParams(n170,LayoutParams.WRAP_CONTENT);
		name.setLayoutParams(for_name);
		Name_holder.addView(name);
		
		TextView works = new TextView(Name_holder.getContext());
		works.setText(work);
		works.setMaxLines(1);
		works.setEllipsize(TruncateAt.END);
		works.setTextSize(14);
		works.setTextColor(getResources().getColor(R.color.gray3));
		works.setTypeface(Typeface.DEFAULT ,Typeface.NORMAL);
		LayoutParams for_works= new LayoutParams(n170,LayoutParams.WRAP_CONTENT);
		works.setLayoutParams(for_works);
		Name_holder.addView(works);

		
		
		TextView tstamp = new TextView(item.getContext());
		RelativeLayout.LayoutParams for_tstamp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT);
		tstamp.setGravity(Gravity.CENTER);
		for_tstamp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		for_tstamp.addRule(RelativeLayout.CENTER_VERTICAL);
		for_tstamp.setMargins(0,0,n15,0);
		tstamp.setLayoutParams(for_tstamp);
		
		if(time==null) tstamp.setText("--:--");
		else tstamp.setText(time);
		
		tstamp.setTextSize(13);
		tstamp.setTextColor(getResources().getColor(R.color.gray3));
		tstamp.setTypeface(Typeface.DEFAULT_BOLD ,Typeface.NORMAL);
		item.addView(tstamp);
		
		list.addView(item);

		list.invalidate();
		addLine();
	
	}
	
	public boolean checkConnection() 
	{
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo == null ) return false;
        else return true;
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
                			Intent intent=new Intent(cc,Screen3.class);
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
                				IntentIntegrator.initiateScan(Screen2.this, R.layout.capture, R.id.viewfinder_view, R.id.preview_view, true);
                			}
                			else
                			{
                				Intent intent=new Intent(cc,Screen3.class);
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

	@Override
	public void onFetchComplete(){}

	@Override
	public void onFetchFailure(String msg){}

	@Override
	public void onFetchComplete(String string)
	{
		if(string.equals("done")||string.startsWith("done"))
		{
			syncdb.execSQL("DELETE FROM visitorproducts_new;");
		}
	}
}
