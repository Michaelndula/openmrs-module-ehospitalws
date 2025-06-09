package org.openmrs.module.ehospitalws.task;

import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ehospitalws.constants.queries.GetNextAppointmentDate;
import org.openmrs.module.ehospitalws.service.ScheduledMessageService;
import org.openmrs.module.ehospitalws.service.SmsService;
import org.openmrs.module.ehospitalws.util.OpenMRSPropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Date;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
public class ScheduledAppointmentReminderTask {
	
	private static final Logger log = LoggerFactory.getLogger(ScheduledAppointmentReminderTask.class);
	
	@PersistenceContext
	private EntityManager entityManager;
	
	private final SmsService smsService;
	
	private final GetNextAppointmentDate getNextAppointmentDate;
	
	private final ScheduledMessageService scheduledMessageService;
	
	@Autowired
	private PatientService patientService;
	
	@Autowired
	private PersonService personService;
	
	public static final ZoneId LOCAL_TIMEZONE = ZoneId.of("Africa/Nairobi");
	
	public ScheduledAppointmentReminderTask(SmsService smsService, GetNextAppointmentDate getNextAppointmentDate,
	    ScheduledMessageService scheduledMessageService) {
		this.smsService = smsService;
		this.getNextAppointmentDate = getNextAppointmentDate;
		this.scheduledMessageService = scheduledMessageService;
	}
	
	/**
	 * Runs every day at 5 PM EAT (14:10 UTC) to schedule messages for the next day.
	 */
	@Scheduled(cron = "0 10 17 * * ?", zone = "Africa/Nairobi")
	public void sendAppointmentReminders() {
		Context.openSession();
		try {
			String adminUsername = OpenMRSPropertiesUtil.getProperty("admin.username", "*****");
			String adminPassword = OpenMRSPropertiesUtil.getProperty("admin.password", "*****");
			
			Context.authenticate(adminUsername, adminPassword);
			
			LocalDate tomorrow = LocalDate.now(LOCAL_TIMEZONE).plusDays(1);
			
			List<Patient> allPatients = patientService.getAllPatients();
			
			for (Patient patient : allPatients) {
				String patientUuid = patient.getUuid();
				
				List<LocalDateTime> appointmentsForTomorrow = getNextAppointmentDate.getAppointmentsForDate(patientUuid,
				    tomorrow);
				
				if (appointmentsForTomorrow == null || appointmentsForTomorrow.isEmpty()) {
					continue;
				}
				
				String formattedTimes = appointmentsForTomorrow.stream().map(utcDateTime -> {
					ZonedDateTime localDateTime = utcDateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(LOCAL_TIMEZONE);
					return localDateTime.format(DateTimeFormatter.ofPattern("hh:mm a"));
				}).collect(Collectors.joining(", "));
				
				Object[] patientDetails = fetchPatientDetails(patientUuid);
				if (patientDetails == null || patientDetails[2] == null) {
					continue;
				}
				
				String firstName = (String) patientDetails[0];
				String lastName = (String) patientDetails[1];
				String phoneNumber = (String) patientDetails[2];
				
				String timeOfDay = getTimeOfDay();
				String tomorrowDateFormatted = tomorrow.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
				
				String message = String.format(
				    "Good %s, %s %s, this is a reminder of your appointment(s) on %s at the following time(s): %s. "
				            + "Location: ST. Josephs Health Center. Please be on time. Stay Healthy.",
				    timeOfDay, firstName, lastName, tomorrowDateFormatted, formattedTimes);
				
				scheduledMessageService.saveScheduledMessage(patientUuid, phoneNumber, message, Date.valueOf(tomorrow));
				log.info("Scheduled reminder for patient {} {}", firstName, lastName);
			}
		}
		catch (Exception e) {
			log.error("Error occurred while scheduling SMS reminders", e);
			e.printStackTrace();
		}
		finally {
			Context.closeSession();
		}
	}
	
	private Object[] fetchPatientDetails(String patientUuid) {
		Person person = personService.getPersonByUuid(patientUuid);
		if (person == null) {
			return null;
		}
		
		// Get the phone number from the person attributes
		PersonAttributeType phoneAttributeType = personService
		        .getPersonAttributeTypeByUuid("14d4f066-15f5-102d-96e4-000c29c2a5d7");
		
		if (phoneAttributeType == null) {
			return null;
		}
		
		PersonAttribute phoneAttribute = person.getAttribute(phoneAttributeType);
		String phoneNumber = phoneAttribute != null ? phoneAttribute.getValue() : null;
		
		return new Object[] { person.getGivenName(), person.getFamilyName(), phoneNumber };
	}
	
	private String getTimeOfDay() {
		LocalTime now = LocalTime.now(LOCAL_TIMEZONE);
		if (now.isBefore(LocalTime.NOON)) {
			return "morning";
		} else if (now.isBefore(LocalTime.of(17, 0))) {
			return "afternoon";
		} else {
			return "evening";
		}
	}
}
