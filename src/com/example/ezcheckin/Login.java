
package com.example.ezcheckin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import com.crashlytics.android.Crashlytics;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

public class Login extends Activity implements FetchDataListener
{	
	private ProgressDialog dialog;
	private SQLiteDatabase maindb;
	public String EMAIL=null,PASSWORD=null;
	public static String IDTRADESHOW=null;
	public SharedPreferences session_info;
	public SharedPreferences.Editor editor;
	public String EzCheckInPrefs="ezcheckinprefs";
	public EditText mail_et,pass_et;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Crashlytics.start(this);
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		FontsOverride.setDefaultFont(this, "SANS_SERIF", "robotoregular.ttf");
		setContentView(R.layout.login);
		setCustomActionBar();		
		mail_et=(EditText)findViewById(R.id.name);
		pass_et=(EditText)findViewById(R.id.pass);
		
		pass_et.setImeOptions(EditorInfo.IME_ACTION_GO);
		pass_et.setOnEditorActionListener(new OnEditorActionListener() 
		{   
			@Override
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2)
			{
				onClickLogin(null);
				return true;
			}
		});
		createDatabases();
		checkForExistingSession();
	}
	
	public void checkForExistingSession()
	{
		session_info=getSharedPreferences(EzCheckInPrefs,0);
		IDTRADESHOW=session_info.getString("idtradeshow", "NONE");
		if(!IDTRADESHOW.equals("NONE"))
		{
			Intent intent=new Intent(this,Screen1.class);
			intent.putExtra("idtradeshow",IDTRADESHOW);
			intent.putExtra("fromsyncnow", false);
			startActivity(intent);
			maindb.close();
			finish();
		}
	}
	
	private void createDatabases()
	{
		maindb = openOrCreateDatabase("ezcheckinmain.db", SQLiteDatabase.CREATE_IF_NECESSARY, null);
		maindb.setVersion(1);
		maindb.setLocale(Locale.getDefault());
		try
		{
			maindb.execSQL("CREATE TABLE users(email TEXT,password TEXT,idtradeshow TEXT);");
		}
		catch(Exception e){Log.i("error","Login.java, createDatabases()");}
	}
	
	private void setCustomActionBar()
	{
		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getActionBar().setCustomView(R.layout.actionbar_login);
	}
	
	public void onClickLogin(View v)
	{
		InputMethodManager inputManager=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
		inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
		
		EMAIL=mail_et.getText().toString();
		PASSWORD=pass_et.getText().toString();
		
		if(!validateLoginFields(EMAIL,PASSWORD))return;
		
		dialog = ProgressDialog.show(this,"","Please Wait..");
        
		checkLocalDb(EMAIL,PASSWORD);
		if(IDTRADESHOW.equals("passwordwrong"))
		{
			dialog.dismiss();
			pass_et.setText("");
			Toast.makeText(this, "You've got the password wrong!",Toast.LENGTH_LONG).show();
			return;
		}
		else if(IDTRADESHOW.equals("noentry"))
		{
			if(CheckConnection()==1)
			{
				String url="http://cardleadr.com/webservices/ez/users.php";
				try {
					url = new URI("http", "//www.cardleadr.com/webservices/ez/login.php?mailid="+EMAIL+"&password="+PASSWORD, null).toASCIIString();
				} 
				catch (URISyntaxException e){Log.i("URI Syntax Exception","URI Syntax Exception");}
				
				FetchDataTaskLogin task = new FetchDataTaskLogin(this,maindb);
				task.execute(url);
			}
			else
			{
				dialog.dismiss();
				Toast.makeText(this, "Seems like you're logging in for the first time here. Turn on mobile data!", Toast.LENGTH_LONG).show();
				return;
			}
		}
		else
		{
			Intent intent=new Intent(this,Screen1.class);
			intent.putExtra("idtradeshow",IDTRADESHOW);
			intent.putExtra("fromsyncnow", false);
			startActivity(intent);
			finish();
		}
		
	}
	
	private int CheckConnection() 
	{
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo == null ) return 0;
        else return 1;
    }
	
	private void checkLocalDb(String email,String password)
	{
		Cursor c=maindb.rawQuery("SELECT password,idtradeshow FROM users WHERE email='"+EMAIL+"'", null);
		try
		{
			c.moveToFirst();
			if(password.equals(c.getString(0)))
			{
				IDTRADESHOW=c.getString(1);
			}
			else
			{
				IDTRADESHOW="passwordwrong";
			}
		}
		catch(Exception e)
		{
			IDTRADESHOW="noentry";
		}
	}
	
	private boolean validateLoginFields(String mailid,String password)
	{
		boolean ret_val=true;
		EditText mail_et=(EditText)findViewById(R.id.name);
		if(mailid.equals("")||!mailid.contains("@")||!mailid.contains("."))
		{
			mail_et.setText("");
			Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
	        findViewById(R.id.name).startAnimation(shake);
	        ret_val=false;
		}
		if(password.equals(""))
		{
			Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
	        findViewById(R.id.pass).startAnimation(shake);
	        ret_val=false;
		}
		return ret_val;
	}
	
	public void onFetchComplete(String s){}
	
	@Override
    public void onFetchComplete()
	{
		dialog.dismiss();
		
		try
		{
		
		if(IDTRADESHOW.equals("noentry"))
		{
			EditText mail_et=(EditText)findViewById(R.id.name);
			EditText pass_et=(EditText)findViewById(R.id.pass);
			mail_et.setText("");
			pass_et.setText("");
			Toast.makeText(this, "Email id and Password do not match!", Toast.LENGTH_LONG).show();
			return;
		}
		else if(IDTRADESHOW.equals("inputstreamnull"))
		{
			EditText mail_et=(EditText)findViewById(R.id.name);
			EditText pass_et=(EditText)findViewById(R.id.pass);
			mail_et.setText("");
			pass_et.setText("");
			Toast.makeText(this, "Couldn't contact the server, try again later!", Toast.LENGTH_LONG).show();
			return;
		}
		else
		{
			maindb.execSQL("INSERT INTO users VALUES('"+EMAIL+"','"+PASSWORD+"','"+IDTRADESHOW+"');");
			Intent intent=new Intent(this,Screen1.class);
			intent.putExtra("idtradeshow", IDTRADESHOW);
			intent.putExtra("fromsyncnow", false);
			startActivity(intent);		
			finish();
		}
		
		}
		catch(Exception e)
		{
			Log.i("Exception in","onFetchComplete()");
		}
	}
 
    @Override
    public void onFetchFailure(String msg)
    {
    	if(dialog != null)  dialog.dismiss();
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
