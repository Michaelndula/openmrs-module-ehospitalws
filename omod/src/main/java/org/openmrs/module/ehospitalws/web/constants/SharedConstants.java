package org.openmrs.module.ehospitalws.web.constants;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.*;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ehospitalws.web.dto.PatientObservations;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SharedConstants {
	
	public static SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy");
	
	public static final double THRESHOLD = 1000.0;
	
	public static Date[] getStartAndEndDate(String qStartDate, String qEndDate, SimpleDateFormat dateTimeFormatter)
	        throws ParseException {
		Date endDate = (qEndDate != null) ? dateTimeFormatter.parse(qEndDate) : new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(endDate);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		Date startDate = (qStartDate != null) ? dateTimeFormatter.parse(qStartDate) : calendar.getTime();
		
		return new Date[] { startDate, endDate };
	}
	
	// Retrieves a list of encounters filtered by encounter types.
	public static List<Encounter> getEncountersByEncounterTypes(List<String> encounterTypeUuids, Date startDate,
	        Date endDate) {
		List<EncounterType> encounterTypes = encounterTypeUuids.stream()
		        .map(uuid -> Context.getEncounterService().getEncounterTypeByUuid(uuid)).collect(Collectors.toList());
		
		EncounterSearchCriteria encounterCriteria = new EncounterSearchCriteria(null, null, startDate, endDate, null, null,
		        encounterTypes, null, null, null, false);
		return Context.getEncounterService().getEncounters(encounterCriteria);
	}
	
	/**
	 * Retrieves a list of concepts based on their UUIDs.
	 * 
	 * @param conceptUuids A list of UUIDs of concepts to retrieve.
	 * @return A list of concepts corresponding to the given UUIDs.
	 */
	public static List<Concept> getConceptsByUuids(List<String> conceptUuids) {
		return conceptUuids.stream().map(uuid -> Context.getConceptService().getConceptByUuid(uuid))
		        .collect(Collectors.toList());
	}
	
	public static Map<String, Object> createResultMap(String key, int value) {
		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put(key, value);
		return resultMap;
	}
	
	public static ResponseEntity<Object> buildErrorResponse(String message, HttpStatus status) {
		return new ResponseEntity<>(message, new HttpHeaders(), status);
	}
	
	public static List<Map<String, String>> getIdentifiersList(Patient patient) {
		List<Map<String, String>> identifiersList = new ArrayList<>();
		for (PatientIdentifier identifier : patient.getIdentifiers()) {
			Map<String, String> identifierObj = new HashMap<>();
			identifierObj.put("identifier", identifier.getIdentifier().trim());
			identifierObj.put("identifierType", identifier.getIdentifierType().getName().trim());
			identifiersList.add(identifierObj);
		}
		return identifiersList;
	}
	
	public static ArrayNode getPatientIdentifiersArray(Patient patient) {
		ArrayNode identifiersArray = JsonNodeFactory.instance.arrayNode();
		for (PatientIdentifier identifier : patient.getIdentifiers()) {
			ObjectNode identifierObj = JsonNodeFactory.instance.objectNode();
			identifierObj.put("identifier", identifier.getIdentifier());
			identifierObj.put("identifierType", identifier.getIdentifierType().getName());
			identifiersArray.add(identifierObj);
		}
		return identifiersArray;
	}
	
	public static String getPatientFullAddress(Patient patient) {
		String village = "";
		String landmark = "";
		for (PersonAddress address : patient.getAddresses()) {
			if (address.getAddress5() != null) {
				village = address.getAddress5();
			}
			if (address.getAddress6() != null) {
				landmark = address.getAddress6();
			}
		}
		return "Village: " + village + ", Landmark: " + landmark;
	}
	
	public static String formatBirthdate(Date birthdate) {
		return dateTimeFormatter.format(birthdate);
	}
	
	public static long calculateAge(Date birthdate) {
		Date currentDate = new Date();
		return (currentDate.getTime() - birthdate.getTime()) / (1000L * 60 * 60 * 24 * 365);
	}
	
	public static Double getPatientAge(Patient patient) {
		LocalDate birthdate = patient.getBirthdate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate currentDate = LocalDate.now();
		
		return (double) Period.between(birthdate, currentDate).getYears();
	}
	
	public static Map<String, Object> buildResponseMap(Patient patient, long age, String birthdate,
	        List<Map<String, String>> identifiersList, PatientObservations observations) {
		Map<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put("Name", patient.getPersonName() != null ? patient.getPersonName().toString() : "");
		responseMap.put("uuid", patient.getUuid());
		responseMap.put("age", age);
		responseMap.put("birthdate", birthdate);
		responseMap.put("sex", patient.getGender());
		responseMap.put("Identifiers", identifiersList);
		responseMap.put("results", Collections.singletonList(observations));
		
		return responseMap;
	}
	
	public static String getLastVisitDate(Patient patient) {
		VisitService visitService = Context.getVisitService();
		List<Visit> visits = visitService.getVisitsByPatient(patient);
		
		if (!visits.isEmpty()) {
			// get the latest visit date
			visits.sort((v1, v2) -> v2.getStartDatetime().compareTo(v1.getStartDatetime()));
			
			Date lastVisitDate = visits.get(0).getStartDatetime();
			
			if (lastVisitDate != null) {
				return dateTimeFormatter.format(lastVisitDate);
			}
		}
		return "";
	}
	
	public enum Flags {
		LLM_CONSENT_YES,
		LLM_CONSENT_NO,
		PATIENT_TYPE_SHA,
		PATIENT_TYPE_STANDARD
	}
	
	/**
	 * Calculates the age of a patient based on their birthdate.
	 */
	public static Period calculatePatientAge(Patient patient) {
		LocalDate birthDate = patient.getBirthdate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
		return Period.between(birthDate, LocalDate.now());
	}
	
	/**
	 * Formats the patient's age into a human-readable string.
	 */
	public static String formatAge(Period age) {
		int years = age.getYears();
		int months = age.getMonths();
		
		if (years >= 1) {
			return years + " year" + (years > 1 ? "s" : "");
		}
		if (months >= 1) {
			return months + " month" + (months > 1 ? "s" : "");
		}
		
		long weeks = age.getDays() / 7;
		return weeks + " week" + (weeks > 1 ? "s" : "");
	}
	
	/**
	 * Fetches the last encounter for a given patient and encounter type.
	 * 
	 * @param patient The patient
	 * @param encounterType The encounter type
	 * @return The most recent encounter of the given type for the patient, or null if none exist
	 */
	public static Encounter getLastEncounterForType(Patient patient, EncounterType encounterType) {
		List<Encounter> encounters = Context.getEncounterService().getEncounters(patient, null, null, null, null,
		    Collections.singletonList(encounterType), null, null, null, false);
		if (!encounters.isEmpty()) {
			encounters.sort((e1, e2) -> e2.getEncounterDatetime().compareTo(e1.getEncounterDatetime()));
			return encounters.get(0);
		}
		return null;
	}
	
	/**
	 * Fetches the most recent active visit for a given patient.
	 */
	public static Visit getLatestActiveVisit(Patient patient) {
		List<Visit> activeVisits = Context.getVisitService().getActiveVisitsByPatient(patient);
		if (activeVisits.isEmpty()) {
			return null;
		}
		activeVisits.sort(Comparator.comparing(Visit::getStartDatetime).reversed());
		return activeVisits.get(0);
	}
	
	/**
	 * helper method to get a coded observation's value from the current active visit.
	 * 
	 * @param patient The patient.
	 * @param conceptUuid The UUID of the concept question.
	 * @return The coded value's name or null if not found or no active visit.
	 */
	public static String getCodedObsValueFromActiveVisit(Patient patient, String conceptUuid) {
		Visit activeVisit = getLatestActiveVisit(patient);
		
		if (activeVisit == null || activeVisit.getEncounters().isEmpty()) {
			return null;
		}
		
		List<Encounter> encountersInVisit = new ArrayList<>(activeVisit.getEncounters());
		
		List<Obs> obsList = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    encountersInVisit, Collections.singletonList(Context.getConceptService().getConceptByUuid(conceptUuid)), null,
		    null, null, null, null, null, null, null, false);
		
		if (!obsList.isEmpty()) {
			obsList.sort(Comparator.comparing(Obs::getObsDatetime).reversed());
			Obs latestObs = obsList.get(0);
			
			if (latestObs.getValueCoded() != null && latestObs.getValueCoded().getName() != null) {
				return latestObs.getValueCoded().getName().getName();
			}
		}
		
		return null;
	}
}
