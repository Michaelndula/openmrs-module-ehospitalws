package org.openmrs.module.ehospitalws.task;

import org.openmrs.api.context.Context;
import org.openmrs.module.ehospitalws.model.ScheduledMessage;
import org.openmrs.module.ehospitalws.service.ScheduledMessageService;
import org.openmrs.module.ehospitalws.service.SmsService;
import org.openmrs.module.ehospitalws.util.OpenMRSPropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class SendMessageTask {
	
	private static final Logger log = LoggerFactory.getLogger(SendMessageTask.class);
	
	private final SmsService smsService;
	
	private final ScheduledMessageService scheduledMessageService;
	
	public SendMessageTask(SmsService smsService, ScheduledMessageService scheduledMessageService) {
		this.smsService = smsService;
		this.scheduledMessageService = scheduledMessageService;
	}
	
	/**
	 * Runs every day at 5 PM EAT (14:30 UTC) to send messages scheduled for the current day.
	 */
	@Scheduled(cron = "0 30 17 * * ?", zone = "Africa/Nairobi")
	public void sendScheduledMessages() {
		Context.openSession();
		try {
			String adminUsername = OpenMRSPropertiesUtil.getProperty("admin.username", "*****");
			String adminPassword = OpenMRSPropertiesUtil.getProperty("admin.password", "*****");
			Context.authenticate(adminUsername, adminPassword);
			
			log.info("SendMessageTask started.");
			
			List<ScheduledMessage> messagesToSend = scheduledMessageService.getMessagesScheduledForTomorrow();
			
			if (messagesToSend.isEmpty()) {
				log.info("No messages for tomorrow's appointments found to be sent today.");
				return;
			}
			
			log.info("Found {} message(s) to send for tomorrow's appointments.", messagesToSend.size());
			
			for (ScheduledMessage message : messagesToSend) {
				boolean smsSent = smsService.sendSms(message.getPhoneNumber(), message.getMessage());
				
				if (smsSent) {
					scheduledMessageService.updateMessageStatus(message.getId(), "SENT",
					    Timestamp.valueOf(LocalDateTime.now()));
					log.info("Successfully sent SMS to {} (Message ID: {})", message.getPhoneNumber(), message.getId());
				} else {
					log.warn("Failed to send SMS to {} (Message ID: {})", message.getPhoneNumber(), message.getId());
				}
			}
		}
		catch (Exception e) {
			log.error("A critical error occurred during the SendMessageTask", e);
			e.printStackTrace();
		}
		finally {
			Context.closeSession();
		}
	}
}
