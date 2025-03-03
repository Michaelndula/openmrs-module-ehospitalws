package org.openmrs.module.ehospitalws.web.constants;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.*;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static org.openmrs.module.ehospitalws.web.constants.Orders.*;
import static org.openmrs.module.ehospitalws.web.constants.SharedConcepts.*;

@Component
public class Constants {
	
	public static SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy");
	
	public static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
	
	public static final double THRESHOLD = 1000.0;
	
	@Autowired
	private PersonService personService;
	
	public enum filterCategory {
		CHILDREN_ADOLESCENTS,
		DIAGNOSIS,
		ADULTS,
		CONSULTATION,
		DENTAL,
		ULTRASOUND,
		OPD_VISITS,
		OPD_REVISITS
	};
	
	private static final List<String> DIAGNOSIS_CONCEPT_UUIDS = Arrays.asList(IMPRESSION_DIAGNOSIS_CONCEPT_UUID,
	    OTHER_DIAGNOSIS, OTHER_MENINGITIS, OTHER_BITES, OTHER_RESPIRATORY_DISEASE, OTHER_INJURIES,
	    OTHER_CONVULSIVE_DISORDER);
	
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
	
	// Get date as String
	public static String getPatientDateByConcept(Patient patient, String conceptUuid) {
		List<Obs> conceptDateObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    null, Collections.singletonList(Context.getConceptService().getConceptByUuid(conceptUuid)), null, null, null,
		    null, 0, null, null, null, false);
		
		if (!conceptDateObs.isEmpty()) {
			Obs dateObs = conceptDateObs.get(0);
			Date conceptDate = dateObs.getValueDate();
			if (conceptDate != null) {
				return dateTimeFormatter.format(conceptDate);
			}
		}
		
