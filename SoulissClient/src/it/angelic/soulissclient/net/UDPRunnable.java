package it.angelic.soulissclient.net;

import it.angelic.soulissclient.R;
import it.angelic.soulissclient.SoulissClient;
import it.angelic.soulissclient.SoulissDataService;
import it.angelic.soulissclient.db.SoulissCommandDTO;
import it.angelic.soulissclient.db.SoulissDBHelper;
import it.angelic.soulissclient.db.SoulissTriggerDTO;
import it.angelic.soulissclient.helpers.SoulissPreferenceHelper;
import it.angelic.soulissclient.model.SoulissNode;
import it.angelic.soulissclient.model.SoulissTrigger;
import it.angelic.soulissclient.typicals.SoulissTypical;

import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.DatagramChannel;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

/**
 * Apre una porta sul 23000 e si mette in ascolto per le risposte.
 * 
 * Come farlo "a due teste"? Ne vale la pena?
 * 
 * @author Ale
 *
 */
public class UDPRunnable implements Runnable {

	// implements Runnable so it can be created as a new thread
	private static final String TAG = "Souliss:UDP";
	private DatagramSocket socket;
	private SoulissPreferenceHelper opzioni;
	private Context context;

	public UDPRunnable(SoulissPreferenceHelper opzioni, Context ctx) {
		super();
		this.opzioni = opzioni;
		this.context = ctx;
	}

	public void run() {
		// Souliss listens on port 23000
		Looper.prepare();
		// lifecycle
		Log.d("UDP", "***Creating server bind on port" + Constants.SERVERPORT);
		while (true) {
			try {
				//InetAddress serverAddr = InetAddress.getByName(SOULISSIP);
				DatagramChannel channel = DatagramChannel.open();
				socket = channel.socket();
				
				//socket = new DatagramSocket();
				socket.setReuseAddress(true);
				
				//port to receive souliss board data
				InetSocketAddress sa = new InetSocketAddress(Constants.SERVERPORT);
				socket.bind(sa);
				
				// create a buffer to copy packet contents into
				byte[] buf = new byte[200];
				// create a packet to receive
				final DatagramPacket packet = new DatagramPacket(buf, buf.length);
				int to = opzioni.getDataServiceIntervalMsec();
				Log.d(TAG, "***Waiting on packet, timeout="+to);
				socket.setSoTimeout(to);
				// wait to receive the packet
				socket.receive(packet);
				//spawn a decoder and go on
				new Thread(new Runnable() {
					@Override
					public void run() {
						UDPSoulissDecoder decoder = new UDPSoulissDecoder(opzioni, SoulissClient.getAppContext());
						decoder.decodevNet(packet);
					}
				}).start();
				

				socket.close();

			} catch (BindException e) {
				Log.e(TAG, "***UDP Port busy, Souliss already listening: " + e.getMessage());
				e.printStackTrace();
				try {
					Thread.sleep(opzioni.getDataServiceIntervalMsec());
					socket.close();
				} catch (Exception e1) {
					Log.e(TAG, "***UDP close failed" + e1.toString());
				}
			} catch (SocketTimeoutException e2) {
				Log.w(TAG, "***UDP SocketTimeoutException close!" + e2);
				socket.close();
			} catch (Exception ee) {
				Log.w(TAG, "***UDP unhandled!" + ee.getMessage());
				ee.printStackTrace();
			}
		}
	}


	

}