package it.angelic.soulissclient;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Constants {
	public static final String TAG = "SoulissApp";
	public static final int MAX_HEALTH = 255;
	public static final int ICON_REQUEST = 1;
	public static final float[] roundedCorners = new float[] { 5,5,5,5,5,5,5,5 };
	public static final int versionNumber = Integer.valueOf(android.os.Build.VERSION.SDK_INT);
	public static final SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm:ss");
	public static final SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy-MM-dd");

	public static DecimalFormat df = new DecimalFormat("#.######");
	//public static SimpleDateFormat yearFormat = new SimpleDateFormat("yyyyMMdd");
	public static final long POSITION_UPDATE_INTERVAL = 10 * 1000;
	public static final long POSITION_UPDATE_MIN_DIST = 25;
	public static final int GUI_UPDATE_INTERVAL = 5000;

	public static final float POSITION_AWAY_THRESHOLD = 150;
	//public static final int   CHECK_STATUS_PAUSE_MSEC = 250;
	
	//public static final long TEXT_SIZE_TITLE_OFFSET = 10;

	public static final int COMMAND_TIMED = 0;
	// Interpretati come Calendar
	public static final int COMMAND_COMEBACK_CODE = 1;
	public static final int COMMAND_GOAWAY_CODE = 2;
	public static final int COMMAND_TRIGGERED = 3;
	public static final int COMMAND_SINGLE = 4;
	public static final int COMMAND_MASSIVE = 5;

	public static final int MASSIVE_NODE_ID = -1;
	public static final int CONNECTION_NONE = -1;
	//Souliss Data Intent
	public static final String CUSTOM_INTENT = "it.angelic.soulissclient.GOT_DATA";

	/**
	 * utility minutes
	 * 
	 * @param ref
	 * @return
	 */
	public static String getTimeAgo(Calendar ref) {
		Calendar now = Calendar.getInstance();

		long milliseconds1 = ref.getTimeInMillis();
		long milliseconds2 = now.getTimeInMillis();
		long diff = milliseconds2 - milliseconds1;
		long diffSeconds = diff / 1000;
		return getScaledTime(diffSeconds) + SoulissClient.getAppContext().getString(R.string.ago);
	}
	public static String getScaledTime(long diffSeconds) {
		if (diffSeconds < 120)
			return "" + diffSeconds + " sec.";
		long diffMinutes = diffSeconds / 60;
		if (diffMinutes < 120)
			return "" + diffMinutes + " min.";
		long diffHours = diffMinutes / (60);
		if (diffHours < 72)
			return "" + diffHours + " hr";
		
		long diffDays = diffHours / (24);
			return "" + diffDays + SoulissClient.getAppContext().getString(R.string.days);
	}
	private static final String[] ROM = {"X\u0305", "V\u0305", "M", "D", "C", "L", "X", "V", "I"};
	private static final int MAX = 10000; // value of R[0], must be a power of 10

	private static final int[][] DIGITS = {
	    {},{0},{0,0},{0,0,0},{0,1},{1},
	    {1,0},{1,0,0},{1,0,0,0},{0,2}};


	/**
	 * Utility per numeri Romani
	 * @param number
	 * @return
	 */
	public static String int2roman(int number) {
	    if (number < 0 || number >= MAX*4) throw new IllegalArgumentException(
	            "int2roman: " + number + " is not between 0 and " + (MAX*4-1));
	    if (number == 0) return "O";
	    StringBuilder sb = new StringBuilder();
	    int i = 0, m = MAX;
	    while (number > 0) {
	        int[] d = DIGITS[number / m];
	        for (int n: d) sb.append(ROM[i-n]);
	        number %= m;
	        m /= 10;
	        i += 2;
	    }
	    return sb.toString();
	}


}