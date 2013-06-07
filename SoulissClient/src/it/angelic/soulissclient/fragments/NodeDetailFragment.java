package it.angelic.soulissclient.fragments;

import static it.angelic.soulissclient.typicals.Constants.Souliss_T11;
import static it.angelic.soulissclient.typicals.Constants.Souliss_T12;
import static it.angelic.soulissclient.typicals.Constants.Souliss_T16;
import static junit.framework.Assert.assertTrue;
import it.angelic.soulissclient.AirConActivity;
import it.angelic.soulissclient.Constants;
import it.angelic.soulissclient.R;
import it.angelic.soulissclient.R.color;
import it.angelic.soulissclient.RGBAdvancedActivity;
import it.angelic.soulissclient.RGBIrActivity;
import it.angelic.soulissclient.SensorDetailActivity;
import it.angelic.soulissclient.SoulissClient;
import it.angelic.soulissclient.SoulissDataService;
import it.angelic.soulissclient.Typical1nDetail;
import it.angelic.soulissclient.adapters.TypicalsListAdapter;
import it.angelic.soulissclient.adapters.TypicalsListAdapter.TypicalViewHolder;
import it.angelic.soulissclient.db.SoulissDBHelper;
import it.angelic.soulissclient.helpers.AlertDialogHelper;
import it.angelic.soulissclient.helpers.SoulissPreferenceHelper;
import it.angelic.soulissclient.model.SoulissNode;
import it.angelic.soulissclient.net.UDPHelper;
import it.angelic.soulissclient.typicals.SoulissTypical;
import it.angelic.soulissclient.typicals.SoulissTypical11;
import it.angelic.soulissclient.typicals.SoulissTypical12;
import it.angelic.soulissclient.typicals.SoulissTypical15;
import it.angelic.soulissclient.typicals.SoulissTypical16AdvancedRGB;
import it.angelic.soulissclient.typicals.SoulissTypical32AirCon;
import it.angelic.soulissclient.typicals.SoulissTypicalHumiditySensor;
import it.angelic.soulissclient.typicals.SoulissTypicalTemperatureSensor;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.LinearGradient;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class NodeDetailFragment extends ListFragment {
	private static final String TAG = "SOULISSCLIENT - Node detail";
	private SoulissNode collected;
	private SoulissPreferenceHelper opzioni;
	private ListView listaTypicalsView;
	private SoulissDBHelper datasource;
	private Timer autoUpdate;
	private ProgressBar par;
	private TextView tt;
	private TypicalsListAdapter ta;
	private TextView upda;

	private SoulissDataService mBoundService;
	private boolean mIsBound;

	public int getShownIndex() {
		if (getArguments() != null)
			return getArguments().getInt("index", 0);
		else
			return 0;
	}

	public static NodeDetailFragment newInstance(int index, SoulissNode content) {
		NodeDetailFragment f = new NodeDetailFragment();

		// Supply index input as an argument.
		Bundle args = new Bundle();
		args.putInt("index", index);
		// Ci metto il nodo dentro
		if (content != null) {
			args.putSerializable("NODO", content);
		}
		f.setArguments(args);

		return f;
	}

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
	private ImageView nodeic;
	private Handler timeoutHandler;
	private boolean mDualPane;
	private SwipeGestureListener gestureListener;

	void doBindService() {
		if (!mIsBound) {
			getActivity().bindService(new Intent(getActivity(), SoulissDataService.class), mConnection,
					Context.BIND_AUTO_CREATE);
			mIsBound = true;
		}
	}

	void doUnbindService() {
		if (mIsBound) {
			getActivity().unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View ret = inflater.inflate(R.layout.frag_nodedetail, container, false);
		return ret;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		opzioni = SoulissClient.getOpzioni();

		if (opzioni.isLightThemeSelected())
			getActivity().setTheme(com.actionbarsherlock.R.style.Theme_Sherlock_Light);
		else
			getActivity().setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		super.onActivityCreated(savedInstanceState);
		timeoutHandler = new Handler();
		setHasOptionsMenu(true);
		datasource = new SoulissDBHelper(getActivity());
		View detailsFrame = getActivity().findViewById(R.id.details);
		mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
		// nodoInfo.removeAllViews();
		tt = (TextView) getActivity().findViewById(R.id.TextViewTypicalsTitle);
		// health = (TextView) findViewById(R.id.TextViewHealth);
		upda = (TextView) getActivity().findViewById(R.id.TextViewNodeUpdate);
		par = (ProgressBar) getActivity().findViewById(R.id.progressBarNodo);

		if (upda == null)
			return;
		Bundle extras = getActivity().getIntent().getExtras();
		// recuper nodo da extra
		if (extras != null && extras.get("NODO") != null)
			collected = (SoulissNode) extras.get("NODO");
		else if (getArguments() != null) {
			collected = (SoulissNode) getArguments().get("NODO");
		} else {
			try {
				// TODO remove this branch
				Log.w(Constants.TAG, "Attempting emergency load");
				collected = datasource.getSoulissNode(getShownIndex());
			} catch (Exception e) {
				Log.e(Constants.TAG, "Error retriving node:" + e.getMessage());
				return;
			}
		}

		assertTrue("NODO NULLO", collected != null);
		getActivity().setTitle(collected.getNiceName());

		// SFONDO
		SoulissClient.setBackground((RelativeLayout) getActivity().findViewById(R.id.containerlista), getActivity()
				.getWindowManager());
		// listaTypicalsView = (ListView) getListView();
		listaTypicalsView = getListView();
		nodeic = (ImageView) getActivity().findViewById(R.id.node_icon);
		// Icona, puo esser nullo dopo rotazione schermo
		if (nodeic != null) {
			nodeic.setImageResource(collected.getDefaultIconResourceId());
			createHeader();
			registerForContextMenu(listaTypicalsView);
		} else
			Log.e(TAG, "porcatroia");

	}

	@SuppressLint("NewApi")
	@Override
	public void onStart() {
		super.onStart();
		if (collected == null || upda == null) {
			return;// no detail selected
		}
		if (Constants.versionNumber >= 11) {
			ActionBar actionBar = getActivity().getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(collected.getNiceName());
		}

		if (opzioni.isDbConfigured()) {
			datasource.open();
			// per il refresh dal dettaglio
			collected = datasource.getSoulissNode(collected.getId());
			doBindService();

			// poll 1 node
			new Thread(new Runnable() {
				@Override
				public void run() {
					/*
					 * UDPHelper.healthRequest(opzioni, 1, collected.getId());
					 * try { Thread.sleep(500); } catch (InterruptedException e)
					 * { e.printStackTrace(); }
					 */
					UDPHelper.pollRequest(opzioni, 1, collected.getId());
				}
			}).start();

			createHeader();
			// tipici dal DB
			List<SoulissTypical> goer = collected.getTypicals();
			ArrayList<SoulissTypical> copy = new ArrayList<SoulissTypical>();
			for (SoulissTypical soulissTypical : goer) {
				if (!soulissTypical.isEmpty() && !soulissTypical.isRelated())// tolgo
																				// el.
																				// vuoti
					copy.add(soulissTypical);
			}
			SoulissTypical[] typs = new SoulissTypical[copy.size()];
			typs = copy.toArray(typs);

			ta = new TypicalsListAdapter(getActivity(), mBoundService, typs, getActivity().getIntent(), datasource,
					opzioni);
			listaTypicalsView = getListView();
			// Adapter della lista
			listaTypicalsView.setAdapter(ta);

			listaTypicalsView.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					TypicalViewHolder holder = (TypicalViewHolder) arg1.getTag();
					showDetails(arg2, (SoulissTypical) holder.data);
				}
			});
			gestureListener = new SwipeGestureListener(getActivity());
			listaTypicalsView.setOnTouchListener(gestureListener);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		View detailsFrame = getActivity().findViewById(R.id.details);
		mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
		super.onConfigurationChanged(newConfig);
	}

	/**
	 * Helper function to show the details of a selected item, either by
	 * displaying a fragment in-place in the current UI, or starting a whole new
	 * activity in which it is displayed.
	 */
	private void showDetails(int index, SoulissTypical target) {
		// mCurCheckPosition = index;

		if (mDualPane) {
			ListView li = getListView();
			li.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			// listaNodiView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			// We can display everything in-place with fragments, so update
			// the list to highlight the selected item and show the data.
			li.setItemChecked(index, true);
			// Check what fragment is currently shown, replace if needed.
			Fragment details = getFragmentManager().findFragmentById(R.id.details);
			// Istanzia e ci mette l'indice
			if (target instanceof SoulissTypicalHumiditySensor || target instanceof SoulissTypicalTemperatureSensor)
				details = SensorDetailFragment.newInstance(index, target);
			else if (target instanceof SoulissTypical16AdvancedRGB)
				details = RGBAdvancedFragment.newInstance(index, target);
			else if (target instanceof SoulissTypical11 || target instanceof SoulissTypical12)
				details = Typical1nFragment.newInstance(index, target);
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			if (opzioni.isAnimationsEnabled())
				ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
			ft.replace(R.id.details, details);
			// ft.addToBackStack(null);

			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
			ft.commit();

		} else {

			if (target.isSensor()) {
				Log.d(TAG, getResources().getString(R.string.manual_showing_typ) + index);
				// Activity Dettaglio nodo
				Intent nodeDatail = new Intent(getActivity(), SensorDetailActivity.class);
				nodeDatail.putExtra("TIPICO", target);
				NodeDetailFragment.this.startActivity(nodeDatail);
				if (opzioni.isAnimationsEnabled())
					getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

			} else if (target.getTypicalDTO().getTypical() == it.angelic.soulissclient.typicals.Constants.Souliss_T32_IrCom_AirCon) {
				Intent nodeDatail = new Intent(getActivity(), AirConActivity.class);
				nodeDatail.putExtra("TIPICO", (SoulissTypical32AirCon) target);
				nodeDatail.putExtra("RELATO", collected.getTypical((short) (target.getTypicalDTO().getSlot() + 1)));
				NodeDetailFragment.this.startActivity(nodeDatail);
				if (opzioni.isAnimationsEnabled())
					getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

			} else if (target.getTypicalDTO().getTypical() == it.angelic.soulissclient.typicals.Constants.Souliss_T1n_RGB) {
				Intent nodeDatail = new Intent(getActivity(), RGBIrActivity.class);
				nodeDatail.putExtra("TIPICO", (SoulissTypical15) target);
				NodeDetailFragment.this.startActivity(nodeDatail);
				if (opzioni.isAnimationsEnabled())
					getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

			} else if (target.getTypicalDTO().getTypical() == Souliss_T16) {
				Intent nodeDatail = new Intent(getActivity(), RGBAdvancedActivity.class);
				nodeDatail.putExtra("TIPICO", (SoulissTypical16AdvancedRGB) target);
				NodeDetailFragment.this.startActivity(nodeDatail);
				if (opzioni.isAnimationsEnabled())
					getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
			} else if (target.getTypicalDTO().getTypical() == Souliss_T11
					|| target.getTypicalDTO().getTypical() == Souliss_T12) {
				Intent nodeDatail = new Intent(getActivity(), Typical1nDetail.class);
				nodeDatail.putExtra("TIPICO", (SoulissTypical) target);
				NodeDetailFragment.this.startActivity(nodeDatail);
				if (opzioni.isAnimationsEnabled())
					getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
			}

		}
	}

	private void refreshHeader() {
		par.setProgress(collected.getHealth());
		par.setProgress(collected.getHealth());
		upda.setText(getResources().getString(R.string.update) + " " + Constants.getTimeAgo(collected.getRefreshedAt()));
	}

	/**
	 * Riga grigia cra spazio
	 * 
	 * @param activity
	 * @return
	 */
	private void createHeader() {

		if (collected.getDefaultIconResourceId() != 0)
			nodeic.setImageResource(collected.getDefaultIconResourceId());

		// Animazione icona nodo
		/*
		 * if (opzioni.getTextFx()) { Animation a =
		 * AnimationUtils.loadAnimation(getApplicationContext(),
		 * R.anim.scalerotale); a.reset(); nodeic.startAnimation(a); }
		 */

		upda.setText(getResources().getString(R.string.update) + Constants.getTimeAgo(collected.getRefreshedAt()));
		par.setMax(Constants.MAX_HEALTH);

		// ProgressBar sfumata
		final ShapeDrawable pgDrawable = new ShapeDrawable(new RoundRectShape(Constants.roundedCorners, null, null));
		final LinearGradient gradient = new LinearGradient(0, 0, SoulissClient.getDisplayWidth() / 2, 0, getResources()
				.getColor(color.aa_red), getResources().getColor(color.aa_green),
				android.graphics.Shader.TileMode.CLAMP);
		pgDrawable.getPaint().setStrokeWidth(3);
		pgDrawable.getPaint().setDither(true);
		pgDrawable.getPaint().setShader(gradient);

		ClipDrawable progress = new ClipDrawable(pgDrawable, Gravity.LEFT, ClipDrawable.HORIZONTAL);
		par.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.progress_horizontal));
		par.setProgressDrawable(progress);
		par.setMax(50);
		par.setProgress(20);
		par.setProgress(0); // <-- BUG Android
		par.setMax(Constants.MAX_HEALTH);
		par.setProgress(collected.getHealth());

		Log.d(TAG, "Setting bar at " + collected.getHealth() + " win width=" + SoulissClient.getDisplayWidth() / 2);

		// Font dei titoli
		if ("def".compareToIgnoreCase(opzioni.getPrefFont()) != 0) {
			Typeface font = Typeface.createFromAsset(getActivity().getAssets(), opzioni.getPrefFont());
			tt.setTypeface(font, Typeface.NORMAL);
			// titolo.setTypeface(font, Typeface.NORMAL);
		}

		// tt.setTextSize(TypedValue.COMPLEX_UNIT_SP,
		// opzioni.getListDimensTesto() + tt.getTextSize());
		// upda.setTextSize(TypedValue.COMPLEX_UNIT_SP,
		// opzioni.getListDimensTesto() + tt.getTextSize());
		return;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		// Rinomina nodo e scelta icona
		inflater.inflate(R.menu.typical_ctx_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		TypicalsListAdapter ada = (TypicalsListAdapter) listaTypicalsView.getAdapter();

		long arrayAdapterPosition = info.position;
		final SoulissTypical todoItem = (SoulissTypical) ada.getItem((int) arrayAdapterPosition);

		switch (item.getItemId()) {
		case R.id.rinomina:
			AlertDialog.Builder alert3 = AlertDialogHelper.renameSoulissObjectDialog(getActivity(), null,
					listaTypicalsView, datasource, todoItem);
			alert3.show();
			break;
		case R.id.sceglicona:
			SoulissTypical convertView = (SoulissTypical) listaTypicalsView.getItemAtPosition(item.getOrder());
			ImageView at = new ImageView(getActivity());
			at.setImageResource(convertView.getDefaultIconResourceId());
			AlertDialog.Builder alert2 = AlertDialogHelper.chooseIconDialog(getActivity(), at, listaTypicalsView,
					datasource, todoItem);
			alert2.show();
			break;
		default:
			return super.onContextItemSelected(item);
		}

		return true;
	}

	@Override
	public void onResume() {
		super.onResume();
		// pezza
		datasource.open();
		doBindService();
		IntentFilter filtere = new IntentFilter();
		filtere.addAction("it.angelic.soulissclient.GOT_DATA");
		filtere.addAction(it.angelic.soulissclient.net.Constants.CUSTOM_INTENT_SOULISS_RAWDATA);
		getActivity().registerReceiver(datareceiver, filtere);

		// this is only used for refresh UI
		IntentFilter filtera = new IntentFilter();
		filtera.addAction(it.angelic.soulissclient.net.Constants.CUSTOM_INTENT_SOULISS_TIMEOUT);
		getActivity().registerReceiver(timeoutReceiver, filtera);

		autoUpdate = new Timer();
		autoUpdate.schedule(new TimerTask() {
			@Override
			public void run() {
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						if (listaTypicalsView != null)
							listaTypicalsView.invalidateViews();
					}
				});
			}
		}, 100, Constants.GUI_UPDATE_INTERVAL); // updates GUI each 40 secs
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				// nothing to do here...
			} else {
				getActivity().finish();
				if (opzioni.isAnimationsEnabled())
					getActivity().overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPause() {
		super.onPause();
		autoUpdate.cancel();
		getActivity().unregisterReceiver(datareceiver);
		getActivity().unregisterReceiver(timeoutReceiver);
		doUnbindService();
		timeoutHandler.removeCallbacks(timeExpired);
	}

	// Aggiorna il feedback
	private BroadcastReceiver datareceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "Detected data arrival, refresh from DB");
			// cancel timeout
			timeoutHandler.removeCallbacks(timeExpired);
			if (listaTypicalsView == null)
				return;
			datasource.open();
			collected = datasource.getSoulissNode(collected.getId());
			refreshHeader();

			List<SoulissTypical> goer = collected.getTypicals();
			SoulissTypical[] typs = new SoulissTypical[goer.size()];
			typs = goer.toArray(typs);
			for (int i = 0; i < typs.length; i++) {
				typs[i].setPrefs(opzioni);
				typs[i].setCtx(context);
			}
			ta.setTypicals(typs);
			ta.notifyDataSetChanged();

			// save index and top position
			int index = listaTypicalsView.getFirstVisiblePosition();
			View v = listaTypicalsView.getChildAt(0);
			int top = (v == null) ? 0 : v.getTop();
			// Adapter della lista
			listaTypicalsView.setAdapter(ta);
			listaTypicalsView.invalidateViews();
			listaTypicalsView.setSelectionFromTop(index, top);
		}
	};

	Runnable timeExpired = new Runnable() {
		@Override
		public void run() {
			Log.e(TAG, "TIMEOUT detected!!!");
			// Reset cachedaddress
			opzioni.reload();
		}
	};
	// meccanismo per timeout detection
	private BroadcastReceiver timeoutReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle extras = intent.getExtras();
			int delay = extras.getInt("REQUEST_TIMEOUT_MSEC");
			Log.w(TAG, "Posting timeout, delay " + delay);
			timeoutHandler.postDelayed(timeExpired, delay);
		}
	};

	class SwipeGestureListener extends SimpleOnGestureListener implements OnTouchListener {
		Context context;
		GestureDetector gDetector;
		static final int SWIPE_MIN_DISTANCE = 120;
		static final int SWIPE_MAX_OFF_PATH = 250;
		static final int SWIPE_THRESHOLD_VELOCITY = 200;

		public SwipeGestureListener() {
			super();
		}

		public SwipeGestureListener(Context context) {
			this(context, null);
		}

		public SwipeGestureListener(Context context, GestureDetector gDetector) {

			if (gDetector == null)
				gDetector = new GestureDetector(context, this);

			this.context = context;
			this.gDetector = gDetector;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

			final int position = listaTypicalsView.pointToPosition(Math.round(e1.getX()), Math.round(e1.getY()));

			SoulissTypical countryName = (SoulissTypical) listaTypicalsView.getItemAtPosition(position);

			if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
				if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH
						|| Math.abs(velocityY) < SWIPE_THRESHOLD_VELOCITY) {
					return false;
				}
				/*if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE) {
					Toast.makeText(getActivity(), "bottomToTop" + countryName, Toast.LENGTH_SHORT).show();
				} else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE) {
					Toast.makeText(getActivity(), "topToBottom  " + countryName, Toast.LENGTH_SHORT).show();
				}*/
			} else {
				if (Math.abs(velocityX) < SWIPE_THRESHOLD_VELOCITY) {
					return false;
				}
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE) {
					// Toast.makeText(getActivity(),
					// "swipe RightToLeft " + countryName, 5000).show();
					showDetails(position, countryName);
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE) {
					Toast.makeText(getActivity(), "swipe LeftToright  " + countryName, Toast.LENGTH_SHORT).show();
					// TODO close current
					if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
						// nothing to do here...
					} else {
						getActivity().finish();
						if (opzioni.isAnimationsEnabled())
							getActivity().overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
					}
				}
			}

			return super.onFling(e1, e2, velocityX, velocityY);

		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {

			return gDetector.onTouchEvent(event);
		}

		public GestureDetector getDetector() {
			return gDetector;
		}

	}
}
