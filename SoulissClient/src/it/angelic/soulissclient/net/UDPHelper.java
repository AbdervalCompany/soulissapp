package it.angelic.soulissclient.net;

import static junit.framework.Assert.assertEquals;
import it.angelic.soulissclient.SoulissClient;
import it.angelic.soulissclient.db.SoulissCommandDTO;
import it.angelic.soulissclient.helpers.SoulissPreferenceHelper;
import it.angelic.soulissclient.model.SoulissCommand;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.util.Log;

public class UDPHelper {

	private static final String TAG = "SoulissApp:UDPHelper";

	// private String android_id =
	// Secure.getString(getContext().getContentResolver(),
	// Secure.ANDROID_ID);
	/**
	 * Costruzione frame vNet: 0D 0C 17 11 00 64 01 XX 00 00 00 01 01 0D è la
	 * lunghezza complessiva del driver vNet 0C è la lunghezza complessiva vNet
	 * 17 è la porta MaCaco su vNet (fissa)
	 * 
	 * 11 00 è l'indirizzo della scheda quindi un indirizzo IP con ultimo byte
	 * 100 va SEMPRE passato l'indirizzo privato della scheda
	 * 
	 * 64 01 è l'indirizzo dell'interfaccia utente, 01 è l'User Mode Index
	 * 
	 * @param macaco
	 *            frame input
	 * @param prefs
	 *            per ottenere l'indirizzo di souliss
	 * @return
	 */
	public static ArrayList<Byte> buildVNetFrame(List<Byte> macaco, String ipd, int useridx, int nodeidx) {

		assertEquals(true, useridx < Byte.MAX_VALUE/2);
		assertEquals(true, nodeidx < Byte.MAX_VALUE);
		
		ArrayList<Byte> frame = new ArrayList<Byte>();
		InetAddress ip;
		try {
			ip = InetAddress.getByName(ipd);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return frame;
		}
		byte[] dude = ip.getAddress();

		frame.add((byte) 23);// PUTIN

		frame.add((byte) dude[3]);// BOARD
		frame.add((byte) 0);//

		frame.add((byte) useridx);// USER
		frame.add((byte) nodeidx); // NODE INDEX

		frame.add(0, (byte) (frame.size() + macaco.size() + 1));
		frame.add(0, (byte) (frame.size() + macaco.size() + 1));// Check 2

		frame.addAll(macaco);

		Byte[] ret = new Byte[frame.size()];

		StringBuilder deb = new StringBuilder();
		for (int i = 0; i < ret.length; i++) {
			deb.append("0x" + Long.toHexString((long) frame.get(i) & 0xff) + " ");
		}
		Log.d(Constants.TAG, "vNet Frame built: " + deb.toString());

		// Send broadcast timeout
		Intent i = new Intent();
		Log.d(TAG, "Posting timeout msec. " + SoulissClient.getOpzioni().getRemoteTimeoutPref()
				* SoulissClient.getOpzioni().getBackoff());
		int it = (int) (SoulissClient.getOpzioni().getRemoteTimeoutPref() * SoulissClient.getOpzioni().getBackoff());
		i.putExtra("REQUEST_TIMEOUT_MSEC", it);
		i.setAction(Constants.CUSTOM_INTENT_SOULISS_TIMEOUT);
		SoulissClient.getOpzioni().getContx().sendBroadcast(i);

		return frame;

	}
/**
 * Builds a Macaco frame to issue a command
 * @param functional
 * @param id NodeId
 * @param slot
 * @param cmd
 * @return
 */
	private static ArrayList<Byte> buildMaCaCoForce(byte functional, String id, String slot, String... cmd) {
		assertEquals(true, functional < Byte.MAX_VALUE);
		ArrayList<Byte> frame = new ArrayList<Byte>();

		frame.add((byte) functional);// functional code

		frame.add(Byte.valueOf("0"));// PUTIN
		frame.add(Byte.valueOf("0"));

		frame.add(Byte.valueOf(id)); // STARTOFFSET
		frame.add(((byte) (Byte.valueOf(slot) + cmd.length))); // NUMBEROF

		for (int i = 0; i <= Byte.valueOf(slot); i++) {
			if (i == Byte.valueOf(slot)) {

				for (String number : cmd) {
					// che schifo
					int merdata = Integer.decode(number);
					if (merdata > Byte.MAX_VALUE)
						Log.w(Constants.TAG, "Overflow with command "+number);
					frame.add((byte) merdata);
				}

				break;// ho finito un comando su piu bytes
			} else
				frame.add(Byte.valueOf("0"));
		}

		Log.d(Constants.TAG, "MaCaCo frame built size:" + frame.size());
		return frame;

	}

