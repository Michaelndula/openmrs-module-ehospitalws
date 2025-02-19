package org.openmrs.module.ehospitalws.web.constants;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.Patient;
import org.openmrs.module.ehospitalws.web.controller.eHospitalWebServicesController;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GeneratePatientListObj {

    private final GeneratePatientObject generatePatientObject;

    private final GenerateSummary generateSummary;

    public GeneratePatientListObj(GeneratePatientObject generatePatientObject, GenerateSummary generateSummary) {
        this.generatePatientObject = generatePatientObject;
        this.generateSummary = generateSummary;
    }

    /**
     * Generates a summary of patient data within a specified date range, grouped by year, month, and
     * week.
     *
     * @param allPatients A set of all patients to be considered for the summary.
     * @param startDate The start date of the range for which to generate the summary.
     * @param endDate The end date of the range for which to generate the summary.
     * @param filterCategory The category to filter patients.
     * @return A JSON string representing the summary of patient data.
     */
    public Object generatePatientListObj(HashSet<Patient> allPatients, Date startDate, Date endDate,
                                         Constants.filterCategory filterCategory, ObjectNode allPatientsObj) {

        ArrayNode patientList = JsonNodeFactory.instance.arrayNode();

        List<Date> patientDates = new ArrayList<>();
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDate);

        for (Patient patient : allPatients) {
            ObjectNode patientObj = generatePatientObject.generatePatientObject(startDate, endDate, filterCategory, patient);
            if (patientObj != null) {
                patientList.add(patientObj);

                Calendar patientCal = Calendar.getInstance();
                patientCal.setTime(patient.getDateCreated());

                if (!patientCal.before(startCal) && !patientCal.after(endCal)) {
                    patientDates.add(patient.getDateCreated());
                }
            }
        }

        Map<String, Map<String, Integer>> summary = generateSummary.generateSummary(patientDates);

        ObjectNode groupingObj = JsonNodeFactory.instance.objectNode();
        ObjectNode groupYear = JsonNodeFactory.instance.objectNode();
        ObjectNode groupMonth = JsonNodeFactory.instance.objectNode();
        ObjectNode groupWeek = JsonNodeFactory.instance.objectNode();

        summary.get("groupYear").forEach(groupYear::put);
        summary.get("groupMonth").forEach(groupMonth::put);
        summary.get("groupWeek").forEach(groupWeek::put);

        groupingObj.put("groupYear", groupYear);
        groupingObj.put("groupMonth", groupMonth);
        groupingObj.put("groupWeek", groupWeek);

        allPatientsObj.put("totalPatients", allPatients.size());
        allPatientsObj.put("results", patientList);
        allPatientsObj.put("summary", groupingObj);

        return allPatientsObj.toString();
    }

    public Object generatePatientListObj(HashSet<Patient> allPatients, Date endDate) {
        return generatePatientListObj(allPatients, new Date(), endDate, null, JsonNodeFactory.instance.objectNode());
    }

    public Object generatePatientListObj(HashSet<Patient> allPatients, Date startDate, Date endDate) {
        return generatePatientListObj(allPatients, startDate, endDate, null, JsonNodeFactory.instance.objectNode());
    }

    public Object generatePatientListObj(HashSet<Patient> allPatients, Date startDate, Date endDate,
                                          Constants.filterCategory filterCategory) {
        return generatePatientListObj(allPatients, new Date());
    }

    public Object generatePatientListObj(HashSet<Patient> allPatients) {
        return generatePatientListObj(allPatients, new Date());
    }
}
