package org.openmrs.module.ehospitalws.util;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatterUtil {
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/**
	 * Formats a Timestamp into a human-readable string (YYYY-MM-DD HH:mm:ss).
	 * 
	 * @param timestamp The Timestamp to format.
	 * @return A formatted date string or null if the input is null.
	 */
	public static String formatTimestamp(Timestamp timestamp) {
		return (timestamp != null) ? DATE_FORMAT.format(new Date(timestamp.getTime())) : null;
	}
}