	private static ArrayList<Byte> buildMaCaCoMassive(byte functional, String typical, String... cmd) {
		assertEquals(true, functional < Byte.MAX_VALUE);
		ArrayList<Byte> frame = new ArrayList<Byte>();

		frame.add(new Byte((byte) functional));// functional code

		frame.add(Byte.valueOf("0"));// PUTIN
		frame.add(Byte.valueOf("0"));

		frame.add(Byte.valueOf(typical)); // STARTOFFSET
		frame.add((byte) cmd.length); // NUMBEROF

		for (String number : cmd) {
			// che schifo
			int merdata = Integer.parseInt(number);
			if (merdata > Byte.MAX_VALUE)
				Log.w(Constants.TAG, "Overflow with command "+cmd);
			frame.add((byte) merdata);
		}

		Log.d(Constants.TAG, "MaCaCo MASSIVE frame built size:" + frame.size());
		return frame;

	}

	public static String issueSoulissCommand(SoulissCommand in, SoulissPreferenceHelper prefs) {
		SoulissCommandDTO dto = in.getCommandDTO();

		String ret = issueSoulissCommand(String.valueOf(dto.getNodeId()), String.valueOf(dto.getSlot()), prefs,
				dto.getType(), String.valueOf(dto.getCommand()));
		return ret;

	}

	public static String issueMassiveCommand(String typ, SoulissPreferenceHelper prefs,
			String... cmd){
		InetAddress serverAddr;
		DatagramSocket sender = null;
		DatagramPacket packet;
		try {
			// Log.d(TAG, "Issuing command " + cmd[0]);
			serverAddr = InetAddress.getByName(prefs.getAndSetCachedAddress());

			sender = getSenderSocket(serverAddr);
			ArrayList<Byte> buf;
				buf = buildVNetFrame(buildMaCaCoMassive(Constants.Souliss_UDP_function_force_massive, typ, cmd),
						prefs.getPrefIPAddress(), prefs.getUserIndex(),prefs.getNodeIndex());

			byte[] merd = new byte[buf.size()];
			for (int i = 0; i < buf.size(); i++) {
				merd[i] = (byte) buf.get(i);
			}
			packet = new DatagramPacket(merd, merd.length, serverAddr, Constants.SOULISSPORT);

			sender.send(packet);
			Log.d(Constants.TAG, "***Command sent to: " + serverAddr);

			return "UDP massive command OK";
		} catch (UnknownHostException ed) {
			ed.printStackTrace();
			return ed.getLocalizedMessage();
		} catch (SocketException et) {
			et.printStackTrace();
			return et.getLocalizedMessage();
		} catch (Exception e) {
			Log.d(Constants.TAG, "***Fail", e);
			return e.getLocalizedMessage();
		} finally {
			if (sender != null && !sender.isClosed())
				sender.close();
		}
	}
	/**
	 * Issue a command to Souliss, at specified coordinates
	 * 
	 * @param id
	 * @param slot
	 * @param cmd
	 * @return TODO output string
	 */
	public static String issueSoulissCommand(String id, String slot, SoulissPreferenceHelper prefs, int type,
			String... cmd) {
		InetAddress serverAddr;
		DatagramSocket sender = null;
		DatagramPacket packet;
		try {
			// Log.d(TAG, "Issuing command " + cmd[0]);
			serverAddr = InetAddress.getByName(prefs.getAndSetCachedAddress());

			sender = getSenderSocket(serverAddr);
			ArrayList<Byte> buf;
			if (type == it.angelic.soulissclient.Constants.COMMAND_MASSIVE) {
				buf = buildVNetFrame(buildMaCaCoMassive(Constants.Souliss_UDP_function_force_massive, slot, cmd),
						prefs.getPrefIPAddress(), prefs.getUserIndex(),prefs.getNodeIndex());
			} else {
				buf = buildVNetFrame(buildMaCaCoForce(Constants.Souliss_UDP_function_force, id, slot, cmd),
						prefs.getPrefIPAddress(), prefs.getUserIndex(),prefs.getNodeIndex());
			}

			byte[] merd = new byte[buf.size()];
			for (int i = 0; i < buf.size(); i++) {
				merd[i] = (byte) buf.get(i);
			}
			packet = new DatagramPacket(merd, merd.length, serverAddr, Constants.SOULISSPORT);

			sender.send(packet);
			Log.d(Constants.TAG, "***Command sent to: " + serverAddr);

			return "UDP command OK";
		} catch (UnknownHostException ed) {
			ed.printStackTrace();
			return ed.getLocalizedMessage();
		} catch (SocketException et) {
			et.printStackTrace();
			return et.getLocalizedMessage();
		} catch (Exception e) {
			Log.d(Constants.TAG, "***Fail", e);
			return e.getLocalizedMessage();
		} finally {
			if (sender != null && !sender.isClosed())
				sender.close();
		}
	}

