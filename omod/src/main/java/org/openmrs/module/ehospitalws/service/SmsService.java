package org.openmrs.module.ehospitalws.service;

import org.openmrs.module.ehospitalws.util.OpenMRSPropertiesUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class SmsService {
	
	private final String smsApiUrl;
	
	private final String apiKey;
	
	private final String partnerId;
	
	private final String shortcode;
	
	public SmsService() {
		this.smsApiUrl = OpenMRSPropertiesUtil.getProperty("sms.api.url", "https://test.sms.com/api/services/sendsms/");
		this.apiKey = OpenMRSPropertiesUtil.getProperty("sms.api.key", "default-api-key");
		this.partnerId = OpenMRSPropertiesUtil.getProperty("sms.partner.id", "default-partner-id");
		this.shortcode = OpenMRSPropertiesUtil.getProperty("sms.shortcode", "default-shortcode");
	}
	
	public boolean sendSms(String phoneNumber, String message) {
		RestTemplate restTemplate = new RestTemplate();
		
		// Create the request body
		Map<String, String> requestBody = new HashMap<>();
		requestBody.put("apikey", apiKey);
		requestBody.put("partnerID", partnerId);
		requestBody.put("mobile", phoneNumber);
		requestBody.put("message", message);
		requestBody.put("shortcode", shortcode);
		requestBody.put("pass_type", "plain");
		
		try {
			// Send the POST request
			ResponseEntity<String> response = restTemplate.postForEntity(smsApiUrl, requestBody, String.class);
			return response.getStatusCode() == HttpStatus.OK;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
