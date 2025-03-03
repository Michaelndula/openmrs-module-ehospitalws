package org.openmrs.module.ehospitalws.service;

import org.openmrs.module.ehospitalws.model.LLMMessages;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

public interface LLMMessagesService {
	
	LLMMessages saveMessage(LLMMessages message);
	
	List<LLMMessages> getMessagesByPatientUuid(String patientUuid);
	
	void updateMessageStatus(Long id, String status, Timestamp sentAt, String successOrErrorMessage);
	
	LLMMessages getMessageById(Long messageId);
	
	LLMMessages getLatestMessageByPatientUuid(String patientUuid);
	
	List<LLMMessages> getAllMessages();
}
