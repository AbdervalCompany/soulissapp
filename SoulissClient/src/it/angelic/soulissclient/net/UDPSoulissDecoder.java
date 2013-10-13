package it.angelic.soulissclient.net;

import static it.angelic.soulissclient.model.typicals.Constants.Souliss_T41_Antitheft_Main;
import static it.angelic.soulissclient.model.typicals.Constants.Souliss_T4n_InAlarm;
import static junit.framework.Assert.assertEquals;
import it.angelic.soulissclient.R;
import it.angelic.soulissclient.SoulissClient;
import it.angelic.soulissclient.SoulissDataService;
import it.angelic.soulissclient.Typical4nDetail;
import it.angelic.soulissclient.db.SoulissCommandDTO;
import it.angelic.soulissclient.db.SoulissDBLowHelper;
import it.angelic.soulissclient.db.SoulissTriggerDTO;
import it.angelic.soulissclient.db.SoulissTypicalDTO;
import it.angelic.soulissclient.helpers.SoulissPreferenceHelper;
import it.angelic.soulissclient.model.SoulissNode;
import it.angelic.soulissclient.model.SoulissTrigger;
import it.angelic.soulissclient.model.SoulissTypical;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Classe per il decode dei pacchetti nativi souliss 
 * 
 * This class decodes incoming Souliss packets, starting from decodevNet
 * 
 * @author Ale
 * 
 */
public class UDPSoulissDecoder {

	SoulissPreferenceHelper opzioni;
	private SoulissDBLowHelper database;
	private SharedPreferences soulissSharedPreference;
	private Context context;

	public UDPSoulissDecoder(SoulissPreferenceHelper opts, Context ctx) {
		this.opzioni = opts;
		this.context = ctx;
		database = new SoulissDBLowHelper(ctx);
		soulissSharedPreference = opts.getContx().getSharedPreferences("SoulissPrefs", Activity.MODE_PRIVATE);
		database.open();
	}

	/**
	 * processa il pacchetto UDP ricevuto e agisce di condeguenza
	 * 
	 * @param packet
	 *            incoming datagram
	 */
	public void decodeVNetDatagram(DatagramPacket packet) {
		int checklen = packet.getLength();
		// Log.d("UDPDecoder", "** Packet received");
		ArrayList<Short> mac = new ArrayList<Short>();
		for (int ig = 7; ig < checklen; ig++) {
			mac.add((short) (packet.getData()[ig] & 0xFF));
		}

		// 0xf 0xe 0x17 0x64 0x1 0x11 0x0 0x18 0x0 0x0 0x0 0x3 0xa 0x8 0xa
		// il primo byte dev essere la dimensione
		if (checklen != packet.getData()[0]) {
			StringBuilder dump = new StringBuilder();
			for (int ig = 0; ig < checklen; ig++) {
				// 0xFF & buf[index]
				dump.append("0x" + Long.toHexString(0xFF & packet.getData()[ig]) + " ");
				// dump.append(":"+packet.getData()[ig]);
			}
			Log.e("UDPDecoder", "**WRONG PACKET SIZE: " + packet.getData()[0] + "bytes\n" + "Actual size: " + checklen
					+ "\n" + dump.toString());
		} else {
			decodeMacaco(mac);
		}

		/*
		 * DEBUG PACCHETTO StringBuilder dump = new StringBuilder(); for (int ig
		 * = 0; ig < checklen; ig++) { // 0xFF & buf[index] dump.append("0x" +
		 * Long.toHexString(0xFF & packet.getData()[ig]) + " "); //
		 * dump.append(":"+packet.getData()[ig]); } Log.d("UDPDecoder", "***" +
		 * dump.toString());
		 */
		// Qualcosa ho ricevuto, invia broadcast
		Intent i = new Intent();
		i.putExtra("MACACO", mac);
		i.setAction(Constants.CUSTOM_INTENT_SOULISS_RAWDATA);
		opzioni.getContx().sendBroadcast(i);
		// resetta backoff irraggiungibilit�
		opzioni.resetBackOff();

	}

