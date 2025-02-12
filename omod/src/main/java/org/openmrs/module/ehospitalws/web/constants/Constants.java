package org.openmrs.module.ehospitalws.web.constants;

import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.parameter.EncounterSearchCriteria;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.openmrs.module.ehospitalws.web.constants.SharedConcepts.*;

public class Constants {
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
        List<Obs> weightObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
                null, Collections.singletonList(Context.getConceptService().getConceptByUuid(WEIGHT_UUID)), null,
                null, null, null, null, null, null, null, false);

        if (!weightObs.isEmpty()) {
            Obs weightObservation = weightObs.get(0);
            return weightObservation.getValueNumeric();
        }

        return null;
    }

    public static Double getPatientHeight(Patient patient) {
        List<Obs> heightObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
                null, Collections.singletonList(Context.getConceptService().getConceptByUuid(HEIGHT_UUID)), null,
                null, null, null, null, null, null, null, false);

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
                null, diagnosisConcepts, null,
                null, null, null, null, null, null, null, false);

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

    public static Double getPatientSystolicPressure(Patient patient) {
        List<Obs> systolicPressureObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
                null, Collections.singletonList(Context.getConceptService().getConceptByUuid(SYSTOLIC_BLOOD_PRESSURE_UUID)), null,
                null, null, null, null, null, null, null, false);

        if (!systolicPressureObs.isEmpty()) {
            Obs systolicPressureObservation = systolicPressureObs.get(0);
            return systolicPressureObservation.getValueNumeric();
        }

        return null;
    }

    public static Double getPatientDiastolicPressure(Patient patient) {
        List<Obs> diastolicPressureObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
                null, Collections.singletonList(Context.getConceptService().getConceptByUuid(DIASTOLIC_BLOOD_PRESSURE_UUID)), null,
                null, null, null, null, null, null, null, false);

        if (!diastolicPressureObs.isEmpty()) {
            Obs diastolicPressureObservation = diastolicPressureObs.get(0);
            return diastolicPressureObservation.getValueNumeric();
        }
        return null;

    }

    public static Double getPatientHeartRate(Patient patient) {
        List<Obs> heartRateObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
                null, Collections.singletonList(Context.getConceptService().getConceptByUuid(PULSE_RATE_UUID)), null,
                null, null, null, null, null, null, null, false);

        if (!heartRateObs.isEmpty()) {
            Obs heartRateObservation = heartRateObs.get(0);
            return heartRateObservation.getValueNumeric();
        }

        return null;
    }

    public static Double getPatientTemperature(Patient patient) {
        List<Obs> temperatureObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
                null, Collections.singletonList(Context.getConceptService().getConceptByUuid(TEMPERATURE_UUID)), null,
                null, null, null, null, null, null, null, false);

        if (!temperatureObs.isEmpty()) {
            Obs temperatureObservation = temperatureObs.get(0);
            return temperatureObservation.getValueNumeric();
        }

        return null;
    }

    public static Double getPatientBMI(Patient patient) {
        List<Obs> bmiObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
                null, Collections.singletonList(Context.getConceptService().getConceptByUuid(BMI_UUID)), null,
                null, null, null, null, null, null, null, false);

        if (!bmiObs.isEmpty()) {
            Obs bmiObservation = bmiObs.get(0);
            return bmiObservation.getValueNumeric();
        }

        return null;
    }

}
