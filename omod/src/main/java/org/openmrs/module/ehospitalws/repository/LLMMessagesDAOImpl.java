package org.openmrs.module.ehospitalws.repository;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.ehospitalws.dao.LLMMessagesDAO;
import org.openmrs.module.ehospitalws.model.LLMMessages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LLMMessagesDAOImpl implements LLMMessagesDAO {
	
	private final SessionFactory sessionFactory;
	
	@Autowired
	public LLMMessagesDAOImpl(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public LLMMessages saveMessage(LLMMessages message) {
		sessionFactory.getCurrentSession().saveOrUpdate(message);
		return message;
	}
	
	@Override
	public LLMMessages getMessageById(Long id) {
		return (LLMMessages) sessionFactory.getCurrentSession().get(LLMMessages.class, id);
	}
	
	@Override
	public List<LLMMessages> getMessagesByPatientUuid(String patientUuid) {
		return sessionFactory.getCurrentSession().createCriteria(LLMMessages.class)
		        .add(Restrictions.eq("patientUuid", patientUuid)).list();
	}
}
