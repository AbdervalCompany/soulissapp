package it.angelic.soulissclient.helpers;

import static it.angelic.soulissclient.Constants.TAG;
import static junit.framework.Assert.assertTrue;
import it.angelic.soulissclient.Constants;
import it.angelic.soulissclient.PreferencesActivity;
import it.angelic.soulissclient.R;
import it.angelic.soulissclient.SoulissClient;
import it.angelic.soulissclient.adapters.NodesListAdapter;
import it.angelic.soulissclient.adapters.ProgramListAdapter;
import it.angelic.soulissclient.adapters.SceneListAdapter;
import it.angelic.soulissclient.adapters.SoulissIconAdapter;
import it.angelic.soulissclient.adapters.TypicalsListAdapter;
import it.angelic.soulissclient.db.SoulissDB;
import it.angelic.soulissclient.db.SoulissDBHelper;
import it.angelic.soulissclient.model.ISoulissObject;
import it.angelic.soulissclient.model.SoulissCommand;
import it.angelic.soulissclient.model.SoulissNode;
import it.angelic.soulissclient.model.SoulissScene;
import it.angelic.soulissclient.model.SoulissTypical;
import it.angelic.soulissclient.net.UDPHelper;
import it.angelic.soulissclient.preferences.DbSettingsFragment;
import it.angelic.soulissclient.preferences.NetSettingsFragment;
import it.angelic.soulissclient.preferences.ServiceSettingsFragment;

import java.util.LinkedList;
import java.util.List;

import us.feras.ecogallery.EcoGallery;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Looper;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Classe helper per i dialoghi riciclabili
 * 
 * */
public class AlertDialogHelper {
	// private static ProgressDialog progressDialog;

