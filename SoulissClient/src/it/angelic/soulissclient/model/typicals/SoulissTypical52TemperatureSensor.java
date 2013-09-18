package it.angelic.soulissclient.model.typicals;

import it.angelic.soulissclient.Constants;
import it.angelic.soulissclient.R;
import it.angelic.soulissclient.R.color;
import it.angelic.soulissclient.adapters.TypicalsListAdapter;
import it.angelic.soulissclient.helpers.HalfFloatUtils;
import it.angelic.soulissclient.helpers.SoulissPreferenceHelper;
import it.angelic.soulissclient.model.ISoulissTypical;
import it.angelic.soulissclient.model.SoulissTypical;

import java.text.DecimalFormat;
import java.util.Calendar;

import android.content.Context;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Occupa DUE slot, quindi l'output viene dal suo e dal suo fratello destro (related)
 * 
 * @author Ale
 * 
 */
public class SoulissTypical52TemperatureSensor extends SoulissTypical implements ISoulissTypical {
	private final DecimalFormat df = new DecimalFormat("###.##");
	public SoulissTypical52TemperatureSensor(SoulissPreferenceHelper pre) {
		super(pre);
	}

	int maxTemp;
	int minTemp;
	/**
	 * 
	 */
	private static final long serialVersionUID = 3784476625375361669L;

	/**
	 * La conversione del half fp si basa su HalfFloatUtils.toFloat
	 */
	public float getOutputFloat() {
		int miofratello = ((SoulissTypical) getParentNode().getTypical((short) (typicalDTO.getSlot() + 1))).getTypicalDTO().getOutput();
		//ora ho i due bytes, li converto
		int shifted = miofratello << 8;
		Log.i(Constants.TAG,"first:"+ Long.toHexString((long) typicalDTO.getOutput())+" second:"+ Long.toHexString((long) miofratello)+ "SENSOR Reading:" + Long.toHexString((long) shifted + typicalDTO.getOutput()) );

		float ret = HalfFloatUtils.toFloat(shifted + typicalDTO.getOutput());

	    return ret;

	}

	public String getOutputCelsius() {
		

		return df.format(getOutputFloat() );
	}

	@Override
	public String getOutputDesc() {
		if (Calendar.getInstance().getTime().getTime() - typicalDTO.getRefreshedAt().getTime().getTime() < (prefs.getDataServiceIntervalMsec()*3))
			return "OK";
		else
			return "STALE";
	}

	@Override
	public void getActionsLayout(TypicalsListAdapter ble, Context ctx, Intent parentIntent, View convertView,
			ViewGroup parent) {
		WindowManager mWinMgr = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		int displayWidth = mWinMgr.getDefaultDisplay().getWidth();
		LinearLayout cont = (LinearLayout) convertView.findViewById(R.id.linearLayoutButtons);
		cont.removeAllViews();
		final TextView cmd = new TextView(ctx);

		cmd.setText(Html.fromHtml("<b>Reading:</b> " + getOutputCelsius() + "°"));
		if (prefs.isLightThemeSelected())
			cmd.setTextColor(ctx.getResources().getColor(R.color.black));
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		cmd.setLayoutParams(lp);
		lp.setMargins(2, 0, 0, 2);
		//cmd.setGravity(Gravity.TOP);
		cont.addView(cmd);

		ProgressBar par = new ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
		// ProgressBar sfumata
		final ShapeDrawable pgDrawable = new ShapeDrawable(new RoundRectShape(Constants.roundedCorners, null, null));
		final LinearGradient gradient = new LinearGradient(0, 0, displayWidth / 2, 0, ctx.getResources().getColor(
				color.aa_blue), ctx.getResources().getColor(color.aa_red), android.graphics.Shader.TileMode.CLAMP);
		pgDrawable.getPaint().setStrokeWidth(3);
		pgDrawable.getPaint().setDither(true);
		pgDrawable.getPaint().setShader(gradient);

		ClipDrawable progress = new ClipDrawable(pgDrawable, Gravity.LEFT, ClipDrawable.HORIZONTAL);
		par.setProgressDrawable(progress);
		par.setBackgroundResource(android.R.drawable.progress_horizontal);
		
		RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		par.setLayoutParams(lp2);
		par.setMax(50);
		par.setProgress(20);
		par.setProgress(0);
		par.setMax(40);
		par.setProgress((int) getOutputFloat());

		cont.addView(par);

	}
}
