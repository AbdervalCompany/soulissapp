package it.angelic.soulissclient.helpers;

import java.util.Calendar;

import it.angelic.soulissclient.R;
import it.angelic.soulissclient.SoulissApp;

/**
 * Created by Ale on 10/10/2015.
 */
public class Utils {
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
        return getScaledTime(diffSeconds) + SoulissApp.getAppContext().getString(R.string.ago);
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
			return "" + diffDays + SoulissApp.getAppContext().getString(R.string.days);
	}

    public static String getDuration(long typicalOnDurationMsec) {
		long secondi = typicalOnDurationMsec / 1000;
		if (secondi < 60)
			return "" + secondi + " sec.";
		long diffMinutes = secondi / 60;
		secondi = secondi  % 60;//resto
		if (diffMinutes < 120)
			return "" + diffMinutes + " minuti e "+secondi+" secondi";
		long diffHours = diffMinutes / (60);
		diffMinutes = diffMinutes % 60;
			return "" + diffHours + " ore e "+diffMinutes+" minuti";
		//return null;
	}

    public static Float celsiusToFahrenheit(float in) {
        return Float.valueOf((9.0f / 5.0f) * in + 32);
    }

    public static Float fahrenheitToCelsius(float fahrenheit) {
        return Float.valueOf((5.0f / 9.0f) * (fahrenheit - 32));
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
