package org.openmrs.module.ehospitalws.web.constants;

import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.ehospitalws.web.controller.eHospitalWebServicesController;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static org.openmrs.module.ehospitalws.web.constants.SharedConcepts.*;

@Component
public class Constants {
	
	public static SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy");
	
	public static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
	
	public static final double THRESHOLD = 1000.0;
	
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
	
	public static String getPatientDiagnosis(Patient patient) {
		
		List<Concept> diagnosisConcepts = new ArrayList<>();
		diagnosisConcepts.add(Context.getConceptService().getConceptByUuid(IMPRESSION_DIAGNOSIS_CONCEPT_UUID));
		diagnosisConcepts.add(Context.getConceptService().getConceptByUuid(OTHER_DIAGNOSIS));
		diagnosisConcepts.add(Context.getConceptService().getConceptByUuid(OTHER_MENINGITIS));
		diagnosisConcepts.add(Context.getConceptService().getConceptByUuid(OTHER_BITES));
		diagnosisConcepts.add(Context.getConceptService().getConceptByUuid(OTHER_RESPIRATORY_DISEASE));
		diagnosisConcepts.add(Context.getConceptService().getConceptByUuid(OTHER_INJURIES));
		diagnosisConcepts.add(Context.getConceptService().getConceptByUuid(OTHER_CONVULSIVE_DISORDER));
		
		List<Obs> diagnosisObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    null, diagnosisConcepts, null, null, null, null, null, null, null, null, false);
		
		if (!diagnosisObs.isEmpty()) {
			Obs diagnosisObservation = diagnosisObs.get(0);
			if (diagnosisObservation.getValueCoded() != null) {
				return diagnosisObservation.getValueCoded().getName().getName();
			} else if (diagnosisObservation.getValueText() != null) {
				return diagnosisObservation.getValueText();
			}
		}
		
		return null;
	}
	
	public static String getDiagnosis(Date startDate, Date endDate, Patient patient) {
		
		List<Concept> diagnosisConcepts = new ArrayList<>();
		diagnosisConcepts.add(Context.getConceptService().getConceptByUuid(IMPRESSION_DIAGNOSIS_CONCEPT_UUID));
		diagnosisConcepts.add(Context.getConceptService().getConceptByUuid(OTHER_DIAGNOSIS));
		diagnosisConcepts.add(Context.getConceptService().getConceptByUuid(OTHER_MENINGITIS));
		diagnosisConcepts.add(Context.getConceptService().getConceptByUuid(OTHER_BITES));
		diagnosisConcepts.add(Context.getConceptService().getConceptByUuid(OTHER_RESPIRATORY_DISEASE));
		diagnosisConcepts.add(Context.getConceptService().getConceptByUuid(OTHER_INJURIES));
		diagnosisConcepts.add(Context.getConceptService().getConceptByUuid(OTHER_CONVULSIVE_DISORDER));
		
		List<Obs> diagnosisObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    null, diagnosisConcepts, null, null, null, null, null, null, startDate, endDate, false);
		
		if (!diagnosisObs.isEmpty()) {
			Obs diagnosisObservation = diagnosisObs.get(0);
			if (diagnosisObservation.getValueCoded() != null) {
				return diagnosisObservation.getValueCoded().getName().getName();
			} else if (diagnosisObservation.getValueText() != null) {
				return diagnosisObservation.getValueText();
			}
		}
		
		return null;
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
}
