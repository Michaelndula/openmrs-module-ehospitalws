package org.openmrs.module.ehospitalws.service;

import org.openmrs.module.ehospitalws.model.LLMMessages;

import java.util.List;

public interface LLMMessagesService {
	
	LLMMessages saveMessage(LLMMessages message);
	
	List<LLMMessages> getMessagesByPatientUuid(String patientUuid);
}