	/**
	 * N+1 N 17 B1 00 64 01 08 00 00 00 00 ipubbl can be = to ip, if local area
	 * LAN is used
	 * 
	 * @return
	 * 
	 */
	public static InetAddress ping(String ip, String ipubbl, int userix, int nodeix) throws Exception {

		InetAddress serverAddr;
		DatagramSocket sender = null;
		DatagramPacket packet;

		try {

			serverAddr = InetAddress.getByName(ipubbl);

			DatagramChannel channel = DatagramChannel.open();
			sender = channel.socket();
			sender.setReuseAddress(true);

			// hole punch
			InetSocketAddress sa = new InetSocketAddress(Constants.SERVERPORT);
			sender.bind(sa);

			List<Byte> macaco = new ArrayList<Byte>();
			macaco = Arrays.asList(Constants.PING_PAYLOAD);
			macaco.set(1, (byte) (ip.compareTo(ipubbl) == 0 ? 0xF : 0xB));
			ArrayList<Byte> buf = UDPHelper.buildVNetFrame(macaco, ip, userix, nodeix);

			byte[] merd = new byte[buf.size()];
			for (int i = 0; i < buf.size(); i++) {
				merd[i] = (byte) buf.get(i);
			}
			packet = new DatagramPacket(merd, merd.length, serverAddr, Constants.SOULISSPORT);
			sender.send(packet);
			Log.w(Constants.TAG, "Ping sent to: " + serverAddr);
			return serverAddr;
		} finally {
			if (sender != null && !sender.isClosed())
				sender.close();
		}

	}

	/**
	 * N+1 N 17 B1 00 64 01 08 00 00 00 00 used to recreate DB
	 * 
	 */
	public static void requestDBStruct(SoulissPreferenceHelper prefs) {

		InetAddress serverAddr;
		DatagramSocket sender = null;
		DatagramPacket packet;

		try {
			serverAddr = InetAddress.getByName(prefs.getAndSetCachedAddress());
			sender = getSenderSocket(serverAddr);

			List<Byte> macaco = new ArrayList<Byte>();
			macaco = Arrays.asList(Constants.DBSTRUCT_PAYLOAD);
			ArrayList<Byte> buf = UDPHelper.buildVNetFrame(macaco, prefs.getPrefIPAddress(), prefs.getUserIndex(),prefs.getNodeIndex());

			byte[] merd = new byte[buf.size()];
			for (int i = 0; i < buf.size(); i++) {
				merd[i] = (byte) buf.get(i);
			}
			packet = new DatagramPacket(merd, merd.length, serverAddr, Constants.SOULISSPORT);
			sender.send(packet);
			Log.w(Constants.TAG, "DB struct sent. bytes:" + packet.getLength());
			return;
		} catch (UnknownHostException ed) {
			ed.printStackTrace();
			return;
		} catch (SocketException et) {
			et.printStackTrace();
			return;
		} catch (Exception e) {
			Log.d(Constants.TAG, "***requestDBStruct Fail", e);
			return;
		} finally {
			if (sender != null && !sender.isClosed())
				sender.close();
		}

	}

	private static byte[] toByteArray(ArrayList<Byte> buf) {
		byte[] merd = new byte[buf.size()];
		for (int i = 0; i < buf.size(); i++) {
			merd[i] = (byte) buf.get(i);
		}
		return merd;
	}

