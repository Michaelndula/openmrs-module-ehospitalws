package org.openmrs.module.ehospitalws.web.constants;

import org.openmrs.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.openmrs.module.ehospitalws.web.constants.Constants.*;
import static org.openmrs.module.ehospitalws.web.constants.SharedConstants.*;

@Component
public class DeterminePatientFlags {
	
	private static final Logger logger = LoggerFactory.getLogger(DeterminePatientFlags.class);
	
	public List<SharedConstants.Flags> determinePatientFlags(Patient patient, Date startDate, Date endDate) {
		List<SharedConstants.Flags> flags = new ArrayList<>();
		
		String llmConsent = getPatientLLMConsent(patient);
		if ("Yes".equalsIgnoreCase(llmConsent)) {
			flags.add(Flags.LLM_CONSENT_YES);
		} else if ("No".equalsIgnoreCase(llmConsent)) {
			flags.add(Flags.LLM_CONSENT_NO);
		}
		
		String patientType = getPatientType(patient);
		if ("SHA patient".equalsIgnoreCase(patientType)) {
			flags.add(Flags.PATIENT_TYPE_SHA);
		} else if ("Standard patient".equalsIgnoreCase(patientType)) {
			flags.add(Flags.PATIENT_TYPE_STANDARD);
		}
		
		logger.info("Determined flags for patient {}: {}", patient.getUuid(), flags);
		return flags;
	}
}
