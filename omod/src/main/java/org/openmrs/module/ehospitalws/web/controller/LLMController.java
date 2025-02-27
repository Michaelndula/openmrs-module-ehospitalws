package org.openmrs.module.ehospitalws.web.controller;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
		
		populateBasicDetails(patient, patientObj);
		populateVitals(patient, patientObj);
		populateBloodPressure(patient, patientObj);
		populateDiagnoses(patient, patientObj);
		populateTests(patient, patientObj);
		populateMedications(patient, patientObj);
		populateConditions(patient, patientObj);
		
		return patientObj;
	}
}
