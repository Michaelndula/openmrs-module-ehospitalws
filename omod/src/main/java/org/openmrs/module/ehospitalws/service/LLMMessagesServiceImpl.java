package org.openmrs.module.ehospitalws.service;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.ehospitalws.dao.LLMMessagesDAO;
import org.openmrs.module.ehospitalws.model.LLMMessages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Service
public class LLMMessagesServiceImpl extends BaseOpenmrsService implements LLMMessagesService {
	
	@Autowired
	private LLMMessagesDAO llmMessagesDAO;
	
	@Override
	@Transactional
	public LLMMessages saveMessage(LLMMessages message) {
		return llmMessagesDAO.saveMessage(message);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<LLMMessages> getMessagesByPatientUuid(String patientUuid) {
		return llmMessagesDAO.getMessagesByPatientUuid(patientUuid);
	}
	
	@Override
	@Transactional
	public void updateMessageStatus(Long id, String status, Timestamp sentAt) {
		llmMessagesDAO.updateMessageStatus(id, status, sentAt);
	}
	
	@Override
	@Transactional(readOnly = true)
	public LLMMessages getMessageById(Long messageId) {
		return llmMessagesDAO.getMessageById(messageId);
	}
	
	@Override
	@Transactional(readOnly = true)
	public LLMMessages getLatestMessageByPatientUuid(String patientUuid) {
		return llmMessagesDAO.getLatestMessageByPatientUuid(patientUuid);
	}
}
