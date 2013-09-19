package it.angelic.soulissclient.preferences;

import it.angelic.soulissclient.R;
import it.angelic.soulissclient.SoulissClient;
import it.angelic.soulissclient.helpers.SoulissPreferenceHelper;
import android.annotation.TargetApi;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.widget.Toast;

@TargetApi(11)
public class VisualSettingsFragment extends PreferenceFragment {

	private SoulissPreferenceHelper opzioni;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		opzioni = SoulissClient.getOpzioni();
		addPreferencesFromResource(R.xml.settings_visual);
		final Preference restoreWarns = (Preference) findPreference("restoredialogs");
		//final Preference lightThemeCheckBox = (Preference) findPreference("checkboxHoloLight");

		// Rimette i dialogs

		restoreWarns.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference arg0) {
				opzioni.setDontShowAgain(getResources().getString(R.string.dialog_disabled_db), false);
				opzioni.setDontShowAgain(getResources().getString(R.string.dialog_disabled_service), false);
				Toast.makeText(getActivity(), SoulissClient.getAppContext().getString(R.string.opt_dialog_restored),
						Toast.LENGTH_SHORT).show();
				return true;
			}
		});
		// lightThemeCheckBox.setOnPreferenceChangeListener(new
		// OnPreferenceChangeListener() {
		//
		// @Override
		// public boolean onPreferenceChange(Preference preference, Object
		// newValue) {
		// // TODO togliere?
		// opzioni.reload();
		// final Intent preferencesActivity = new Intent(getActivity(),
		// PreferencesActivity.class);
		// preferencesActivity.putExtra("opt_screen", "visual_PrefScreen");
		// preferencesActivity.setAction("visual_PrefScreen");
		// preferencesActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		// startActivity(preferencesActivity);
		// return true;
		// }
		// });

	}

}