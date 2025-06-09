package org.openmrs.module.ehospitalws.constants.queries;

import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GetNextAppointmentDate {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	
	public String getNextAppointmentDate(String patientUuid) {
		return getNextOrLastAppointmentDateByUuid(patientUuid);
	}
	
	private String getNextOrLastAppointmentDateByUuid(String patientUuid) {
		if (patientUuid == null || patientUuid.trim().isEmpty()) {
			return "Invalid patient UUID";
		}
		
		if (entityManager == null) {
			throw new IllegalStateException("EntityManager is not initialized!");
		}
		
		Date now = new Date();
		
		// Query for the next upcoming scheduled appointment
		String futureQuery = "SELECT fp.start_date_time " + "FROM openmrs.patient_appointment fp "
		        + "JOIN openmrs.person p ON fp.patient_id = p.person_id " + "WHERE p.uuid = :patientUuid "
		        + "AND fp.start_date_time >= :now " + "AND fp.status = 'Scheduled' " + "ORDER BY fp.start_date_time ASC";
		
		@SuppressWarnings("unchecked")
		List<Object> futureResults = entityManager.createNativeQuery(futureQuery).setParameter("patientUuid", patientUuid)
		        .setParameter("now", now).getResultList();
		
		if (futureResults != null && !futureResults.isEmpty()) {
			Timestamp timestamp = (Timestamp) futureResults.get(0);
			return timestamp.toLocalDateTime().format(dateTimeFormatter);
		} else {
			return null;
		}
	}
	
	public List<LocalDateTime> getAppointmentsForDate(String patientUuid, LocalDate date) {
		if (patientUuid == null || patientUuid.trim().isEmpty()) {
			return null;
		}
		
		if (entityManager == null) {
			throw new IllegalStateException("EntityManager is not initialized!");
		}
		
		String query = "SELECT fp.start_date_time " + "FROM openmrs.patient_appointment fp "
		        + "JOIN openmrs.person p ON fp.patient_id = p.person_id " + "WHERE p.uuid = :patientUuid "
		        + "AND DATE(fp.start_date_time) = :appointmentDate " + "AND fp.status = 'Scheduled' "
		        + "ORDER BY fp.start_date_time ASC";
		
		@SuppressWarnings("unchecked")
		List<Timestamp> results = entityManager.createNativeQuery(query).setParameter("patientUuid", patientUuid)
		        .setParameter("appointmentDate", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).getResultList();
		
		return results.stream().map(Timestamp::toLocalDateTime).collect(Collectors.toList());
	}
}
