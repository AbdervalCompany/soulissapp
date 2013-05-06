package it.angelic.receivers;

import it.angelic.soulissclient.SoulissClient;
import it.angelic.soulissclient.SoulissDataService;
import it.angelic.soulissclient.helpers.SoulissPreferenceHelper;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receive per controllo esecuzione servizio.
 * Viene invocato dopo il boot, e all'USER_PRESENT
 * http://www.hascode.com/2011/11/managing-background-tasks-on-android-using-the-alarm-manager/

 */
public class LifeCheckerEventReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context ctx, final Intent intent) {
		Log.d("SoulissApp:LifeChecker", "ScheduledEventReceiver.onReceive() called");
		SoulissPreferenceHelper opzioni = SoulissClient.getOpzioni();

		if (opzioni.isDataServiceEnabled()) {
			Intent eventService = new Intent(ctx, SoulissDataService.class);
			if (!isMyServiceRunning(ctx)){
				ctx.startService(eventService);
				Log.w("SoulissApp:LifeChecker", "Service restarted");
			}
			else
				Log.d("SoulissApp:LifeChecker", "Service already running");
		}
	}
	//FIXME or error checks
	private boolean isMyServiceRunning(final Context ctx) {
		ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (SoulissDataService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

}