package org.openmrs.module.ehospitalws.web.constants;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.module.ehospitalws.web.controller.eHospitalWebServicesController;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;

import static org.openmrs.module.ehospitalws.web.constants.Constants.*;

@Component
public class GeneratePatientObject {
	
	public static ObjectNode generatePatientObject(Date startDate, Date endDate, filterCategory filterCategory,
	        Patient patient) {
		ObjectNode patientObj = JsonNodeFactory.instance.objectNode();
		String contact = patient.getAttribute("Telephone Number") != null
		        ? String.valueOf(patient.getAttribute("Telephone Number"))
		        : "";
		String alternateContact = patient.getAttribute("Contact Number") != null
		        ? String.valueOf(patient.getAttribute("Contact Number"))
		        : "";
		// Calculate age in years based on patient's birthdate and current date
		Date birthdate = patient.getBirthdate();
		Date currentDate = new Date();
		long age = (currentDate.getTime() - birthdate.getTime()) / (1000L * 60 * 60 * 24 * 365);
		
		ArrayNode identifiersArray = JsonNodeFactory.instance.arrayNode();
		for (PatientIdentifier identifier : patient.getIdentifiers()) {
			ObjectNode identifierObj = JsonNodeFactory.instance.objectNode();
			identifierObj.put("identifier", identifier.getIdentifier());
			identifierObj.put("identifierType", identifier.getIdentifierType().getName());
			identifiersArray.add(identifierObj);
		}
		
		String county = "";
		String subCounty = "";
		String ward = "";
		for (PersonAddress address : patient.getAddresses()) {
			if (address.getCountyDistrict() != null) {
				county = address.getCountyDistrict();
			}
			if (address.getStateProvince() != null) {
				subCounty = address.getStateProvince();
			}
			if (address.getAddress4() != null) {
				ward = address.getAddress4();
			}
		}
		String fullAddress = "County: " + county + ", Sub County: " + subCounty + ", Ward: " + ward;
		
		String diagnosis = getDiagnosis(startDate, endDate, patient);
		
		patientObj.put("uuid", patient.getUuid());
		patientObj.put("name", patient.getPersonName() != null ? patient.getPersonName().toString() : "");
		patientObj.put("sex", patient.getGender());
		patientObj.put("age", age);
		patientObj.put("identifiers", identifiersArray);
		patientObj.put("address", fullAddress);
		patientObj.put("contact", contact);
		patientObj.put("alternateContact", alternateContact);
		patientObj.put("childrenAdolescents", age <= 19 ? true : false);
		patientObj.put("dateRegistered", dateTimeFormatter.format(patient.getPersonDateCreated()));
		patientObj.put("timeRegistered",
		    timeFormatter.format(patient.getPersonDateCreated().toInstant().atZone(ZoneId.systemDefault())));
		patientObj.put("diagnosis", diagnosis);
		patientObj.put("OPD Visits", isOpdVisit(patient, startDate, endDate));
		patientObj.put("OPD Revisit", isOpdRevisit(patient, startDate, endDate));
		
		// check filter category and filter patients based on the category
		if (filterCategory != null) {
			switch (filterCategory) {
				case DIAGNOSIS:
					if (diagnosis != null && diagnosis.toLowerCase().contains(filterCategory.toString())) {
						return patientObj;
					}
					break;
				case OPD_VISITS:
					if (isOpdVisit(patient, startDate, endDate)) {
						return patientObj;
					}
					break;
				case OPD_REVISITS:
					if (isOpdRevisit(patient, startDate, endDate)) {
						return patientObj;
					}
					break;
				case CHILDREN_ADOLESCENTS:
					if (age <= 19) {
						return patientObj;
					}
					break;
				case ADULTS:
					if (age > 19) {
						return patientObj;
					}
					break;
			}
		} else {
			return patientObj;
		}
		return null;
	}
}
