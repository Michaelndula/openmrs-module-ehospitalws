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
import java.math.BigDecimal;
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
		Date birthdate = patient.getBirthdate();
		Date currentDate = new Date();
		long age = (currentDate.getTime() - birthdate.getTime()) / (1000L * 60 * 60 * 24 * 365);
		
		List<String> diagnoses = getLatestVisitDiagnoses(patient);
		Double weight = getPatientWeight(patient);
		Double height = getPatientHeight(patient);
		Integer systolic_blood_pressure = getPatientSystolicPressure(patient);
		Integer diastolic_blood_pressure = getPatientDiastolicPressure(patient);
		String blood_pressure = (systolic_blood_pressure != null && diastolic_blood_pressure != null)
		        ? systolic_blood_pressure + "/" + diastolic_blood_pressure
		        : "N/A";
		Integer heart_rate = getPatientHeartRate(patient);
		Double temperature = getPatientTemperature(patient);
		
		patientObj.put("gender", patient.getGender());
		patientObj.put("age", age);
		patientObj.put("weight", weight);
		patientObj.put("height", height);
		patientObj.put("blood_pressure", blood_pressure);
		patientObj.put("heart_rate", heart_rate);
		patientObj.put("temperature", temperature);
		patientObj.put("diagnosis", diagnoses.toString());
		
		List<Order> testOrders = getPatientTestOrders(patient.getUuid());
		List<DrugOrder> medications = getPatientMedications(patient.getUuid());
		List<Condition> conditions = getPatientConditions(patient.getUuid());
		
		Map<String, ObjectNode> testMap = new HashMap<>();
		
		for (Order testOrder : testOrders) {
			if (testOrder.getConcept() != null && testOrder.getConcept().getDisplayString() != null) {
				String testName = testOrder.getConcept().getDisplayString();
				
				testMap.putIfAbsent(testName, JsonNodeFactory.instance.objectNode());
				ObjectNode testObj = testMap.get(testName);
				testObj.put("name", testName);
				
				ArrayNode testResultsArray = (ArrayNode) testObj.get("results");
				if (testResultsArray == null) {
					testResultsArray = JsonNodeFactory.instance.arrayNode();
					testObj.put("results", testResultsArray);
				}
				
				List<Obs> testObservations = getTestObservations(patient.getUuid(), testOrder.getConcept().getUuid());
				
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
		
		ArrayNode testsArray = patientObj.putArray("tests");
		for (ObjectNode testObj : testMap.values()) {
			testsArray.add(testObj);
		}
		
		ArrayNode medicationsArray = patientObj.putArray("medications");
		for (DrugOrder medOrder : medications) {
			if (medOrder.getDrug() != null) {
				medicationsArray.add(medOrder.getDrug().getName());
			} else if (medOrder.getConcept() != null && medOrder.getConcept().getName() != null) {
				medicationsArray.add(medOrder.getConcept().getName().getName());
			}
		}
		
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
		
		return patientObj;
	}
}
