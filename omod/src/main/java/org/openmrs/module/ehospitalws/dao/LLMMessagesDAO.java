package org.openmrs.module.ehospitalws.dao;

import org.openmrs.module.ehospitalws.model.LLMMessages;

import java.sql.Timestamp;
import java.util.List;

public interface LLMMessagesDAO {
	
	LLMMessages saveMessage(LLMMessages message);
	
	LLMMessages getMessageById(Long id);
	
	List<LLMMessages> getMessagesByPatientUuid(String patientUuid);
	
	void updateMessageStatus(Long id, String status, Timestamp sentAt);
	
	LLMMessages getLatestMessageByPatientUuid(String patientUuid);
}