	/**
	 * Mostra warning che il sistema non ha l'IP settato
	 * 
	 * @param source
	 * @return
	 */
	public static AlertDialog.Builder sysNotInitedDialog(final Activity source) {
		AlertDialog.Builder alert = new AlertDialog.Builder(source);
		alert.setIcon(android.R.drawable.ic_dialog_alert);
		alert.setTitle(source.getResources().getString(R.string.notconfigured));
		alert.setMessage(source.getResources().getString(R.string.dialog_notinited_ip));

		alert.setPositiveButton(source.getResources().getString(R.string.proceed),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						final Intent preferencesActivity = new Intent(source.getBaseContext(),
								PreferencesActivity.class);

						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
							setExtra(preferencesActivity, NetSettingsFragment.class.getName());
						// preferencesActivity.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS,com);
						preferencesActivity.setAction("network_setup");
						preferencesActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
						preferencesActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

						source.startActivity(preferencesActivity);
					}
				});
		alert.setNegativeButton(source.getResources().getString(android.R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

		return alert;
	}

	public static void dbNotInitedDialog(final Activity source) {
		final SoulissPreferenceHelper opts = new SoulissPreferenceHelper(source);
		AlertDialog.Builder alert = new AlertDialog.Builder(source);
		if (!opts.getDontShowAgain(source.getResources().getString(R.string.dialog_disabled_db))) {
			final CheckBox checkBox = new CheckBox(source);
			checkBox.setText(source.getResources().getString(R.string.dialog_dontshowagain));
			if (opts.isLightThemeSelected() && Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
				checkBox.setTextColor(source.getResources().getColor(R.color.black));
			LinearLayout linearLayout = new LinearLayout(source);
			linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.FILL_PARENT));
			linearLayout.setOrientation(1);
			alert.setMessage(source.getResources().getString(R.string.dialog_notinited_db));
			linearLayout.addView(checkBox);
			alert.setView(linearLayout);

			alert.setIcon(android.R.drawable.ic_dialog_alert);
			alert.setTitle(source.getResources().getString(R.string.dialog_disabled_db));

			alert.setPositiveButton(source.getResources().getString(R.string.proceed),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							if (checkBox.isChecked()) {
								opts.setDontShowAgain(source.getResources().getString(R.string.dialog_disabled_db),
										true);
							}
							final Intent preferencesActivity = new Intent(source.getBaseContext(),
									PreferencesActivity.class);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
								setExtra(preferencesActivity, DbSettingsFragment.class.getName());
							preferencesActivity.setAction("db_setup");
							preferencesActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
							preferencesActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							source.startActivity(preferencesActivity);
						}
					});
			alert.setNegativeButton(source.getResources().getString(android.R.string.cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							if (checkBox.isChecked()) {
								opts.setDontShowAgain(source.getResources().getString(R.string.dialog_disabled_db),
										true);
							}
						}
					});

			alert.show();
		}
	}

	public static void serviceNotActiveDialog(final Activity source) {
		final SoulissPreferenceHelper opts = SoulissClient.getOpzioni();
		AlertDialog.Builder alert = new AlertDialog.Builder(source);

		final CheckBox checkBox = new CheckBox(source);
		TextView textView = new TextView(source);
		if (!opts.getDontShowAgain(source.getResources().getString(R.string.dialog_disabled_service))) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
				checkBox.setTextColor(source.getResources().getColor(R.color.white_bitaplha));
			checkBox.setText(source.getResources().getString(R.string.dialog_dontshowagain));
			alert.setMessage(source.getResources().getString(R.string.dialog_notinited_service));

			LinearLayout linearLayout = new LinearLayout(source);
			linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT));
			linearLayout.setOrientation(1);
			linearLayout.addView(textView);
			linearLayout.addView(checkBox);
			alert.setView(linearLayout);

			alert.setIcon(android.R.drawable.ic_dialog_alert);
			alert.setTitle(source.getResources().getString(R.string.dialog_disabled_service));

			alert.setPositiveButton(source.getResources().getString(R.string.proceed),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							if (checkBox.isChecked()) {
								opts.setDontShowAgain(
										source.getResources().getString(R.string.dialog_disabled_service), true);
							}
							final Intent preferencesActivity = new Intent(source.getBaseContext(),
									PreferencesActivity.class);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
								setExtra(preferencesActivity, ServiceSettingsFragment.class.getName());
							preferencesActivity.setAction("service_setup");
							preferencesActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
							preferencesActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							source.startActivity(preferencesActivity);
						}
					});
			alert.setNegativeButton(source.getResources().getString(android.R.string.cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							if (checkBox.isChecked()) {
								opts.setDontShowAgain(
										source.getResources().getString(R.string.dialog_disabled_service), true);
							}
						}
					});

			alert.show();
		}
	}

	public static AlertDialog.Builder dropSoulissDBDialog(final Activity source, final SoulissDBHelper datasource) {
		AlertDialog.Builder alert = new AlertDialog.Builder(source);
		// AlertDialog.Builder alert;
		// alert = new AlertDialog.Builder(new ContextThemeWrapper(source,
		// R.style.AboutDialog));
		alert.setIcon(android.R.drawable.ic_dialog_alert);
		final SharedPreferences soulissCust = source.getSharedPreferences("SoulissPrefs", Activity.MODE_PRIVATE);
		alert.setTitle(source.getResources().getString(R.string.dialog_warn_db));
		alert.setMessage(source.getResources().getString(R.string.dialog_drop_db));

		alert.setPositiveButton(source.getResources().getString(R.string.proceed),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						datasource.close();
						if (source.deleteDatabase(SoulissDB.DATABASE_NAME)) {
							SharedPreferences.Editor editor = soulissCust.edit();
							// tolgo db dalle prefs
							if (soulissCust.contains("numNodi"))
								editor.remove("numNodi");
							if (soulissCust.contains("numTipici"))
								editor.remove("numTipici");
							// Toast.makeText(source, "Souliss DB Deleted",
							// Toast.LENGTH_SHORT).show();
							editor.commit();
							Log.w(TAG, "Souliss DB dropped");
							// source.finish();
							final Intent preferencesActivity = new Intent(source.getBaseContext(),
									PreferencesActivity.class);
							// com.putString("opt_screen", "db_setup");
							// preferencesActivity.putExtra("opt_screen",com);
							// preferencesActivity.putExtra("opt_screen",
							// "db_setup");
							// preferencesActivity.putExtra(PreferenceActivity.EXTRA_NO_HEADERS,
							// true);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
								setExtra(preferencesActivity, DbSettingsFragment.class.getName());
							// preferencesActivity.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS,com);
							preferencesActivity.setAction("db_setup");
							preferencesActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
							preferencesActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							source.startActivity(preferencesActivity);

						} else {
							Log.e(TAG, "Unable to DROP DB");
							Toast.makeText(source, "Unable to DROP DB", Toast.LENGTH_SHORT).show();
						}
					}
				});
		alert.setNegativeButton(source.getResources().getString(android.R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

		return alert;
	}

	@TargetApi(11)
	public static void setExtra(Intent preferencesActivity, String name) {
		preferencesActivity.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, name);

	}

	/**
	 * Remove a command
	 * 
	 * @param ctx
	 *            used to invalidate views
	 * @param datasource
	 *            to store new value
	 * @param toRename
	 * @return
	 */
	public static AlertDialog.Builder removeCommandDialog(final Context cont, final ListView ctx,
			final SoulissDBHelper datasource, final SoulissCommand toRename) {
		AlertDialog.Builder alert = new AlertDialog.Builder(cont);
		alert.setTitle(cont.getString(R.string.dialog_remove_title));
		alert.setIcon(android.R.drawable.ic_dialog_alert);
		alert.setMessage(cont.getString(R.string.dialog_remove_cmd));
		alert.setPositiveButton(cont.getResources().getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int whichButton) {
						datasource.open();
						int res = datasource.deleteCommand(toRename);
						Log.i(TAG, "SoulissCommand deletion returned: " + res);
						if (ctx != null) {
							// prendo comandi dal DB
							LinkedList<SoulissCommand> goer = datasource.getUnexecutedCommands(cont);
							SoulissCommand[] programsArray = new SoulissCommand[goer.size()];
							programsArray = goer.toArray(programsArray);

							ProgramListAdapter progsAdapter = new ProgramListAdapter(cont, programsArray, datasource
									.getTriggerMap(cont), new SoulissPreferenceHelper(cont.getApplicationContext()));
							// Adapter della lista
							ctx.setAdapter(progsAdapter);
							ctx.invalidateViews();
						}
						datasource.close();
					}
				});

		alert.setNegativeButton(cont.getResources().getString(android.R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});
		return alert;
	}

	/**
	 * Rename a node
	 * 
	 * @param listV
	 *            used to invalidate views
	 * @param datasource
	 *            to store new value
	 * @param toRename
	 * @return
	 */
	public static AlertDialog.Builder renameSoulissObjectDialog(final Context cont, final TextView tgt,
			final ListView listV, final SoulissDBHelper datasource, final ISoulissObject toRename) {
		final AlertDialog.Builder alert = new AlertDialog.Builder(cont);
		final SoulissPreferenceHelper opzioni = new SoulissPreferenceHelper(cont);
		assertTrue("chooseIconDialog: NOT instanceof", toRename instanceof SoulissNode
				|| toRename instanceof SoulissScene || toRename instanceof SoulissTypical);
		alert.setIcon(android.R.drawable.ic_dialog_dialer);
		alert.setTitle(cont.getString(R.string.rename) + " " + toRename.getNiceName());

		// Set an EditText view to get user input
		final EditText input = new EditText(cont);
		alert.setView(input);
		input.setText(toRename.getNiceName());
		alert.setPositiveButton(cont.getResources().getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						toRename.setName(value);
						datasource.open();
						if (toRename instanceof SoulissNode) {
							datasource.createOrUpdateNode((SoulissNode) toRename);
							if (listV != null) {
								List<SoulissNode> goer = datasource.getAllNodes();
								SoulissNode[] nodiArray = new SoulissNode[goer.size()];
								nodiArray = goer.toArray(nodiArray);
								NodesListAdapter nodesAdapter = new NodesListAdapter(cont, nodiArray, opzioni);
								// Adapter della lista
								listV.setAdapter(nodesAdapter);
								listV.invalidateViews();
							}

						} else if (toRename instanceof SoulissScene) {
							datasource.createOrUpdateScene((SoulissScene) toRename);
							if (listV != null) {
								LinkedList<SoulissScene> goer = datasource.getScenes(SoulissClient.getAppContext());
								SoulissScene[] scenesArray = new SoulissScene[goer.size()];
								scenesArray = goer.toArray(scenesArray);
								try {
									SceneListAdapter sa = (SceneListAdapter) listV.getAdapter();
									// SceneListAdapter progsAdapter = new
									// SceneListAdapter(cont, scenesArray,
									// opzioni);
									// Adapter della lista
									sa.setScenes(scenesArray);
									sa.notifyDataSetChanged();
									// listV.setAdapter(sa);
									listV.invalidateViews();
								} catch (Exception e) {
									Log.w(Constants.TAG, "rename didn't find proper view to refresh");
								}
							}
						} else {
							if (listV != null) {
								((SoulissTypical) toRename).getTypicalDTO().persist();
								TypicalsListAdapter ta = (TypicalsListAdapter) listV.getAdapter();
								ta.notifyDataSetChanged();
								listV.invalidateViews();
							}
						}
						if (cont instanceof Activity && !(toRename instanceof SoulissTypical))
							((Activity) cont).setTitle(toRename.getNiceName());
						if (tgt != null) {
							tgt.setText(value);
							tgt.setText(toRename.getNiceName());
						}

					}
				});

		alert.setNegativeButton(cont.getResources().getString(android.R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});
		return alert;
	}

	public static AlertDialog equalizerDialog(final Context context, final TextView toUpdate) {
		final SoulissPreferenceHelper opzioni = SoulissClient.getOpzioni();
		// alert2.setTitle("Choose " + toRename.toString() + " icon");
		final AlertDialog.Builder deleteBuilder = new AlertDialog.Builder(context);

		LayoutInflater factory = LayoutInflater.from(context);
		final View deleteDialogView = factory.inflate(R.layout.dialog_equalizer, null);

		final SeekBar low = (SeekBar) deleteDialogView.findViewById(R.id.seekBarLow);
		final SeekBar med = (SeekBar) deleteDialogView.findViewById(R.id.seekBarMed);
		final SeekBar hi = (SeekBar) deleteDialogView.findViewById(R.id.seekBarHigh);
		low.setProgress(Float.valueOf(opzioni.getEqLow() * 100f).intValue());
		med.setProgress(Float.valueOf(opzioni.getEqMed() * 100f).intValue());
		hi.setProgress(Float.valueOf(opzioni.getEqHigh() * 100f).intValue());
		Log.i("SoulissEqualizer", "Setting new eq low:" + opzioni.getEqLow() + " med: " + opzioni.getEqMed()
				+ " high: " + opzioni.getEqHigh());

		deleteBuilder.setPositiveButton(context.getResources().getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						opzioni.setEqLow(low.getProgress() / 100f);
						opzioni.setEqMed((float) med.getProgress() / 100f);
						opzioni.setEqHigh(hi.getProgress() / 100f);
						String strDisease2Format = context.getString(R.string.Souliss_TRGB_eq);
						String strDisease2Msg = String.format(strDisease2Format,
								Constants.twoDecimalFormat.format(opzioni.getEqLow()),
								Constants.twoDecimalFormat.format(opzioni.getEqMed()),
								Constants.twoDecimalFormat.format(opzioni.getEqHigh()));
						toUpdate.setText(strDisease2Msg);
					}
				});

		deleteBuilder.setNegativeButton(context.getResources().getString(android.R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

					}
				});
		final AlertDialog deleteDialog = deleteBuilder.create();
		deleteDialog.setView(deleteDialogView);

		deleteDialog.setTitle("Global equalizer");
		return deleteDialog;
	}

	/**
	 * Sceglie nuova icona
	 * 
	 * @param context
	 * @param ctx
	 * @param list
	 * @param datasource
	 * @param toRename
	 *            puo essere nodo o Scenario
	 * @return
	 */
	public static AlertDialog.Builder chooseIconDialog(final Context context, final ImageView ctx, final ListView list,
			final SoulissDBHelper datasource, final ISoulissObject toRename) {
		final int savepoint = toRename.getDefaultIconResourceId();
		final SoulissPreferenceHelper opzioni = new SoulissPreferenceHelper(context);
		assertTrue("chooseIconDialog: NOT instanceof", toRename instanceof SoulissNode
				|| toRename instanceof SoulissScene || toRename instanceof SoulissTypical);
		final AlertDialog.Builder alert2 = new AlertDialog.Builder(context);
		// alert2.setTitle("Choose " + toRename.toString() + " icon");
		alert2.setTitle(context.getString(R.string.dialog_choose_icon) + " " + toRename.getNiceName());

		alert2.setIcon(android.R.drawable.ic_dialog_dialer);
		// loads gallery and requires icon selection*/
		final EcoGallery gallery = new EcoGallery(context);
		// final Gallery gallery = new Gallery(context);
		// Gallery gallery = (Gallery) findViewById(R.id.gallery);
		// gallery.setMinimumHeight(300);
		// gallery.setLayoutParams(new Layo);
		gallery.setAdapter(new SoulissIconAdapter(context));
		alert2.setView(gallery);

		/*
		 * gallery.setOnItemClickListener(new OnItemClickListener() {
		 * 
		 * @Override public void onItemClick(EcoGalleryAdapterView<?> parent,
		 * View view, int position, long id) { SoulissIconAdapter ad =
		 * (SoulissIconAdapter) gallery.getAdapter();
		 * toRename.setIconResourceId(ad.getItemResId(position)); } });
		 * /*gallery.setOnItemClickListener(new OnItemClickListener() {
		 * 
		 * @Override public void onItemClick(AdapterView<?> arg0, View arg1, int
		 * position, long arg3) { SoulissIconAdapter ad = (SoulissIconAdapter)
		 * gallery.getAdapter();
		 * toRename.setIconResourceId(ad.getItemResId(position)); } });
		 */

		alert2.setPositiveButton(context.getResources().getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						int pos = gallery.getSelectedItemPosition();
						SoulissIconAdapter ad = (SoulissIconAdapter) gallery.getAdapter();
						toRename.setIconResourceId(ad.getItemResId(pos));
						if (toRename instanceof SoulissNode) {
							datasource.createOrUpdateNode((SoulissNode) toRename);
							if (list != null) {
								List<SoulissNode> goer = datasource.getAllNodes();
								SoulissNode[] nodiArray = new SoulissNode[goer.size()];
								nodiArray = goer.toArray(nodiArray);
								NodesListAdapter nodesAdapter = new NodesListAdapter(context, nodiArray, opzioni);
								// Adapter della lista
								list.setAdapter(nodesAdapter);
								list.invalidateViews();
							}
						} else if (toRename instanceof SoulissScene) {
							datasource.createOrUpdateScene((SoulissScene) toRename);
							if (list != null) {
								LinkedList<SoulissScene> goer = datasource.getScenes(SoulissClient.getAppContext());
								SoulissScene[] scenesArray = new SoulissScene[goer.size()];
								scenesArray = goer.toArray(scenesArray);
								SceneListAdapter progsAdapter = new SceneListAdapter(context, scenesArray, opzioni);
								// Adapter della lista
								list.setAdapter(progsAdapter);
								list.invalidateViews();
							}
						} else {
							((SoulissTypical) toRename).getTypicalDTO().persist();
							if (list != null) {
								TypicalsListAdapter ta = (TypicalsListAdapter) list.getAdapter();
								ta.notifyDataSetChanged();
								list.invalidateViews();
							}
						}
						ctx.setImageResource(toRename.getDefaultIconResourceId());
						ctx.invalidate();

					}
				});

		alert2.setNegativeButton(context.getResources().getString(android.R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
						toRename.setIconResourceId(savepoint);
					}
				});

		return alert2;
	}

	/**
	 * dialog per il check della rete non piu usata ma non mi va di toglierla
	 * 
	 * @param preferencesActivity
	 * @param local_ip
	 *            dalle opzioni, forse null
	 * @param public_ip
	 *            dalle opzioni, forse null
	 * @param opzioni
	 * @return il dialogo da mostrare
	 */
	/*
	 * public static ProgressDialog checkConnectionResultDialog(final Activity
	 * preferencesActivity, final String local_ip, final String public_ip, final
	 * SoulissPreferenceHelper opzioni) { final ProgressDialog mProgressDialog =
	 * new ProgressDialog(preferencesActivity);
	 * mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
	 * mProgressDialog
	 * .setTitle(SoulissClient.getAppContext().getString(R.string.
	 * dialog_net_test));
	 * mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	 * 
	 * new Thread() { public void run() { preferencesActivity.runOnUiThread(new
	 * Runnable() { public void run() {
	 * mProgressDialog.setMessage(SoulissClient.
	 * getAppContext().getString(R.string.dialog_local_test) + "\n"); } });
	 * 
	 * final StringBuilder memo = new StringBuilder(); try { Thread.sleep(400);
	 * if (local_ip != null && "".compareTo(local_ip) != 0) { // IP LOCALE if
	 * (JSONHelper.checkSoulissHttp(local_ip, opzioni.getRemoteTimeoutPref() *
	 * 2)) {
	 * memo.append(SoulissClient.getAppContext().getString(R.string.dialog_local_ok
	 * ) + "\n"); // opzioni.setBestAddress(); } else {
	 * memo.append(SoulissClient
	 * .getAppContext().getString(R.string.dialog_warn_wifi) + "\n"); }
	 * preferencesActivity.runOnUiThread(new Runnable() { public void run() {
	 * mProgressDialog.setMessage(memo.toString()); } }); } if (public_ip !=
	 * null && "".compareTo(public_ip) != 0) { // PUBLIC TEST, NON Else perche
	 * non esclusivo memo.append("\n" +
	 * SoulissClient.getAppContext().getString(R.string.dialog_remote_test));
	 * preferencesActivity.runOnUiThread(new Runnable() { public void run() {
	 * mProgressDialog.setMessage(memo.toString()); } }); Thread.sleep(500); //
	 * TEST REMOTO if (JSONHelper.checkSoulissHttp(public_ip,
	 * opzioni.getRemoteTimeoutPref() * 3)) { //
	 * memo.delete(memo.indexOf("Executing remote test..."), // memo.length());
	 * memo.append("\n" +
	 * SoulissClient.getAppContext().getString(R.string.dialog_remote_ok) +
	 * "\n"); // opzioni.setBestAddress(); } else { memo.delete(
	 * memo.indexOf(SoulissClient
	 * .getAppContext().getString(R.string.dialog_remote_test)), memo.length());
	 * memo
	 * .append(SoulissClient.getAppContext().getString(R.string.dialog_remote_notok
	 * )); } preferencesActivity.runOnUiThread(new Runnable() { public void
	 * run() { mProgressDialog.setMessage(memo.toString()); } }); } else if
	 * (local_ip == null || "".compareTo(local_ip) == 0) {
	 * memo.append(SoulissClient
	 * .getAppContext().getString(R.string.dialog_notconf) + "\n");
	 * preferencesActivity.runOnUiThread(new Runnable() { public void run() {
	 * mProgressDialog.setMessage(memo.toString()); } }); }
	 * 
	 * // TODO togliere ?? mProgressDialog.getListView();
	 * 
	 * } catch (final Exception e) { preferencesActivity.runOnUiThread(new
	 * Runnable() { public void run() {
	 * mProgressDialog.setMessage("Test Failed: " + e.getLocalizedMessage()); }
	 * }); Log.e(TAG, "Connection test FAILED", e); } } }.start();
	 * 
	 * mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "OK", new
	 * DialogInterface.OnClickListener() { public void onClick(DialogInterface
	 * dialog, int whichButton) { mProgressDialog.dismiss(); } });
	 * 
	 * return mProgressDialog;
	 * 
	 * }
	 */

	/**
	 * Dialogo creazione DB
	 * 
	 * @param preferencesActivity
	 * @param ip
	 * @return
	 */
	public static AlertDialog.Builder updateSoulissDBDialog(final Activity preferencesActivity, final String ip,
			final SoulissPreferenceHelper opts) {
		// ProgressDialog.Builder alert = new
		// ProgressDialog.Builder(preferencesActivity);
		AlertDialog.Builder alert = new AlertDialog.Builder(preferencesActivity);
		// final SharedPreferences customSharedPreference =
		// preferencesActivity.getSharedPreferences("SoulissPrefs",
		// Activity.MODE_PRIVATE);
		alert.setTitle(preferencesActivity.getResources().getString(R.string.dialog_warn_db));
		alert.setIcon(android.R.drawable.ic_dialog_alert);
		if (opts.isSoulissReachable()) {
			// alert.setIcon()
			alert.setMessage(preferencesActivity.getResources().getString(R.string.dialog_create_db) + ip
					+ preferencesActivity.getResources().getString(R.string.dialog_create_db2));

			alert.setPositiveButton(preferencesActivity.getResources().getString(android.R.string.ok),
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int whichButton) {

							new Thread() {
								public void run() {
									Looper.prepare();
									UDPHelper.dbStructRequest(opts);
								}
							}.start();

						}
					});
		} else {
			alert.setMessage(preferencesActivity.getResources().getString(R.string.souliss_unavailable));

		}

		alert.setNegativeButton(SoulissClient.getAppContext().getResources().getString(android.R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

		return alert;

	}

}