	/**
	 * Decodes lower level MaCaCo packet
	 * 
	 * @param macacoPck
	 */
	private void decodeMacaco(ArrayList<Short> macacoPck) {
		int functionalCode = macacoPck.get(0);
		// int putIn = mac.get(1);
		// PUTIN :) 2 byte
		// STARTOFFSET 1 byte
		// NUMBEROF 1 byte
		int startOffset = macacoPck.get(3);
		int numberOf = macacoPck.get(4);
		Log.d("UDPDecoder", "** Macaco IN: Start Offset:" + startOffset + ", Number of " + numberOf);
		switch (functionalCode) {
		case Constants.Souliss_UDP_function_subscribe_data:
			Log.d("UDPDecoder", "** Subscription answer");
			decodeStateRequest(macacoPck);
			break;
		case Constants.Souliss_UDP_function_poll_resp:
			Log.d("UDPDecoder", "** Poll answer");
			decodeStateRequest(macacoPck);
			processTriggers();
			break;
		case Constants.Souliss_UDP_function_ping_resp:
			// assertEquals(mac.size(), 8);
			Log.d("UDPDecoder", "** Ping response bytes " + macacoPck.size());
			decodePing(macacoPck);
			break;
		case Constants.Souliss_UDP_function_subscribe_resp:
			Log.d("UDPDecoder", "** State request answer");
			decodeStateRequest(macacoPck);
			processTriggers();
			break;
		case Constants.Souliss_UDP_function_typreq_resp:// Answer for assigned
														// typical logic
			Log.d("UDPDecoder", "** TypReq answer");
			decodeTypRequest(macacoPck);
			break;
		case Constants.Souliss_UDP_function_health_resp:// Answer nodes healty
			Log.d("UDPDecoder", "** Health answer");
			decodeHealthRequest(macacoPck);
			break;
		case Constants.Souliss_UDP_function_db_struct_resp:// Answer nodes
			assertEquals(macacoPck.size(), 9); // healty
			Log.w("UDPDecoder", "** DB Structure answer");
			decodeDBStructRequest(macacoPck);
			break;
		case 0x83:
			Log.e("UDPDecoder", "** (Functional code not supported)");
			break;
		case 0x84:
			Log.e("UDPDecoder", "** (Data out of range)");
			break;
		case 0x85:
			Log.e("UDPDecoder", "** (Subscription refused)");
			break;
		default:
			Log.e("UDPDecoder", "** Unknown functional code: " + functionalCode);
			break;
		}

	}

