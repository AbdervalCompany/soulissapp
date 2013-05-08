package it.angelic.soulissclient;

import static it.angelic.soulissclient.Constants.TAG;
import it.angelic.soulissclient.helpers.AlertDialogHelper;
import it.angelic.soulissclient.helpers.SoulissPreferenceHelper;
import it.angelic.soulissclient.net.Constants;
import it.angelic.soulissclient.preferences.DbPreferenceListener;
import it.angelic.soulissclient.preferences.DbSettingsFragment;
import it.angelic.soulissclient.preferences.IpChangerListener;
import it.angelic.soulissclient.preferences.NetSettingsFragment;
import it.angelic.soulissclient.preferences.ServicePreferenceListener;
import it.angelic.soulissclient.preferences.ServiceSettingsFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity {
	private PackageInfo packageInfo;
	// private String strVersionCode;
	private String strVersionName;
	SoulissPreferenceHelper opzioni;

	@TargetApi(11)
	@Override
	public void onBuildHeaders(List<Header> target) {
		Log.i(TAG, "PreferenceActivityonBuildHeaders()");
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	/*
	 * @TargetApi(11) protected void AddResourceApi11AndGreater() {
	 * getFragmentManager().beginTransaction().replace(android.R.id.content, new
	 * PrefsFragment()).commit(); }
	 */

	@SuppressWarnings("deprecation")
	protected void AddResourceApiLessThan11(String action) {
		if (action != null && action.equals("network_setup")) {
			addPreferencesFromResource(R.xml.settings_net);
		} else if (action != null && action.equals("db_setup")) {
			addPreferencesFromResource(R.xml.settings_db);
		} else if (action != null && action.equals("service_setup")) {
			addPreferencesFromResource(R.xml.settings_dataservice);
		} else if (action != null && action.equals("visual_setup")) {
			addPreferencesFromResource(R.xml.settings_visual);
		} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			// Load the legacy preferences headers
			addPreferencesFromResource(R.xml.preferences_legacy);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		opzioni = SoulissClient.getOpzioni();
		if (opzioni.isLightThemeSelected()) {
			setTheme(R.style.LightThemeSelector);
		} else
			setTheme(R.style.DarkThemeSelector);
		super.onCreate(savedInstanceState);

		// Display the fragment as the main content.
		try {
			getClass().getMethod("getFragmentManager");
			// non serve, ci sono headers
			// AddResourceApi11AndGreater();
		} catch (NoSuchMethodException e) { // Api < 11
			String action = getIntent().getAction();
			AddResourceApiLessThan11(action);
		}

		ListView v = getListView();
		v.setCacheColorHint(0);

	}

	@SuppressLint("NewApi")
	@Override
	protected void onStart() {
		super.onStart();
		String action = getIntent().getAction();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// PrefsFragment pf = new PrefsFragment();
			/*
			 * if (action != null &&
			 * !"android.intent.action.MAIN".equals(action)){ Bundle b = new
			 * Bundle(); b.putString(EXTRA_SHOW_FRAGMENT, action);
			 * 
			 * pf.setArguments(b);
			 * getFragmentManager().beginTransaction().replace
			 * (android.R.id.content, pf).commit(); }
			 */

			// loadHeadersFromResource(R.xml.preference_headers, new
			// ArrayList<Header>());
			Log.d(TAG, "Going thru preference onStart()");
			return;
		}
		opzioni.reload();
		/*
		 * arrivo dai fragments, evido code Dup SCHERMATA NETWORK
		 */
		if (action != null && action.equals("network_setup")) {
			Preference privateIP = (Preference) findPreference("edittext_IP");
			// Vedi se e` gia` settato l'IP PUBBLICO
			Preference publicIP = (Preference) findPreference("edittext_IP_pubb");
			String summar = getResources().getString(R.string.summary_edittext_IP);
			privateIP.setSummary(opzioni.getPrefIPAddress().compareToIgnoreCase("") == 0 ? summar : opzioni
					.getPrefIPAddress());

			String summarP = getResources().getString(R.string.summary_edittext_IP_pubb);
			publicIP.setSummary(opzioni.getIPPreferencePublic().compareToIgnoreCase("") == 0 ? summarP : opzioni
					.getIPPreferencePublic());
			/* aggiorna il sommario PUBBLICO una volta modificato */
			OnPreferenceChangeListener ipChanger = new IpChangerListener(this);
			privateIP.setOnPreferenceChangeListener(ipChanger);
			publicIP.setOnPreferenceChangeListener(ipChanger);
			final Preference userIdx = (Preference) findPreference("userindexIC");
			userIdx.setSummary("The user index identify this device on Souliss boards. Current value is "
					+ opzioni.getUserIndex() + ". Tap to change it");
			userIdx.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					try {
						SharedPreferences prefs = PreferenceManager
								.getDefaultSharedPreferences(PreferencesActivity.this);
						Editor pesta = prefs.edit();
						String ics = (String) newValue;
						pesta.putInt("userindex", Integer.parseInt(ics));
						pesta.commit();
						opzioni.setUserIndex(Integer.parseInt(ics));
						userIdx.setSummary("The user index identify this device on Souliss boards. Current value is "
								+ opzioni.getUserIndex() + ". Tap to change it");
					} catch (Exception e) {
						Toast.makeText(PreferencesActivity.this, "Please insert a number in range 0-127",
								Toast.LENGTH_SHORT).show();
					}
					return true;
				}
			});

		} else if (action != null && action.equals("db_setup")) {
			Preference createDbPref = (Preference) findPreference("createdb");
			Preference dropDbPref = (Preference) findPreference("dropdb");
			Preference exportDBPref = (Preference) findPreference("dbexp");
			Preference imortDBPref = (Preference) findPreference("dbimp");
			Preference dbinfopref = (Preference) findPreference("dbinfo");

			exportDBPref.setOnPreferenceClickListener(new DbPreferenceListener(this));
			/* Scegli il file e importa il DB */
			imortDBPref.setOnPreferenceClickListener(new DbPreferenceListener(this));
			// richiesta creazione nodi
			createDbPref.setOnPreferenceClickListener(new DbPreferenceListener(this));
			// dialogo drop DB
			dropDbPref.setOnPreferenceClickListener(new DbPreferenceListener(this));

			/*
			 * SCHERMATA DBINFO
			 */
			String strMeatFormat = getResources().getString(R.string.opt_dbinfo_desc);
			String nonode = "Souliss not configured yet, DB is empty";
			final String strMeatMsg = opzioni.getCustomPref().getInt("numNodi", 0) == 0 ? nonode : String.format(
					strMeatFormat, opzioni.getCustomPref().getInt("numNodi", 0),
					opzioni.getCustomPref().getInt("numTipici", 0));
			dbinfopref.setSummary(strMeatMsg);
		} else if (action != null && action.equals("service_setup")) {
			final Preference serviceActive = (Preference) findPreference("checkboxService");
			final Preference setHomeLocation = (Preference) findPreference("setHomeLocation");
			final LocationManager locationManager;
			// EXPORT

			final String provider;
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			Criteria criteria = new Criteria();
			// criteria.setAccuracy(Criteria.ACCURACY_FINE);
			provider = locationManager.getBestProvider(criteria, true);
			// datasource = new SoulissDBHelper(getActivity());

			/* START STOP SoulissDataService */
			serviceActive.setOnPreferenceChangeListener(new ServicePreferenceListener(this));

			// Setta home location
			setHomeLocation.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					try {
						Location luogo = locationManager.getLastKnownLocation(provider);
						opzioni.setHomeLatitude(luogo.getLatitude());
						opzioni.setHomeLongitude(luogo.getLongitude());
						opzioni.reload();
						Intent inten = PreferencesActivity.this.getIntent();
						PreferencesActivity.this.overridePendingTransition(0, 0);
						PreferencesActivity.this.finish();
						inten.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
							AlertDialogHelper.setExtra(inten, ServiceSettingsFragment.class.getName());
						inten.setAction("service_setup");

						PreferencesActivity.this.overridePendingTransition(0, 0);
						PreferencesActivity.this.startActivity(inten);
					} catch (Exception e) {
						Toast.makeText(PreferencesActivity.this, "Error getting current position", Toast.LENGTH_SHORT)
								.show();
					}
					return true;
				}
			});

			String loc = null;
			if (opzioni.getHomeLatitude() != 0) {

				Geocoder geocoder = new Geocoder(this, Locale.getDefault());
				List<Address> list;
				try {
					list = geocoder.getFromLocation(opzioni.getHomeLatitude(), opzioni.getHomeLongitude(), 1);
					if (list != null && list.size() > 0) {
						Address address = list.get(0);
						loc = address.getLocality();
					}
				} catch (IOException e) {
					Log.e(TAG, "LOCATION ERR:" + e.getMessage());
				}

			}
			setHomeLocation.setSummary("Home location set to: " + (loc == null ? "" : loc) + " ("
					+ opzioni.getHomeLatitude() + " : " + opzioni.getHomeLongitude() + ")");

		} else if (action != null && action.equals("visual_setup")) {

			final Preference restoreWarns = (Preference) findPreference("restoredialogs");
			restoreWarns.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference arg0) {
					opzioni.setDontShowAgain(getResources().getString(R.string.dialog_disabled_db), false);
					opzioni.setDontShowAgain(getResources().getString(R.string.dialog_disabled_service), false);
					Toast.makeText(PreferencesActivity.this,
							SoulissClient.getAppContext().getString(R.string.opt_dialog_restored), Toast.LENGTH_SHORT)
							.show();
					return true;
				}
			});
		} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			try {
				packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
				// strVersionCode = "Version Code: "
				// + String.valueOf(packageInfo.versionCode);
				strVersionName = packageInfo.versionName;

			} catch (NameNotFoundException e) {
				Log.e(TAG, "Cannot load Version!", e);
				strVersionName = "Cannot load Version!";
			}
			Preference creditsPref = (Preference) findPreference("credits");
			creditsPref.setTitle(getResources().getString(R.string.app_name) + " Version " + strVersionName);
			// Rimette i dialogs

		}

	}

	// Aggiorna la schermata
	private BroadcastReceiver macacoRawDataReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				Bundle extras = intent.getExtras();
				ArrayList<Short> vers = (ArrayList<Short>) extras.get("MACACO");
				Log.w(TAG, "RAW DATA: " + vers);
				switch (vers.get(0)) {
				case Constants.Souliss_UDP_function_typreq_resp:
					;// fallthrought x refresh dicitura tipici
				case Constants.Souliss_UDP_function_db_struct_resp:
					Intent inten = PreferencesActivity.this.getIntent();
					inten.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
					PreferencesActivity.this.finish();
					PreferencesActivity.this.overridePendingTransition(0, 0);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
						inten.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, DbSettingsFragment.class.getName());
					inten.setAction("db_setup");

					PreferencesActivity.this.startActivity(inten);
					break;
				case Constants.Souliss_UDP_function_ping_resp:
					Intent intend = PreferencesActivity.this.getIntent();
					intend.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
					PreferencesActivity.this.finish();
					PreferencesActivity.this.overridePendingTransition(0, 0);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
						AlertDialogHelper.setExtra(intend, NetSettingsFragment.class.getName());
					// preferencesActivity.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS,com);
					intend.setAction("network_setup");

					intend.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

					PreferencesActivity.this.overridePendingTransition(0, 0);
					PreferencesActivity.this.startActivity(intend);
					break;
				default:
					break;
				}
			} catch (Exception e) {
				Log.e(TAG, "EMPTY RAW dATA !!");
			}
		}
	};

	@Override
	protected void onResume() {
		// IDEM, serve solo per reporting
		IntentFilter filtere = new IntentFilter();
		filtere.addAction(it.angelic.soulissclient.net.Constants.CUSTOM_INTENT_SOULISS_RAWDATA);
		registerReceiver(macacoRawDataReceiver, filtere);

		super.onResume();
	}

	@Override
	protected void onPause() {
		unregisterReceiver(macacoRawDataReceiver);
		super.onPause();
	}

	@Deprecated
	private void hackBackGroundLights() {

		PreferenceScreen b = (PreferenceScreen) findPreference("network_setup");
		PreferenceScreen c = (PreferenceScreen) findPreference("db_setup");
		PreferenceScreen d = (PreferenceScreen) findPreference("service_setup");
		PreferenceScreen e = (PreferenceScreen) findPreference("visual_PrefScreen");
		OnPreferenceClickListener ber = new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				PreferenceScreen a = (PreferenceScreen) preference;
				if (opzioni.isLightThemeSelected())
					a.getDialog().getWindow().setBackgroundDrawableResource(R.drawable.radialbacklight);
				else
					a.getDialog().getWindow().setBackgroundDrawableResource(R.drawable.radialback);
				return false;
			}
		};

		b.setOnPreferenceClickListener(ber);
		c.setOnPreferenceClickListener(ber);
		d.setOnPreferenceClickListener(ber);
		e.setOnPreferenceClickListener(ber);
	}

}
