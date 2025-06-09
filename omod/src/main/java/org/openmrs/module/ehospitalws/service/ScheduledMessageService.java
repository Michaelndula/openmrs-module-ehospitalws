package org.openmrs.module.ehospitalws.service;

import org.openmrs.module.ehospitalws.dao.ScheduledMessageDAO;
import org.openmrs.module.ehospitalws.model.ScheduledMessage;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduledMessageService {
	
	private final ScheduledMessageDAO scheduledMessageDAO;
	
	public ScheduledMessageService(ScheduledMessageDAO scheduledMessageDAO) {
		this.scheduledMessageDAO = scheduledMessageDAO;
	}
	
	public List<ScheduledMessage> getScheduledAndSentMessages() {
		return scheduledMessageDAO.getScheduledAndSentMessages();
	}
	
	public void saveScheduledMessage(String patientUuid, String phoneNumber, String message, Date scheduledDate) {
		ScheduledMessage scheduledMessage = new ScheduledMessage(null, patientUuid, phoneNumber, message, scheduledDate,
		        "SCHEDULED", null);
		scheduledMessageDAO.saveScheduledMessage(scheduledMessage);
	}
	
	public List<ScheduledMessage> getMessagesScheduledForTomorrow() {
		return scheduledMessageDAO.getMessagesScheduledForTomorrow();
	}
	
	public void updateMessageStatus(Long messageId, String status, Timestamp sentTimestamp) {
		scheduledMessageDAO.updateMessageStatus(messageId, status, sentTimestamp);
	}
}