	/**
	 * Subscribe request.
	 * 
	 * @param prefs
	 */
	public static void stateRequest(SoulissPreferenceHelper prefs, int numberOf, int startOffset) {

		InetAddress serverAddr;
		DatagramSocket sender = null;
		DatagramPacket packet;

		try {
			serverAddr = InetAddress.getByName(prefs.getAndSetCachedAddress());
			Log.w(Constants.TAG, "Staterequest, numberof=" + numberOf);
			sender = getSenderSocket(serverAddr);

			List<Byte> macaco = new ArrayList<Byte>();
			macaco.add(Constants.Souliss_UDP_function_subscribe);
			// PUTIN, STARTOFFEST, NUMBEROF
			macaco.add((byte) 0x0);// PUTIN
			macaco.add((byte) 0x0);// PUTIN

			macaco.add((byte) startOffset);// startnode
			macaco.add((byte) numberOf);// numberof

			ArrayList<Byte> buf = UDPHelper.buildVNetFrame(macaco, prefs.getPrefIPAddress(), prefs.getUserIndex(),prefs.getNodeIndex());

			byte[] merd = toByteArray(buf);
			packet = new DatagramPacket(merd, merd.length, serverAddr, Constants.SOULISSPORT);
			sender.send(packet);
			Log.w(Constants.TAG, "Subscribe sent. bytes:" + packet.getLength());
		} catch (UnknownHostException ed) {
			ed.printStackTrace();
			return;
		} catch (SocketException et) {
			et.printStackTrace();
			return;
		} catch (Exception e) {
			Log.e(Constants.TAG, "***stateRequest Fail", e);
			return;
		} finally {
			if (sender != null && !sender.isClosed())
				sender.close();
		}

	}

	/**
	 * Subscribe request.
	 * 
	 * @param prefs
	 */
	public static void pollRequest(SoulissPreferenceHelper prefs, int numberOf, int startOffset) {

		InetAddress serverAddr;
		DatagramSocket sender = null;
		DatagramPacket packet;

		try {
			serverAddr = InetAddress.getByName(prefs.getAndSetCachedAddress());
			Log.w(TAG, "Poll request, numberof=" + numberOf);
			sender = getSenderSocket(serverAddr);

			List<Byte> macaco = new ArrayList<Byte>();
			macaco.add(Constants.Souliss_UDP_function_poll);
			// PUTIN, STARTOFFEST, NUMBEROF
			macaco.add((byte) 0x0);// PUTIN
			macaco.add((byte) 0x0);// PUTIN

			macaco.add((byte) startOffset);// startnode
			macaco.add((byte) numberOf);// numberof

			ArrayList<Byte> buf = UDPHelper.buildVNetFrame(macaco, prefs.getPrefIPAddress(), prefs.getUserIndex(),prefs.getNodeIndex());

			// pessimo
			// http://stackoverflow.com/questions/6860055/convert-arraylistbyte-into-a-byte
			byte[] merd = toByteArray(buf);
			packet = new DatagramPacket(merd, merd.length, serverAddr, Constants.SOULISSPORT);
			sender.send(packet);
		} catch (UnknownHostException ed) {
			ed.printStackTrace();
			return;
		} catch (SocketException et) {
			et.printStackTrace();
			return;
		} catch (Exception e) {
			Log.e(Constants.TAG, "***stateRequest Fail", e);
			return;
		} finally {
			if (sender != null && !sender.isClosed())
				sender.close();
		}

	}

	private static DatagramSocket getSenderSocket(InetAddress serverAddr) {
		DatagramSocket sender = null;
		try {
			DatagramChannel channel = DatagramChannel.open();
			sender = channel.socket();
			sender.setReuseAddress(true);
			// hole punch
			InetSocketAddress sa = new InetSocketAddress(Constants.SERVERPORT);
			sender.bind(sa);
		} catch (SocketException e) {
			Log.e(Constants.TAG, "SOCKETERR: "+e.getMessage());
		} catch (IOException e) {
			Log.e(Constants.TAG, "IOERR: "+e.getMessage());
		}
		return sender;
	}

