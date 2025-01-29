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

@Component
public class ScheduledAppointmentReminderTask {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	private final SmsService smsService;
	
	private final GetNextAppointmentDate getNextAppointmentDate;
	
	private final ScheduledMessageService scheduledMessageService;
	
	@Autowired
	private PatientService patientService;
	
	@Autowired
	private PersonService personService;
	
	private static final ZoneId LOCAL_TIMEZONE = ZoneId.of("Africa/Nairobi");
	
	public ScheduledAppointmentReminderTask(SmsService smsService, GetNextAppointmentDate getNextAppointmentDate,
	    ScheduledMessageService scheduledMessageService) {
		this.smsService = smsService;
		this.getNextAppointmentDate = getNextAppointmentDate;
		this.scheduledMessageService = scheduledMessageService;
	}
	
	// Run every day at 5 PM EAT (14:00 UTC)
	@Scheduled(cron = "0 0 14 * * ?")
	public void sendAppointmentReminders() {
		Context.openSession();
		try {
			String adminUsername = OpenMRSPropertiesUtil.getProperty("admin.username", "*****");
			String adminPassword = OpenMRSPropertiesUtil.getProperty("admin.password", "*****");
			
			Context.authenticate(adminUsername, adminPassword);
			
			// ✅ Proceed with appointment reminders logic
			System.out.println("Authenticated as: " + adminUsername);
			
			LocalDate tomorrow = LocalDate.now().plusDays(1);
			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			
			// Get all patients
			List<Patient> allPatients = patientService.getAllPatients();
			
			for (Patient patient : allPatients) {
				String patientUuid = patient.getUuid();
				
				// Get the next appointment date
				String appointmentDateTime = getNextAppointmentDate.getNextAppointmentDate(patientUuid);
				
				// Skip if no upcoming appointment
				if (appointmentDateTime == null) {
					continue;
				}
				
				// Extract date and time from appointmentDateTime
				String[] dateTimeParts = appointmentDateTime.split(" ");
				String appointmentDate = dateTimeParts[0];
				String appointmentTimeStr = dateTimeParts[1];
				
				// Check if appointment is exactly tomorrow
				if (!appointmentDate.equals(tomorrow.format(dateFormatter))) {
					continue;
				}
				
				// Convert appointment time to local timezone
				LocalTime appointmentTime = LocalTime.parse(appointmentTimeStr);
				ZonedDateTime utcDateTime = ZonedDateTime.of(tomorrow, appointmentTime, ZoneOffset.UTC);
				ZonedDateTime localDateTime = utcDateTime.withZoneSameInstant(LOCAL_TIMEZONE);
				
				// Format the time correctly
				String formattedTime = localDateTime.format(DateTimeFormatter.ofPattern("hh:mm a"));
				
				// Fetch patient details (First Name, Last Name, Phone Number)
				Object[] patientDetails = fetchPatientDetails(patientUuid);
				if (patientDetails == null || patientDetails[2] == null) {
					continue;
				}
				
				String firstName = (String) patientDetails[0];
				String lastName = (String) patientDetails[1];
				String phoneNumber = (String) patientDetails[2];
				
				// Determine the time of day
				String timeOfDay = getTimeOfDay();
				
				// Format the message
				String message = String.format(
				    "Good %s, %s %s, you have an upcoming appointment on %s at %s at ST. Josephs Health Center. Please be on time. Stay Healthy.",
				    timeOfDay, firstName, lastName, appointmentDate, formattedTime);
				
				// Store scheduled message in database
				scheduledMessageService.saveScheduledMessage(patientUuid, phoneNumber, message, Date.valueOf(tomorrow));
				
				// Send SMS
				boolean smsSent = smsService.sendSms(phoneNumber, message);
				
				if (smsSent) {
					System.out.println("SMS sent successfully to " + firstName + " " + lastName);
				} else {
					System.err.println("Failed to send SMS to " + firstName + " " + lastName);
				}
			}
		}
		catch (Exception e) {
			System.err.println("Error occurred while sending SMS reminders: " + e.getMessage());
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
		LocalTime now = LocalTime.now();
		if (now.isBefore(LocalTime.NOON)) {
			return "morning";
		} else if (now.isBefore(LocalTime.of(17, 0))) {
			return "afternoon";
		} else {
			return "evening";
		}
	}
}
