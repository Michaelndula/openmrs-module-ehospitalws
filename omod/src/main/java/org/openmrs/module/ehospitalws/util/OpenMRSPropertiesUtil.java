package org.openmrs.module.ehospitalws.util;

import org.openmrs.api.context.Context;

import java.util.Properties;

public class OpenMRSPropertiesUtil {
	
	public static Properties getProperties() {
		return Context.getRuntimeProperties();
	}
	
	// Helper method to fetch and trim properties
	public static String getProperty(String key, String defaultValue) {
		Properties properties = getProperties();
		String value = properties.getProperty(key, defaultValue).trim();
		if (value.isEmpty()) {
			throw new IllegalStateException("Missing required OpenMRS property: " + key);
		}
		return value;
	}
}
