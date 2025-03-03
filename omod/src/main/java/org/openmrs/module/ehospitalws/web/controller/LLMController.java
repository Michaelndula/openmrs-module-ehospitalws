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
import org.openmrs.module.ehospitalws.web.constants.Constants;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.openmrs.module.ehospitalws.web.constants.Constants.*;

/**
 * This class configured as controller using annotation and mapped with the URL of
 * 'module/${rootArtifactid}/${rootArtifactid}Link.form'.
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/ehospital")
public class LLMController {
	
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	private final SmsService smsService;
	
	private final Constants constants;
	
	@Autowired
	private LLMMessagesService llmMessagesService;
	
	@Autowired
	private PersonService personService;
	
	public LLMController(SmsService smsService, Constants constants) {
		this.smsService = smsService;
		this.constants = constants;
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
		
		String phoneNumber = constants.getPatientPhoneNumber(patientUuid);
		
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
	public ResponseEntity<List<Map<String, Object>>> getMessagesByPatient(@RequestParam("patientUuid") String patientUuid) {
		if (!Context.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
		}
		
		List<LLMMessages> messages = llmMessagesService.getMessagesByPatientUuid(patientUuid);
		List<Map<String, Object>> responseList = new ArrayList<>();
		
		for (LLMMessages message : messages) {
			Map<String, Object> messageData = new HashMap<>();
			messageData.put("id", message.getId());
			messageData.put("patientUuid", message.getPatientUuid());
			messageData.put("phoneNumber", message.getPhoneNumber());
			messageData.put("message", message.getMessage());
			messageData.put("status", message.getStatus());
			messageData.put("edited", message.getEdited());
			messageData.put("reasonEdited", message.getReasonEdited());
			messageData.put("regenerated", message.getRegenerated());
			messageData.put("reasonRegenerated", message.getReasonRegenerated());
			
			messageData.put("createdAt",
			    message.getCreatedTimestamp() != null ? dateFormat.format(new Date(message.getCreatedTimestamp().getTime()))
			            : null);
			messageData.put("sentAt",
			    message.getSentTimestamp() != null ? dateFormat.format(new Date(message.getSentTimestamp().getTime()))
			            : null);
			
			messageData.put("successOrErrorMessage", message.getSuccessOrErrorMessage());
			
			responseList.add(messageData);
		}
		
		return ResponseEntity.ok(responseList);
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
		
		boolean smsSent;
		String responseMessage = null;
		
		try {
			smsSent = smsService.sendSms(phoneNumber, messageText);
			responseMessage = smsSent ? "Message sent successfully to " + phoneNumber : "Failed to send message";
		}
		catch (Exception e) {
			smsSent = false;
			responseMessage = "Failed to send message: " + e.getMessage();
		}
		
		String status = smsSent ? "SENT" : "FAILED";
		Timestamp sentAt = smsSent ? new Timestamp(System.currentTimeMillis()) : null;
		llmMessagesService.updateMessageStatus(message.getId(), status, sentAt, responseMessage);
		
		if (smsSent) {
			return ResponseEntity.ok(responseMessage);
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseMessage);
		}
	}
	
	@GetMapping("/messages/all")
	public ResponseEntity<List<Map<String, Object>>> getAllMessages(@RequestParam(required = false) String startDate,
	        @RequestParam(required = false) String endDate) {
		if (!Context.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
		}
		
		List<LLMMessages> messages = llmMessagesService.getAllMessages();
		List<Map<String, Object>> responseList = new ArrayList<>();
		
		Timestamp startTimestamp = null;
		Timestamp endTimestamp = null;
		
		try {
			if (startDate != null && endDate != null) {
				startTimestamp = new Timestamp(inputDateFormat.parse(startDate).getTime());
				endTimestamp = new Timestamp(inputDateFormat.parse(endDate).getTime() + (24 * 60 * 60 * 1000 - 1));
			}
		}
		catch (ParseException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
			    Collections.singletonList(Collections.singletonMap("error", "Invalid date format. Use YYYY-MM-DD.")));
		}
		
		for (LLMMessages message : messages) {
			String createdAtFormatted = message.getCreatedTimestamp() != null
			        ? dateFormat.format(new Date(message.getCreatedTimestamp().getTime()))
			        : null;
			String sentAtFormatted = message.getSentTimestamp() != null
			        ? dateFormat.format(new Date(message.getSentTimestamp().getTime()))
			        : null;
			
			if (startTimestamp != null && endTimestamp != null) {
				if (message.getCreatedTimestamp() == null || message.getCreatedTimestamp().before(startTimestamp)
				        || message.getCreatedTimestamp().after(endTimestamp)) {
					continue;
				}
			}
			
			Map<String, Object> messageData = new HashMap<>();
			messageData.put("patientUuid", message.getPatientUuid());
			messageData.put("patientName", constants.getPatientName(message.getPatientUuid()));
			messageData.put("message", message.getMessage());
			messageData.put("createdAt", createdAtFormatted);
			messageData.put("sentAt", sentAtFormatted);
			messageData.put("status", message.getStatus());
			messageData.put("successOrErrorMessage", message.getSuccessOrErrorMessage());
			
			responseList.add(messageData);
		}
		
		return ResponseEntity.ok(responseList);
	}
}