	public static void typicalRequest(SoulissPreferenceHelper prefs, int numberOf, int startOffset) {

		assertEquals(true, numberOf < 128);
		InetAddress serverAddr;
		DatagramSocket sender = null;
		DatagramPacket packet;

		try {
			serverAddr = InetAddress.getByName(prefs.getAndSetCachedAddress());
			sender = getSenderSocket(serverAddr);

			List<Byte> macaco = new ArrayList<Byte>();
			// PUTIN, STARTOFFEST, NUMBEROF
			macaco.add(Constants.Souliss_UDP_function_typreq);
			macaco.add((byte) 0x0);// PUTIN
			macaco.add((byte) 0x0);// PUTIN
			macaco.add((byte) startOffset);// startnode
			macaco.add((byte) numberOf);// numberof

			ArrayList<Byte> buf = UDPHelper.buildVNetFrame(macaco, prefs.getPrefIPAddress(), prefs.getUserIndex(),prefs.getNodeIndex());

			byte[] merd = toByteArray(buf);
			packet = new DatagramPacket(merd, merd.length, serverAddr, Constants.SOULISSPORT);
			sender.send(packet);
			Log.w(Constants.TAG, "typRequest sent to " + serverAddr.getHostAddress());
		} catch (UnknownHostException ed) {
			ed.printStackTrace();
			return;
		} catch (SocketException et) {
			et.printStackTrace();
			return;
		} catch (Exception e) {
			Log.e(Constants.TAG, "typRequest Fail", e);
			return;
		} finally {
			if (sender != null && !sender.isClosed())
				sender.close();
		}
	}

	public static void healthRequest(SoulissPreferenceHelper prefs, int numberOf, int startOffset) {

		assertEquals(true, numberOf < 128);
		assertEquals(true, prefs.getPrefIPAddress() != null);
		InetAddress serverAddr;
		DatagramSocket sender = null;
		DatagramPacket packet;

		try {
			Log.d(TAG, "Staterequest, numberof=" + numberOf);
			serverAddr = InetAddress.getByName(prefs.getAndSetCachedAddress());
			sender = getSenderSocket(serverAddr);

			List<Byte> macaco = new ArrayList<Byte>();
			// PUTIN, STARTOFFEST, NUMBEROF
			macaco.add(Constants.Souliss_UDP_function_healthReq);
			macaco.add((byte) 0x0);// PUTIN
			macaco.add((byte) 0x0);// PUTIN
			macaco.add((byte) startOffset);// startnode
			macaco.add((byte) numberOf);// numberof

			ArrayList<Byte> buf = UDPHelper.buildVNetFrame(macaco, prefs.getPrefIPAddress(), prefs.getUserIndex(),prefs.getNodeIndex());

			byte[] merd = toByteArray(buf);
			packet = new DatagramPacket(merd, merd.length, serverAddr, Constants.SOULISSPORT);
			sender.send(packet);
			Log.w(Constants.TAG, "healthRequest sent to " + serverAddr.getHostAddress());
		} catch (UnknownHostException ed) {
			Log.e(Constants.TAG, "Souliss unavailable " + ed.getMessage());
			return;
		} catch (SocketException et) {
			// TODO Auto-generated catch block
			et.printStackTrace();
			return;
		} catch (Exception e) {
			Log.e(Constants.TAG, "typRequest Fail", e);
			return;
		} finally {
			if (sender != null && !sender.isClosed())
				sender.close();
		}

	}

	/**
	 * Wrappa una ping, per causare una risposta da soluiss
	 * 
	 * 
	 * 
	 * @param timeoutMsec
	 * @param prefs
	 * @param ip
	 */
	public static String checkSoulissUdp(int timeoutMsec, SoulissPreferenceHelper prefs, String ip) {

		// TODO timer che stacca la connessione in mancata risposta
		// assertEquals(true, ip.equals(prefs.getIPPreferencePublic()) ||
		// ip.equals(prefs.getPrefIPAddress()));

		// local address is alway necessary
		try {
			return ping(prefs.getPrefIPAddress(), ip, prefs.getUserIndex(), prefs.getNodeIndex()).getHostAddress();
		} catch (UnknownHostException ed) {
			ed.printStackTrace();
			return ed.getMessage();
		} catch (SocketException et) {
			et.printStackTrace();
			return et.getMessage();
		} catch (Exception e) {
			Log.e(Constants.TAG, "***Fail", e);
			return e.getMessage();
		}

	}

}