	private void processTriggers() {
		try {

			List<SoulissNode> ref = database.getAllNodes();
			List<SoulissTrigger> triggers = database.getAllTriggers(context);
			Log.i(Constants.TAG, "checked triggers: " + triggers.size());
			// logThings(refreshedNodes);

			Map<Short, SoulissNode> refreshedNodes = new HashMap<Short, SoulissNode>();

			/* Check antifurto */

			for (SoulissNode soulissNode : ref) {
				refreshedNodes.put(soulissNode.getId(), soulissNode);
				if (opzioni.isAntitheftPresent() && opzioni.isAntitheftNotify()) {// giro
																					// i
																					// tipici
																					// solo
																					// se
																					// seve
					for (SoulissTypical ty : soulissNode.getTypicals()) {
						// check Antitheft
						if (ty.getTypicalDTO().getTypical() == Souliss_T41_Antitheft_Main
								&& ty.getTypicalDTO().getOutput() == Souliss_T4n_InAlarm) {
							sendNotification(context, context.getString(R.string.antitheft_notify),
									context.getString(R.string.antitheft_notify_desc), R.drawable.shield);
							break;
						}
					}
				}
			}
			for (SoulissTrigger soulissTrigger : triggers) {
				SoulissCommandDTO command = soulissTrigger.getCommandDto();
				SoulissTriggerDTO src = soulissTrigger.getTriggerDto();
				SoulissTypical source = refreshedNodes.get(src.getInputNodeId()).getTypical(src.getInputSlotlot());
				// SoulissTypical target =
				// refreshedNodes.get(command.getNodeId()).getTypical(command.getSlot());
				Calendar now = Calendar.getInstance();
				if (!soulissTrigger.getTriggerDto().isActivated()) {
					String op = src.getOp();
					if (">".compareTo(op) == 0 && source.getTypicalDTO().getOutput() > src.getThreshVal()) {
						Log.w(Constants.TAG, "TRIGGERING COMMAND " + command.toString());
						soulissTrigger.getTriggerDto().setActive(true);
						UDPHelper.issueSoulissCommand("" + command.getNodeId(), "" + command.getSlot(),
								SoulissClient.getOpzioni(), command.getType(), "" + command.getCommand());
						command.setExecutedTime(now);
						soulissTrigger.persist(database);
						SoulissDataService.sendNotification(context, command.toString(), soulissTrigger.toString(),
								R.drawable.lighthouse);
					} else if ("<".compareTo(op) == 0 && source.getTypicalDTO().getOutput() < src.getThreshVal()) {
						Log.w(Constants.TAG, "TRIGGERING COMMAND " + command.toString());
						soulissTrigger.getTriggerDto().setActive(true);
						UDPHelper.issueSoulissCommand("" + command.getNodeId(), "" + command.getSlot(),
								SoulissClient.getOpzioni(), command.getType(), "" + command.getCommand());
						soulissTrigger.getCommandDto().setExecutedTime(now);
						soulissTrigger.persist(database);
						SoulissDataService.sendNotification(context, command.toString(), soulissTrigger.toString(),
								R.drawable.lighthouse);
					} else if ("=".compareTo(op) == 0 && source.getTypicalDTO().getOutput() == src.getThreshVal()) {
						Log.w(Constants.TAG, "TRIGGERING COMMAND " + command.toString());
						UDPHelper.issueSoulissCommand("" + command.getNodeId(), "" + command.getSlot(),
								SoulissClient.getOpzioni(), command.getType(), "" + command.getCommand());
						soulissTrigger.getTriggerDto().setActive(true);
						soulissTrigger.getCommandDto().setExecutedTime(now);
						soulissTrigger.persist(database);
						SoulissDataService.sendNotification(context, command.toString(), soulissTrigger.toString(),
								R.drawable.lighthouse);
					}
				}
				// vedi se bisogna disattivare
				else {
					String op = src.getOp();
					if (">".compareTo(op) == 0 && source.getTypicalDTO().getOutput() <= src.getThreshVal()) {
						Log.w(Constants.TAG, "DEACTIVATE TRIGGER " + command.toString());
						soulissTrigger.getTriggerDto().setActive(false);
						soulissTrigger.persist(database);
					} else if ("<".compareTo(op) == 0 && source.getTypicalDTO().getOutput() >= src.getThreshVal()) {
						Log.w(Constants.TAG, "DEACTIVATE TRIGGER " + command.toString());
						soulissTrigger.getTriggerDto().setActive(false);
						soulissTrigger.persist(database);
					} else if ("=".compareTo(op) == 0 && source.getTypicalDTO().getOutput() != src.getThreshVal()) {
						Log.w(Constants.TAG, "DEACTIVATE TRIGGER " + command.toString());
						soulissTrigger.getTriggerDto().setActive(false);
						soulissTrigger.persist(database);
					}
				}

			}
		} catch (IllegalStateException e) {
			Log.e(Constants.TAG, "DB connection was closed, check trigger impossible");
			return;
		}

	}

