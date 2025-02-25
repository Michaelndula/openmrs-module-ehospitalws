package org.openmrs.module.ehospitalws.web.controller;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static org.openmrs.module.ehospitalws.web.constants.Constants.*;
import static org.openmrs.module.ehospitalws.web.constants.Orders.*;

/**
 * This class configured as controller using annotation and mapped with the URL of
 * 'module/${rootArtifactid}/${rootArtifactid}Link.form'.
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/ehospital")
public class LLMController {
	
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
		
		// Calculate age
		Date birthdate = patient.getBirthdate();
		if (birthdate != null) {
			long age = (new Date().getTime() - birthdate.getTime()) / (1000L * 60 * 60 * 24 * 365);
			patientObj.put("age", age);
		}
		
		// Gender
		Optional.ofNullable(patient.getGender()).ifPresent(gender -> patientObj.put("gender", gender));
		
		// Retrieve patient vitals
		Optional.ofNullable(getPatientWeight(patient)).ifPresent(weight -> patientObj.put("weight", weight));
		Optional.ofNullable(getPatientHeight(patient)).ifPresent(height -> patientObj.put("height", height));
		Optional.ofNullable(getPatientHeartRate(patient)).ifPresent(heartRate -> patientObj.put("heart_rate", heartRate));
		Optional.ofNullable(getPatientTemperature(patient)).ifPresent(temp -> patientObj.put("temperature", temp));
		
		// Blood pressure
		Integer systolic = getPatientSystolicPressure(patient);
		Integer diastolic = getPatientDiastolicPressure(patient);
		if (systolic != null && diastolic != null) {
			patientObj.put("blood_pressure", systolic + "/" + diastolic);
		}
		
		// Diagnosis (latest visit)
		List<String> diagnoses = getLatestVisitDiagnoses(patient);
		if (!diagnoses.isEmpty()) {
			patientObj.put("diagnosis", diagnoses.toString());
		}
		
		// Retrieve and populate test orders
		List<Order> testOrders = getPatientTestOrders(patient.getUuid());
		Map<String, ObjectNode> testMap = new HashMap<>();
		
		for (Order testOrder : testOrders) {
			if (testOrder.getConcept() != null) {
				String testName = testOrder.getConcept().getDisplayString();
				testMap.putIfAbsent(testName, JsonNodeFactory.instance.objectNode());
				ObjectNode testObj = testMap.get(testName);
				testObj.put("name", testName);
				
				// Retrieve test results
				List<Obs> testObservations = getTestObservations(patient.getUuid(), testOrder.getConcept().getUuid());
				ArrayNode testResultsArray = testObj.putArray("results");
				
				Set<String> addedParameters = new HashSet<>();
				for (Obs obs : testObservations) {
					String paramName = obs.getConcept().getName().getName();
					if (!addedParameters.contains(paramName)) {
						ObjectNode resultObj = JsonNodeFactory.instance.objectNode();
						resultObj.put("parameter", paramName);
						resultObj.put("value", obs.getValueAsString(Context.getLocale()));
						testResultsArray.add(resultObj);
						addedParameters.add(paramName);
					}
				}
			}
		}
		
		// Add tests to patient JSON
		if (!testMap.isEmpty()) {
			ArrayNode testsArray = patientObj.putArray("tests");
			testMap.values().forEach(testsArray::add);
		}
		
		// Retrieve and populate medications
		List<DrugOrder> medications = getPatientMedications(patient.getUuid());
		if (!medications.isEmpty()) {
			ArrayNode medicationsArray = patientObj.putArray("medications");
			for (DrugOrder medOrder : medications) {
				if (medOrder.getDrug() != null) {
					medicationsArray.add(medOrder.getDrug().getName());
				} else if (medOrder.getConcept() != null && medOrder.getConcept().getName() != null) {
					medicationsArray.add(medOrder.getConcept().getName().getName());
				}
			}
		}
		
		// Retrieve and populate conditions
		List<Condition> conditions = getPatientConditions(patient.getUuid());
		if (!conditions.isEmpty()) {
			ArrayNode conditionsArray = patientObj.putArray("conditions");
			for (Condition condition : conditions) {
				if (condition.getCondition() != null) {
					if (condition.getCondition().getCoded() != null) {
						conditionsArray.add(condition.getCondition().getCoded().getName().getName());
					} else if (condition.getCondition().getNonCoded() != null) {
						conditionsArray.add(condition.getCondition().getNonCoded());
					}
				}
			}
		}
		
		return patientObj;
	}
}
