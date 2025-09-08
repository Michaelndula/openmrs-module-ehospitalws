package org.openmrs.module.ehospitalws.web.constants;

import org.openmrs.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class DeterminePatientFlags {
	
	private static final Logger logger = LoggerFactory.getLogger(DeterminePatientFlags.class);
	
	public List<SharedConstants.Flags> determinePatientFlags(Patient patient, Date startDate, Date endDate) {
		List<SharedConstants.Flags> flags = new ArrayList<>();
		
		return flags;
	}
}
