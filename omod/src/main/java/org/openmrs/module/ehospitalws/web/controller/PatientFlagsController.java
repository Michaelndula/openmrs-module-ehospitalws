package org.openmrs.module.ehospitalws.web.controller;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.ehospitalws.web.constants.DeterminePatientFlags;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.openmrs.module.ehospitalws.web.constants.SharedConstants.*;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/ehospital")
public class PatientFlagsController {
	
	private final DeterminePatientFlags determinePatientFlags;
	
	public PatientFlagsController(DeterminePatientFlags determinePatientFlags) {
		this.determinePatientFlags = determinePatientFlags;
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/patient/flags")
	@ResponseBody
	public ResponseEntity<Object> getPatientFlags(HttpServletRequest request,
	        @RequestParam("patientUuid") String patientUuid) throws ParseException {
		
		if (StringUtils.isBlank(patientUuid)) {
			return buildErrorResponse("You must specify patientUuid in the request!", HttpStatus.BAD_REQUEST);
		}
		
		Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
		
		if (patient == null) {
			return buildErrorResponse("The provided patient was not found in the system!", HttpStatus.NOT_FOUND);
		}
		List<Flags> flags = determinePatientFlags.determinePatientFlags(patient, null, null);
		
		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("results", flags.stream().map(Enum::name).collect(Collectors.toList()));
		
		return new ResponseEntity<>(responseMap, new HttpHeaders(), HttpStatus.OK);
	}
}
