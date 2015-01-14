package it.angelic.soulissclient.model.typicals;

import it.angelic.soulissclient.R;
import it.angelic.soulissclient.adapters.TypicalsListAdapter;
import it.angelic.soulissclient.helpers.SoulissPreferenceHelper;
import it.angelic.soulissclient.model.ISoulissTypical;
import it.angelic.soulissclient.model.SoulissCommand;
import it.angelic.soulissclient.model.SoulissTypical;

import java.util.ArrayList;
import java.util.Calendar;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Handle one digital output based on hardware and software commands, output can
 * be timed out.
 * 
 * This logic can be used for lights, wall socket and all the devices that has
 * an ON/OFF behavior.
 * 
 * @author Ale
 * 
 */
public class SoulissTypical42AntiTheftPeer extends SoulissTypical implements ISoulissTypical {

	private static final long serialVersionUID = 4553488985062232592L;

	// Context ctx;

	public SoulissTypical42AntiTheftPeer(SoulissPreferenceHelper fg) {
		super(fg);
	}

	@Override
	public ArrayList<SoulissCommand> getCommands(Context ctx) {
		// ritorna le bozze dei comandi, da riempire con la schermata addProgram
		ArrayList<SoulissCommand> ret = new ArrayList<SoulissCommand>();

		//NO COMMANDS

		return ret;
	}

	/**
	 * Ottiene il layout del pannello comandi
	 * 
	 * @param ble
	 * @param ctx
	 * @param parentIntent
	 * @param convertView
	 * @param parent
	 */
	@Override
	public void getActionsLayout(final TypicalsListAdapter ble, Context ctx, final Intent parentIntent,
			View convertView, final ViewGroup parent) {
		LinearLayout cont = (LinearLayout) convertView.findViewById(R.id.linearLayoutButtons);
		cont.removeAllViews();
		//NO SW COMMANDS

	}
	@Override
	public void setOutputDescView(TextView textStatusVal) {
		textStatusVal.setText(getOutputDesc());
		if (typicalDTO.getOutput() == Constants.Souliss_T4n_Alarm ||
				(Calendar.getInstance().getTime().getTime() - typicalDTO.getRefreshedAt().getTime().getTime() > (prefs.getDataServiceIntervalMsec()*3))) {
			textStatusVal.setTextColor(ctx.getResources().getColor(R.color.std_red));
			textStatusVal.setBackgroundDrawable(ctx.getResources().getDrawable(R.drawable.borderedbackoff));
		} else {
			textStatusVal.setTextColor(ctx.getResources().getColor(R.color.std_green));
			textStatusVal.setBackgroundDrawable(ctx.getResources().getDrawable(R.drawable.borderedbackon));
		}
	}
	@Override
	public String getOutputDesc() {
		String ret;
		if (typicalDTO.getOutput() == Constants.Souliss_T4n_RstCmd)
			ret = "OK";
		else if (typicalDTO.getOutput() == Constants.Souliss_T4n_InAlarm || typicalDTO.getOutput() == Constants.Souliss_T4n_Alarm)
			ret = "ALARM";
		else
			ret = "UNKNOWN";
		
		if (Calendar.getInstance().getTime().getTime() - typicalDTO.getRefreshedAt().getTime().getTime() > (prefs.getDataServiceIntervalMsec()*3))
			ret += "(STALE)";
		return ret;
	}

}
