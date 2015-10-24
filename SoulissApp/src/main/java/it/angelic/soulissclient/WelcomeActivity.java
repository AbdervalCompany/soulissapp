package it.angelic.soulissclient;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import java.io.File;
import java.io.IOException;

import it.angelic.soulissclient.db.SoulissDB;
import it.angelic.soulissclient.db.SoulissDBHelper;
import it.angelic.soulissclient.helpers.Eula;
import it.angelic.soulissclient.helpers.ExportDatabaseCSVTask;
import it.angelic.soulissclient.helpers.ImportDatabaseCSVTask;
import it.angelic.soulissclient.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class WelcomeActivity extends FragmentActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
    Handler mHideHandler = new Handler();
    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };
    private SharedPreferences soulissConfigurationPreference;

    public static void saveWelcomeDisabledPreference(SharedPreferences prefs, boolean val) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("welcome_disabled", val);
        edit.commit();
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        soulissConfigurationPreference = getSharedPreferences("SoulissConfigPrefs", Activity.MODE_PRIVATE);
        setContentView(R.layout.activity_welcome);

        // final TextView welcomeSkipText = (TextView) findViewById(R.id.welcome_skip_text);
        final Button welcomeTourButton = (Button) findViewById(R.id.welcome_tour_button);
        final CheckBox welcomeEnableCheckBox = (CheckBox) findViewById(R.id.welcome_enable_checkbox);
        welcomeEnableCheckBox.setChecked(soulissConfigurationPreference.getBoolean("welcome_disabled", false));
        welcomeEnableCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveWelcomeDisabledPreference(soulissConfigurationPreference, welcomeEnableCheckBox.isChecked());
            }
        });
        final Spinner confSpinner = (Spinner) findViewById(R.id.configSpinner);
        confSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.w(Constants.TAG, "Config spinner selected val:" + confSpinner.getSelectedItem());
                String previousConfig = soulissConfigurationPreference.getString("current_config", "");
                final File importDir = new File(Environment.getExternalStorageDirectory(), "//Souliss");
                SharedPreferences.Editor newConfig = soulissConfigurationPreference.edit();
                newConfig.putString("current_config", confSpinner.getSelectedItem().toString());
                //DEMO
                if (confSpinner.getSelectedItem().equals(getResources().getStringArray(R.array.configChooserArray)[0])) {
                    if (!previousConfig.equals("")) {

                        //save Old DB and config
                        File filePrefs = new File(importDir, previousConfig + "_SoulissDB.csv.prefs");
                        ExportDatabaseCSVTask.saveSharedPreferencesToFile(WelcomeActivity.this, filePrefs);
                        //locateDB
                        SoulissDBHelper db = new SoulissDBHelper(WelcomeActivity.this);
                        String DbPath = SoulissDBHelper.getDatabase().getPath();
                        File oldDb = new File(DbPath + SoulissDB.DATABASE_NAME);
                        Log.w(Constants.TAG, "Saving old DB: " + oldDb.getTotalSpace());


                    }
                    File filePrefs;
                    try {
                        filePrefs = new File(importDir, "DEMO_SoulissDB.csv.prefs");
                        if (!filePrefs.exists())
                            throw new Resources.NotFoundException();

                    } catch (Resources.NotFoundException e) {
                        filePrefs = new File(importDir, "DEMO_SoulissDB.csv.prefs");
                        try {
                            ///se non esiste la demo, Creala
                            filePrefs.createNewFile();
                            SharedPreferences newDefault = PreferenceManager.getDefaultSharedPreferences(WelcomeActivity.this);
                            SharedPreferences.Editor demo = newDefault.edit();
                            demo.putString("edittext_IP_pubb", "demo.souliss.net");
                            demo.putString("edittext_IP", "10.14.10.77");
                            demo.commit();
                            ExportDatabaseCSVTask.saveSharedPreferencesToFile(WelcomeActivity.this, filePrefs);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        Log.e(Constants.TAG, "Errore import prefs", e);
                    }
                    ImportDatabaseCSVTask.loadSharedPreferencesFromFile(WelcomeActivity.this, filePrefs);
                    Log.e(Constants.TAG, "DEMO prefs loaded");

                }
                //https://github.com/ribico/souliss_demo/blob/master/souliss_demo.ino
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        welcomeTourButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //here we've already chosen config and loaded right files
                startSoulissMainActivity();
            }
        });
        /*welcomeSkipText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSoulissMainActivity();
            }
        });

*/
        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        /* show EULA if not accepted */
        Eula.show(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /* check for first time run */
        welcomeEnabledCheck();
    }

    private void startSoulissMainActivity() {
        Intent myIntent = new Intent(WelcomeActivity.this, LauncherActivity.class);
        startActivity(myIntent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void welcomeEnabledCheck() {
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        //SharedPreferences.Editor edit = prefs.edit();
        //edit.remove("first_time_run");
        //edit.commit();
        boolean firstTimeRun = soulissConfigurationPreference.getBoolean("welcome_disabled", true);
        //boolean firstTimeRun = true;
        if (firstTimeRun) {
            startSoulissMainActivity();

        } else {
            //let the user choose config
        }
    }
}