		return "";
	}
	
	// Get unfiltered Date
	public static Date getDateByConcept(Patient patient, String conceptUuid) {
		List<Obs> conceptDateObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    null, Collections.singletonList(Context.getConceptService().getConceptByUuid(conceptUuid)), null, null, null,
		    null, 0, null, null, null, false);
		
		if (!conceptDateObs.isEmpty()) {
			Obs dateObs = conceptDateObs.get(0);
			return dateObs.getValueDate();
		}
		
		return null;
	}
	
	public static Double getPatientWeight(Patient patient) {
		List<Obs> weightObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()), null,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(WEIGHT_UUID)), null, null, null, null,
		    null, null, null, null, false);
		
		if (!weightObs.isEmpty()) {
			Obs weightObservation = weightObs.get(0);
			return weightObservation.getValueNumeric();
		}
		
		return null;
	}
	
	public static Double getPatientHeight(Patient patient) {
		List<Obs> heightObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()), null,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(HEIGHT_UUID)), null, null, null, null,
		    null, null, null, null, false);
		
		if (!heightObs.isEmpty()) {
			Obs heightObservation = heightObs.get(0);
			return heightObservation.getValueNumeric();
		}
		
		return null;
	}
	
	public static List<String> getLatestVisitDiagnoses(Patient patient) {
		Visit latestVisit = getLatestVisit(patient);
		return latestVisit != null ? getDiagnosesForVisit(patient, latestVisit) : Collections.emptyList();
	}
	
	public static String getDiagnosesWithinPeriod(Patient patient, Date startDate, Date endDate) {
		Visit latestVisit = getLatestVisit(patient);
		
		// If no visit found within the range, return n/a
		if (latestVisit == null || (startDate != null && latestVisit.getStartDatetime().before(startDate))
		        || (endDate != null && latestVisit.getStartDatetime().after(endDate))) {
			return "N/A";
		}
		
		List<String> diagnoses = getDiagnosesForVisit(patient, latestVisit);
		return diagnoses.isEmpty() ? "N/A" : String.join(", ", diagnoses);
	}
	
	private static List<String> getDiagnosesForVisit(Patient patient, Visit visit) {
		List<Concept> diagnosisConcepts = getDiagnosisConcepts();
		
		List<Obs> diagnosisObs = new ArrayList<>(
		        Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()), null,
		            diagnosisConcepts, null, null, null, null, null, null, null, null, false));
		
		return diagnosisObs.stream()
		        .map(obs -> obs.getValueCoded() != null ? obs.getValueCoded().getName().getName() : obs.getValueText())
		        .filter(Objects::nonNull).distinct().collect(Collectors.toList());
	}
	
	private static List<Concept> getDiagnosisConcepts() {
		return DIAGNOSIS_CONCEPT_UUIDS.stream().map(uuid -> Context.getConceptService().getConceptByUuid(uuid))
		        .collect(Collectors.toList());
	}
	
	public static Integer getPatientSystolicPressure(Patient patient) {
		List<Obs> systolicPressureObs = Context.getObsService().getObservations(
		    Collections.singletonList(patient.getPerson()), null,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(SYSTOLIC_BLOOD_PRESSURE_UUID)), null,
		    null, null, null, null, null, null, null, false);
		
		if (!systolicPressureObs.isEmpty()) {
			Obs systolicPressureObservation = systolicPressureObs.get(0);
			return systolicPressureObservation.getValueNumeric().intValue();
		}
		
		return null;
	}
	
	public static Integer getPatientDiastolicPressure(Patient patient) {
		List<Obs> diastolicPressureObs = Context.getObsService().getObservations(
		    Collections.singletonList(patient.getPerson()), null,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(DIASTOLIC_BLOOD_PRESSURE_UUID)), null,
		    null, null, null, null, null, null, null, false);
		
		if (!diastolicPressureObs.isEmpty()) {
			Obs diastolicPressureObservation = diastolicPressureObs.get(0);
			return diastolicPressureObservation.getValueNumeric().intValue();
		}
		return null;
		
	}
	
	public static Integer getPatientHeartRate(Patient patient) {
		List<Obs> heartRateObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    null, Collections.singletonList(Context.getConceptService().getConceptByUuid(PULSE_RATE_UUID)), null, null, null,
		    null, null, null, null, null, false);
		
		if (!heartRateObs.isEmpty()) {
			Obs heartRateObservation = heartRateObs.get(0);
			return heartRateObservation.getValueNumeric().intValue();
		}
		
		return null;
	}
	
	public static Double getPatientTemperature(Patient patient) {
		List<Obs> temperatureObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    null, Collections.singletonList(Context.getConceptService().getConceptByUuid(TEMPERATURE_UUID)), null, null,
		    null, null, null, null, null, null, false);
		
		if (!temperatureObs.isEmpty()) {
			Obs temperatureObservation = temperatureObs.get(0);
			return temperatureObservation.getValueNumeric();
		}
		
		return null;
	}
	
	public static Double getPatientBMI(Patient patient) {
		List<Obs> bmiObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()), null,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(BMI_UUID)), null, null, null, null, null,
		    null, null, null, false);
		
		if (!bmiObs.isEmpty()) {
			Obs bmiObservation = bmiObs.get(0);
			return bmiObservation.getValueNumeric();
		}
		
		return null;
	}
	
	public static boolean isOpdVisit(Patient patient, Date startDate, Date endDate) {
		return Context.getVisitService().getVisitsByPatient(patient).stream()
		        .anyMatch(visit -> visit.getStartDatetime().after(startDate) && visit.getStartDatetime().before(endDate)
		                && OPD_VISIT_UUID.equals(visit.getVisitType().getUuid()));
	}
	
	public static boolean isOpdRevisit(Patient patient, Date startDate, Date endDate) {
		return Context.getVisitService().getVisitsByPatient(patient).stream()
		        .anyMatch(visit -> visit.getStartDatetime().after(startDate) && visit.getStartDatetime().before(endDate)
		                && OPD_REVISIT_UUID.equals(visit.getVisitType().getUuid()));
	}
	
	public static List<Patient> getOpdPatients(Date startDate, Date endDate) {
		return Context.getVisitService().getVisits(null, null, null, null, startDate, endDate, null, null, null, true, false)
		        .stream()
		        .filter(visit -> OPD_VISIT_UUID.equals(visit.getVisitType().getUuid())
		                || OPD_REVISIT_UUID.equals(visit.getVisitType().getUuid()))
		        .map(Visit::getPatient).distinct().collect(Collectors.toList());
	}
	
	public static List<Patient> getOpdPatients(Date startDate, Date endDate, BiPredicate<Patient, DateRange> typeFilter) {
		return Context.getVisitService().getVisits(null, null, null, null, startDate, endDate, null, null, null, true, false)
		        .stream()
		        .filter(visit -> (OPD_VISIT_UUID.equals(visit.getVisitType().getUuid())
		                || OPD_REVISIT_UUID.equals(visit.getVisitType().getUuid()))
		                && typeFilter.test(visit.getPatient(), new DateRange(startDate, endDate)))
		        .map(Visit::getPatient).distinct().collect(Collectors.toList());
	}
	
	public static int countOpdVisits(Date startDate, Date endDate) {
		return (int) Context.getVisitService()
		        .getVisits(null, null, null, null, startDate, endDate, null, null, null, true, false).stream()
		        .filter(visit -> OPD_VISIT_UUID.equals(visit.getVisitType().getUuid())).count();
	}
	
	public static int countOpdRevisits(Date startDate, Date endDate) {
		return (int) Context.getVisitService()
		        .getVisits(null, null, null, null, startDate, endDate, null, null, null, true, false).stream()
		        .filter(visit -> OPD_REVISIT_UUID.equals(visit.getVisitType().getUuid())).count();
	}
	
	/**
	 * Checks if a patient has any encounter of a specific type within a given date range.
	 * 
	 * @param patient The patient to check.
	 * @param encounterTypeUuid The UUID of the encounter type to check for.
	 * @return True if the patient has an encounter of the specified type within the given date range,
	 *         false otherwise.
	 */
	public static boolean hasEncounterOfType(Patient patient, DateRange dateRange, String encounterTypeUuid) {
		EncounterType encounterType = Context.getEncounterService().getEncounterTypeByUuid(encounterTypeUuid);
		List<Encounter> encounters = Context.getEncounterService().getEncountersByPatient(patient);
		
		return encounters.stream()
		        .anyMatch(encounter -> encounter.getEncounterDatetime().after(dateRange.getStartDate())
		                && encounter.getEncounterDatetime().before(dateRange.getEndDate())
		                && encounter.getEncounterType().equals(encounterType));
	}
	
	public static boolean isDental(Patient patient, DateRange dateRange) {
		return hasEncounterOfType(patient, dateRange, DENTAL_ENCOUTERTYPE_UUID);
	}
	
	public static boolean isUltrasound(Patient patient, DateRange dateRange) {
		return hasEncounterOfType(patient, dateRange, ULTRASOUND_ENCOUNTERTYPE_UUID);
	}
	
	public static boolean isConsultation(Patient patient, DateRange dateRange) {
		return hasEncounterOfType(patient, dateRange, CONSULTATION_ENCOUNTERTYPE_UUID);
	}
	
	public static class DateRange {
		
		private final Date startDate;
		
		private final Date endDate;
		
		public DateRange(Date startDate, Date endDate) {
			this.startDate = startDate;
			this.endDate = endDate;
		}
		
		public Date getStartDate() {
			return startDate;
		}
		
		public Date getEndDate() {
			return endDate;
		}
	}
	
	public static void populateBasicDetails(Patient patient, ObjectNode patientObj) {
		Date birthdate = patient.getBirthdate();
		if (birthdate != null) {
			long age = (new Date().getTime() - birthdate.getTime()) / (1000L * 60 * 60 * 24 * 365);
			patientObj.put("age", age);
		}
		
		Optional.ofNullable(patient.getGender()).ifPresent(gender -> patientObj.put("gender", gender));
	}
	
	public static void populateVitals(Patient patient, ObjectNode patientObj) {
		Optional.ofNullable(getPatientWeight(patient)).ifPresent(weight -> patientObj.put("weight", weight));
		Optional.ofNullable(getPatientHeight(patient)).ifPresent(height -> patientObj.put("height", height));
		Optional.ofNullable(getPatientHeartRate(patient)).ifPresent(heartRate -> patientObj.put("heart_rate", heartRate));
		Optional.ofNullable(getPatientTemperature(patient)).ifPresent(temp -> patientObj.put("temperature", temp));
	}
	
	public static void populateBloodPressure(Patient patient, ObjectNode patientObj) {
		Integer systolic = getPatientSystolicPressure(patient);
		Integer diastolic = getPatientDiastolicPressure(patient);
		if (systolic != null && diastolic != null) {
			patientObj.put("blood_pressure", systolic + "/" + diastolic);
		}
	}
	
	public static void populateDiagnoses(Patient patient, ObjectNode patientObj) {
		List<String> diagnoses = getLatestVisitDiagnoses(patient);
		if (!diagnoses.isEmpty()) {
			patientObj.put("diagnosis", diagnoses.toString());
		}
	}
	
	public static void populateTests(Patient patient, ObjectNode patientObj) {
		List<Order> testOrders = getPatientTestOrders(patient.getUuid());
		Map<String, ObjectNode> testMap = new HashMap<>();
		
		for (Order testOrder : testOrders) {
			if (testOrder.getConcept() != null) {
				String testName = testOrder.getConcept().getDisplayString();
				testMap.putIfAbsent(testName, JsonNodeFactory.instance.objectNode());
				ObjectNode testObj = testMap.get(testName);
				testObj.put("name", testName);
				
				populateTestResults(patient, testOrder, testObj);
			}
		}
		
		if (!testMap.isEmpty()) {
			ArrayNode testsArray = patientObj.putArray("tests");
			testMap.values().forEach(testsArray::add);
		}
	}
	
	public static void populateTestResults(Patient patient, Order testOrder, ObjectNode testObj) {
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
	
	public static void populateMedications(Patient patient, ObjectNode patientObj) {
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
	}
	
	public static void populateConditions(Patient patient, ObjectNode patientObj) {
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
	}
	
	public String getPatientPhoneNumber(String patientUuid) {
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
	
	public String getPatientName(String patientUuid) {
		Person person = personService.getPersonByUuid(patientUuid);
		if (person != null) {
			return person.getGivenName() + " " + person.getFamilyName();
		}
		return "Unknown Patient";
	}
}
