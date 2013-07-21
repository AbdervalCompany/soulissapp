package it.angelic.soulissclient;

import static junit.framework.Assert.assertTrue;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import it.angelic.soulissclient.adapters.TypicalsListAdapter;
import it.angelic.soulissclient.fragments.SensorDetailFragment;
import it.angelic.soulissclient.helpers.SoulissPreferenceHelper;
import it.angelic.soulissclient.model.typicals.SoulissTypical;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;

public class SensorDetailActivity extends SherlockFragmentActivity {
	private SoulissPreferenceHelper opzioni;
	private TypicalsListAdapter ta;
	private SoulissDataService mBoundService;
	private boolean mIsBound;

	/* SOULISS DATA SERVICE BINDING */
	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {

			mBoundService = ((SoulissDataService.LocalBinder) service).getService();
			if (ta != null)
				ta.setmBoundService(mBoundService);
			// Tell the user about this for our demo.
			// Toast.makeText(NodeDetailActivity.this, "Dataservice connected",
			// Toast.LENGTH_SHORT).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			mBoundService = null;
			// if (ta != null)
			ta.setmBoundService(null);
			// Toast.makeText(NodeDetailActivity.this,
			// "Dataservice disconnected", Toast.LENGTH_SHORT).show();
		}
	};
	//private ImageView nodeic;
	//private Handler timeoutHandler;
	private SoulissTypical collected;
	
	

	void doBindService() {
		if (!mIsBound) {
			bindService(new Intent(SensorDetailActivity.this, SoulissDataService.class), mConnection,
					Context.BIND_AUTO_CREATE);
			mIsBound = true;
		}
	}

	void doUnbindService() {
		if (mIsBound) {
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		opzioni = SoulissClient.getOpzioni();
		if (opzioni.isLightThemeSelected())
			setTheme(com.actionbarsherlock.R.style.Theme_Sherlock_Light);
		else
			setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);
		// recuper nodo da extra
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			// If the screen is now in landscape mode, we can show the
			// dialog in-line with the list so we don't need this activity.
			finish();
			return;
		}
		Bundle extras = getIntent().getExtras();
		//collected.setCtx(getActivity());

		if (extras != null && extras.get("TIPICO") != null)
			collected = (SoulissTypical) extras.get("TIPICO");
		assertTrue("TIPICO NULLO", collected != null);
		if (savedInstanceState == null) {
			// During initial setup, plug in the details fragment.
			SensorDetailFragment details = SensorDetailFragment.newInstance(collected.getTypicalDTO().getSlot(), collected);
			details.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().replace(android.R.id.content, details).commit();
		}
	}
	@SuppressLint("NewApi")
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		if (Constants.versionNumber >= 11) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	// meccanismo per timeout detection
	/*
	 * private BroadcastReceiver timeoutReceiver = new BroadcastReceiver() {
	 * 
	 * @Override public void onReceive(Context context, Intent intent) {
	 * Log.w(TAG, "Posting timeout from " + intent.toString()); Bundle extras =
	 * intent.getExtras(); int delay = extras.getInt("REQUEST_TIMEOUT_MSEC"); }
	 * };
	 */
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			if (opzioni.isAnimationsEnabled())
				overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


}
