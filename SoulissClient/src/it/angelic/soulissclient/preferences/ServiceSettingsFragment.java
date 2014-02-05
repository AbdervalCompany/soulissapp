package it.angelic.soulissclient.preferences;

import it.angelic.soulissclient.Constants;
import it.angelic.soulissclient.R;
import it.angelic.soulissclient.SoulissClient;
import it.angelic.soulissclient.helpers.SoulissPreferenceHelper;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

@TargetApi(11)
public class ServiceSettingsFragment extends PreferenceFragment {

	private SoulissPreferenceHelper opzioni;

	//private static final int DIALOG_LOAD_FILE = 1000;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		final SoulissPreferenceHelper opzioni = SoulissClient.getOpzioni();

		super.onCreate(savedInstanceState);
		final LocationManager locationManager;
		// EXPORT

		final String provider;
		locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		// criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		provider = locationManager.getBestProvider(criteria, true);
		// datasource = new SoulissDBHelper(getActivity());

		addPreferencesFromResource(R.xml.settings_dataservice);
		final Preference serviceActive = (Preference) findPreference("checkboxService");
		final Preference webserviceActive = (Preference) findPreference("webserverEnabled");
		final Preference setHomeLocation = (Preference) findPreference("setHomeLocation");
		final Preference setHtmlRoot = (Preference) findPreference("setHtmlRoot");
		/* START STOP SoulissDataService */
		serviceActive.setOnPreferenceChangeListener(new ServicePreferenceListener(getActivity()));

		webserviceActive.setOnPreferenceChangeListener(new WebServerPreferenceListener(getActivity()));

		setHtmlRoot.setOnPreferenceClickListener(new SetHtmlRootListener(getActivity()));
		
		
		// Setta home location
		setHomeLocation.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				try {
					Location luogo = locationManager.getLastKnownLocation(provider);
					opzioni.setHomeLatitude(luogo.getLatitude());
					opzioni.setHomeLongitude(luogo.getLongitude());
					opzioni.initializePrefs();
					resetMesg(setHomeLocation);
					Toast.makeText(getActivity(), getString(R.string.opt_homepos_set), Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Log.e(Constants.TAG, getString(R.string.opt_homepos_err), e);
					Toast.makeText(getActivity(), getString(R.string.opt_homepos_err), Toast.LENGTH_SHORT).show();
				}
				return true;
			}
		});

		

		// Setta home location
		resetMesg(setHomeLocation);

	}

	public static String[] loadFileList(File mPath, final String ftype) {
		String[] mFileList;
		try {
			mPath.mkdirs();
		} catch (SecurityException e) {
			Log.e(Constants.TAG, "unable to write on the sd card " + e.toString());
		}
		if (mPath.exists()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					File sel = new File(dir, filename);
					return filename.contains(ftype) || sel.isDirectory();
				}
			};
			mFileList = mPath.list(filter);
		} else {
			mFileList = new String[0];
		}
		return mFileList;
	}

	private void resetMesg(Preference setHomeLocation) {
		String loc = null;
		opzioni = SoulissClient.getOpzioni();
		if (opzioni.getHomeLatitude() != 0) {

			Geocoder geocoder = new Geocoder(getActivity(), Locale.getDefault());
			List<Address> list;
			try {
				list = geocoder.getFromLocation(opzioni.getHomeLatitude(), opzioni.getHomeLongitude(), 1);
				if (list != null && list.size() > 0) {
					Address address = list.get(0);
					loc = address.getLocality();
				}
			} catch (IOException e) {
				Log.e(Constants.TAG, "LOCATION ERR:" + e.getMessage());
			}

		}
		setHomeLocation.setSummary(getString(R.string.opt_homepos_set) + ": " + (loc == null ? "" : loc) + " ("
				+ opzioni.getHomeLatitude() + " : " + opzioni.getHomeLongitude() + ")");
	}
}
