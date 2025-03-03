package org.openmrs.module.ehospitalws.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.ehospitalws.dao.LLMMessagesDAO;
import org.openmrs.module.ehospitalws.model.LLMMessages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import java.sql.Timestamp;
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
	
	@Override
	public void updateMessageStatus(Long id, String status, Timestamp sentAt) {
		Session session = sessionFactory.getCurrentSession();
		Query query = session.createQuery("UPDATE LLMMessages SET status = :status, sentTimestamp = :sentAt WHERE id = :id");
		query.setParameter("status", status);
		query.setParameter("sentAt", sentAt);
		query.setParameter("id", id);
		query.executeUpdate();
	}
	
	@Override
	public LLMMessages getLatestMessageByPatientUuid(String patientUuid) {
		return (LLMMessages) sessionFactory.getCurrentSession()
		        .createQuery("FROM LLMMessages WHERE patientUuid = :patientUuid ORDER BY createdTimestamp DESC")
		        .setParameter("patientUuid", patientUuid).setMaxResults(1).uniqueResult();
	}
}
