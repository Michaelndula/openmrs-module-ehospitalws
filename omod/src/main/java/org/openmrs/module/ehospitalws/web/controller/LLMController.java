package org.openmrs.module.ehospitalws.web.controller;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.*;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ehospitalws.model.LLMMessages;
import org.openmrs.module.ehospitalws.service.LLMMessagesService;
import org.openmrs.module.ehospitalws.service.SmsService;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.openmrs.module.ehospitalws.web.constants.Constants.*;
import static org.openmrs.module.ehospitalws.web.constants.Orders.*;
import static org.openmrs.module.ehospitalws.web.constants.SharedConcepts.PHONE_NUMBER_UUID;

/**
 * This class configured as controller using annotation and mapped with the URL of
 * 'module/${rootArtifactid}/${rootArtifactid}Link.form'.
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/ehospital")
public class LLMController {
	
	private final SmsService smsService;
	
	@Autowired
	private LLMMessagesService llmMessagesService;
	
	@Autowired
	private PersonService personService;
	
	public LLMController(SmsService smsService) {
		this.smsService = smsService;
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/patient/encounter")
	@ResponseBody
	public Object getAllPatients(HttpServletRequest request, @RequestParam("patientUuid") String patientUuid)
	        throws ParseException, IOException {
		Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
		if (patient == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"Patient not found\"}");
		}
		
		ObjectNode patientData = generatePatientObject(patient);
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = mapper.writeValueAsString(patientData);
		
		return ResponseEntity.ok(jsonString);
	}
	
	private static ObjectNode generatePatientObject(Patient patient) {
		ObjectNode patientObj = JsonNodeFactory.instance.objectNode();
		
		populateBasicDetails(patient, patientObj);
		populateVitals(patient, patientObj);
		populateBloodPressure(patient, patientObj);
		populateDiagnoses(patient, patientObj);
		populateTests(patient, patientObj);
		populateMedications(patient, patientObj);
		populateConditions(patient, patientObj);
		
		return patientObj;
	}
	
	@PostMapping("/message/save")
	public ResponseEntity<String> saveLLMMessage(@RequestParam("patientUuid") String patientUuid,
	        @RequestBody LLMMessages message, HttpServletRequest request) {
		if (!Context.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		}
		
		String phoneNumber = getPatientPhoneNumber(patientUuid);
		
		if (phoneNumber == null || phoneNumber.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Phone number not found for the patient.");
		}
		
		message.setPatientUuid(patientUuid);
		message.setPhoneNumber(phoneNumber);
		message.setStatus("NOT SENT");
		
		LLMMessages savedMessage = llmMessagesService.saveMessage(message);
		return ResponseEntity.ok("Message saved successfully with ID: " + savedMessage.getId());
	}
	
	@GetMapping("/messages/patient")
	public ResponseEntity<List<LLMMessages>> getMessagesByPatient(@RequestParam("patientUuid") String patientUuid) {
		if (!Context.isAuthenticated()) {
			return ResponseEntity.status(401).body(null);
		}
		
		List<LLMMessages> messages = llmMessagesService.getMessagesByPatientUuid(patientUuid);
		return ResponseEntity.ok(messages);
	}
	
	@PostMapping("/message/send")
	public ResponseEntity<String> sendMessage(@RequestParam String patientUuid) {
		if (!Context.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		}
		
		LLMMessages message = llmMessagesService.getLatestMessageByPatientUuid(patientUuid);
		if (message == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No messages found for this patient.");
		}
		
		String phoneNumber = message.getPhoneNumber();
		String messageText = message.getMessage();
		
		if (phoneNumber == null || phoneNumber.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Phone number not available.");
		}
		
		boolean smsSent = smsService.sendSms(phoneNumber, messageText);
		
		if (smsSent) {
			llmMessagesService.updateMessageStatus(message.getId(), "SENT", new Timestamp(System.currentTimeMillis()));
			return ResponseEntity.ok("Message sent successfully.");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send message.");
		}
	}
	
	private String getPatientPhoneNumber(String patientUuid) {
		Person person = personService.getPersonByUuid(patientUuid);
		
		if (person == null) {
			return null;
		}
		
		PersonAttributeType phoneAttributeType = personService.getPersonAttributeTypeByUuid(PHONE_NUMBER_UUID);
		if (phoneAttributeType == null) {
			return null;
		}
		
		PersonAttribute phoneAttribute = person.getAttribute(phoneAttributeType);
		return (phoneAttribute != null) ? phoneAttribute.getValue() : null;
	}
}