	/**
	 * Alla ricezione di una risposta ping, aggiorna il cached address F e`
	 * locale, se trovo B e` Remoto
	 * 
	 * @param mac
	 */
	private void decodePing(ArrayList<Short> mac) {
		// int nodes = mac.get(5);
		int putIn = mac.get(1);

		// se trovo F e` locale, se trovo B e` Remoto

		SharedPreferences.Editor editor = soulissSharedPreference.edit();
		// if (soulissSharedPreference.contains("cachedAddress"))
		// editor.remove("cachedAddress");
		boolean alreadyPrivate = soulissSharedPreference.getString("cachedAddress", "").compareTo(
				opzioni.getPrefIPAddress()) == 0;

		if (putIn == 0xB && !alreadyPrivate) {
			opzioni.setCachedAddr(opzioni.getIPPreferencePublic());
			editor.putString("cachedAddress", opzioni.getIPPreferencePublic());
			Log.w(Constants.TAG, "Refreshing cached address: " + opzioni.getIPPreferencePublic());
		} else if (putIn == 0xF) {
			opzioni.setCachedAddr(opzioni.getPrefIPAddress());
			editor.putString("cachedAddress", opzioni.getPrefIPAddress());
			Log.w(Constants.TAG, "Refreshing cached address: " + opzioni.getPrefIPAddress());
		} else if (alreadyPrivate) {
			Log.w(Constants.TAG,
					"Local address already set. I'll NOT overwrite it: "
							+ soulissSharedPreference.getString("cachedAddress", ""));
		} else
			Log.e(Constants.TAG, "Unknown putIn code: " + putIn);
		editor.commit();
	}

	/**
	 * Sovrascrive la struttura I nodi e la struttura dei tipici e richiama
	 * UDPHelper.typicalRequest(opzioni, nodes, 0);
	 * 
	 * @param mac
	 */
	private void decodeDBStructRequest(ArrayList<Short> mac) {
		// Threee static bytes
		assertEquals(4, (short) mac.get(4));
		final int nodes = mac.get(5);
		int maxnodes = mac.get(6);
		int maxTypicalXnode = mac.get(7);
		int maxrequests = mac.get(8);

		Log.i(Constants.TAG, "DB Struct requested,nodes: " + nodes + " maxnodes: " + maxnodes + " maxrequests: "
				+ maxrequests);
		database.createOrUpdateStructure(nodes, maxTypicalXnode);
		// Log.w(Constants.TAG, "Drop DB requested, response: " + mac);

		SharedPreferences.Editor editor = soulissSharedPreference.edit();
		// sistema configurato
		if (soulissSharedPreference.contains("numNodi"))
			editor.remove("numNodi");
		if (soulissSharedPreference.contains("TipiciXNodo"))
			editor.remove("TipiciXNodo");

		editor.putInt("numNodi", nodes);
		editor.putInt("TipiciXNodo", maxTypicalXnode);
		editor.commit();

		// FIXME centralizzare sta roba
		new Thread(new Runnable() {
			@Override
			public void run() {
				UDPHelper.typicalRequest(opzioni, nodes, 0);
			}
		}).start();

	}

	/**
	 * Definizione dei tipici
	 * 
	 * @param mac
	 */
	private void decodeTypRequest(ArrayList<Short> mac) {
		try {
			assertEquals(Constants.Souliss_UDP_function_typreq_resp, (short) mac.get(0));
			SharedPreferences.Editor editor = soulissSharedPreference.edit();
			short tgtnode = mac.get(3);
			int numberOf = mac.get(4);
			int done = 0;
			// SoulissNode node = database.getSoulissNode(tgtnode);
			int typXnodo = soulissSharedPreference.getInt("TipiciXNodo", 1);
			// creates Souliss nodes
			for (int j = 0; j < numberOf; j++) {
				if (mac.get(5 + j) != 0) {// create only not-empty typicals
					SoulissTypicalDTO dto = new SoulissTypicalDTO();
					dto.setTypical(mac.get(5 + j));
					dto.setSlot(((short) (j % typXnodo)));// magia
					dto.setNodeId((short) (j / typXnodo));
					// conta solo i master
					if (mac.get(5 + j) != it.angelic.soulissclient.model.typicals.Constants.Souliss_T_related)
						done++;
					dto.persist();
				}
			}
			if (soulissSharedPreference.contains("numTipici"))
				editor.remove("numTipici");//unused
			editor.putInt("numTipici", done);
			editor.commit();
			Log.d(Constants.TAG, "Refreshed typicals for node " + tgtnode);
		} catch (Exception uy) {
			Log.e(Constants.TAG, "decodeTypRequest ERROR", uy);
		}
	}

