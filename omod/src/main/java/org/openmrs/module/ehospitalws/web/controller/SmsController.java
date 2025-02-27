package org.openmrs.module.ehospitalws.web.controller;

import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PersonService;
import org.openmrs.module.ehospitalws.model.ScheduledMessage;
import org.openmrs.module.ehospitalws.constants.queries.GetNextAppointmentDate;
import org.openmrs.module.ehospitalws.service.ScheduledMessageService;
import org.openmrs.module.ehospitalws.service.SmsService;
import org.openmrs.module.ehospitalws.task.ScheduledAppointmentReminderTask;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

import static org.openmrs.module.ehospitalws.web.constants.SharedConcepts.PHONE_NUMBER_UUID;

@RestController
@RequestMapping("/rest/" + RestConstants.VERSION_1 + "/ehospital")
public class SmsController {
	
	private final GetNextAppointmentDate getNextAppointmentDate;
	
	private final SmsService smsService;
	
	private final ScheduledAppointmentReminderTask scheduledAppointmentReminderTask;
	
	private final ScheduledMessageService scheduledMessageService;
	
	@Autowired
	private PersonService personService;
	
	public SmsController(GetNextAppointmentDate getNextAppointmentDate, SmsService smsService,
	    ScheduledAppointmentReminderTask scheduledAppointmentReminderTask, ScheduledMessageService scheduledMessageService) {
		this.getNextAppointmentDate = getNextAppointmentDate;
		this.smsService = smsService;
		this.scheduledAppointmentReminderTask = scheduledAppointmentReminderTask;
		this.scheduledMessageService = scheduledMessageService;
	}
	
	@GetMapping("/scheduled-messages")
	public ResponseEntity<List<ScheduledMessage>> getScheduledAndSentMessages() {
		List<ScheduledMessage> messages = scheduledMessageService.getScheduledAndSentMessages();
		return ResponseEntity.ok(messages);
	}
	
	@PostMapping("/smsAppointmentReminder")
	public ResponseEntity<String> smsAppointmentReminder() {
		scheduledAppointmentReminderTask.sendAppointmentReminders();
		return ResponseEntity.ok("Test SMS reminder executed.");
	}
	
	@PostMapping("/sendAppointmentReminder")
	@ResponseBody
	public ResponseEntity<String> sendAppointmentReminder(@RequestParam String patientUuid) {
		try {
			// Get the next appointment date
			String appointmentDateTime = getNextAppointmentDate.getNextAppointmentDate(patientUuid);
			
			// Restrict sending message if no appointment
			if (appointmentDateTime == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No upcoming appointment found for the patient.");
			}
			
			// Fetch patient details and phone number
			Object[] patientDetails = fetchPatientDetails(patientUuid);
			if (patientDetails == null || patientDetails[2] == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Phone number not found for the patient.");
			}
			
			String firstName = (String) patientDetails[0];
			String lastName = (String) patientDetails[1];
			String phoneNumber = (String) patientDetails[2];
			
			// Determine the time of day
			String timeOfDay = getTimeOfDay();
			
			// Extract date and time from appointmentDateTime
			String[] dateTimeParts = appointmentDateTime.split(" ");
			String date = dateTimeParts[0];
			String time = dateTimeParts[1];
			
			// Format the message
			String message = String.format(
			    "Good %s, %s %s, you have an upcoming appointment on %s at %s at ST. Josephs Health Center. Please be on time. Stay Healthy.",
			    timeOfDay, firstName, lastName, date, time);
			
			// Send SMS
			boolean smsSent = smsService.sendSms(phoneNumber, message);
			
			if (smsSent) {
				return ResponseEntity.ok("SMS reminder sent successfully!");
			} else {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send SMS.");
			}
			
		}
		catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
		}
	}
	
	private Object[] fetchPatientDetails(String patientUuid) {
		// Get the patient from the PersonService using the UUID
		Person person = personService.getPersonByUuid(patientUuid);
		
		if (person == null) {
			throw new IllegalArgumentException("Patient not found for UUID: " + patientUuid);
		}
		
		// Get the phone number from the person attributes
		PersonAttributeType phoneAttributeType = personService.getPersonAttributeTypeByUuid(PHONE_NUMBER_UUID);
		if (phoneAttributeType == null) {
			throw new IllegalArgumentException("Phone attribute type not found.");
		}
		
		PersonAttribute phoneAttribute = person.getAttribute(phoneAttributeType);
		String phoneNumber = phoneAttribute != null ? phoneAttribute.getValue() : null;
		
		// Return first name, last name, and phone number
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