	/**
	 * puo giungere in seguito a state request oppure come subscription data
	 * della publish. Semantica = a typical request. Aggiorna il DB solo se il
	 * tipico esiste
	 * 
	 * @param mac
	 */
	private void decodeStateRequest(ArrayList<Short> mac) {
		try {
			List<SoulissNode> nodes = database.getAllNodes();
			short tgtnode = mac.get(3);
			int numberOf = mac.get(4);
			int typXnodo = soulissSharedPreference.getInt("TipiciXNodo", 1);
			SoulissTypicalDTO dto = new SoulissTypicalDTO();
			//refresh typicals
			for (short j = 0; j < numberOf; j++) {
				try {
					SoulissNode it = nodes.get(j / typXnodo + tgtnode);
					it.getTypical((short) (j % typXnodo));
					dto.setOutput(mac.get(5 + j));
					dto.setSlot(((short) (j % typXnodo)));
					dto.setNodeId((short) (j / typXnodo + tgtnode));
					// sufficiente una refresh
					dto.refresh();
				} catch (NotFoundException e) {
					// skipping unexistent typical");
					continue;
				}
			}
			// Log.d(Constants.TAG, "Refreshed " + refreshed +
			// " typicals STATUS for node " + tgtnode);
		} catch (IllegalStateException e) {
			Log.e(Constants.TAG, "DB connection was closed, impossible to finish");
			return;
		} catch (Exception uy) {
			Log.e(Constants.TAG, "decodeStateRequest ERROR", uy);
		}

	}

	/**
	 * Decodes a souliss nodes health request
	 * 
	 * @param macaco
	 *            packet
	 */
	private void decodeHealthRequest(ArrayList<Short> mac) {
		// Threee static bytes
		int tgtnode = mac.get(3);
		int numberOf = mac.get(4);

		ArrayList<Short> healths = new ArrayList<Short>();
		//build an array containing healths
		for (int i = 5; i < 5 + numberOf; i++) {
			healths.add(Short.valueOf(mac.get(i)));
		}

		try {
			numberOf = database.refreshNodeHealths(healths, tgtnode);
			Log.d(Constants.TAG, "Refreshed " + numberOf + " nodes' health");
		} catch (IllegalStateException e) {
			Log.e(Constants.TAG, "DB connection closed! Can't update healths");
			return;
		}

	}

	/**
	 * Should be moved. Produces Android notification
	 * 
	 * @param ctx
	 * @param desc
	 * @param longdesc
	 * @param icon
	 */
	private static void sendNotification(Context ctx, String desc, String longdesc, int icon) {

		Intent notificationIntent = new Intent(ctx, Typical4nDetail.class);
		PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, 0);
		NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

		Resources res = ctx.getResources();

		NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);

		builder.setContentIntent(contentIntent).setSmallIcon(android.R.drawable.stat_sys_warning)
				.setLargeIcon(BitmapFactory.decodeResource(res, icon)).setTicker(desc)
				.setWhen(System.currentTimeMillis()).setAutoCancel(true).setContentTitle(desc).setContentText(longdesc);
		Notification n = builder.build();
		nm.notify(664, n);
		try {
			Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
			Ringtone r = RingtoneManager.getRingtone(ctx, notification);
			r.play();
		} catch (Exception e) {
			Log.e(Constants.TAG, "Unable toplaysounds:" + e.getMessage());
		}
	}

}
