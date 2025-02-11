package org.openmrs.module.ehospitalws.web.controller;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.Patient;
import org.openmrs.PersonAddress;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.*;

import static org.openmrs.module.ehospitalws.web.controller.eHospitalWebServicesController.getPatientDiagnosis;

/**
 * This class configured as controller using annotation and mapped with the URL of
 * 'module/${rootArtifactid}/${rootArtifactid}Link.form'.
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/ehospital")
public class LLMController {

    @RequestMapping(method = RequestMethod.GET, value = "/patient/encounter")
    @ResponseBody
    public Object getAllPatients(HttpServletRequest request, @RequestParam("patientUuid") String patientUuid) throws ParseException {

    }

    private static ObjectNode generatePatientObject(Date startDate, Date endDate, eHospitalWebServicesController.filterCategory filterCategory,
                                                    Patient patient) {
        ObjectNode patientObj = JsonNodeFactory.instance.objectNode();
        Date birthdate = patient.getBirthdate();
        Date currentDate = new Date();
        long age = (currentDate.getTime() - birthdate.getTime()) / (1000L * 60 * 60 * 24 * 365);


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

        String diagnosis = getPatientDiagnosis(patient, startDate, endDate);

        patientObj.put("sex", patient.getGender());
        patientObj.put("age", age);
        patientObj.put("diagnosis", diagnosis);

        return patientObj;
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
                                         eHospitalWebServicesController.filterCategory filterCategory, ObjectNode allPatientsObj) {

        ArrayNode patientList = JsonNodeFactory.instance.arrayNode();

        List<Date> patientDates = new ArrayList<>();
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDate);

        for (Patient patient : allPatients) {
            ObjectNode patientObj = generatePatientObject(startDate, endDate, filterCategory, patient);
            if (patientObj != null) {
                patientList.add(patientObj);

                Calendar patientCal = Calendar.getInstance();
                patientCal.setTime(patient.getDateCreated());

                if (!patientCal.before(startCal) && !patientCal.after(endCal)) {
                    patientDates.add(patient.getDateCreated());
                }
            }
        }

        ObjectNode groupingObj = JsonNodeFactory.instance.objectNode();
        allPatientsObj.put("summary", groupingObj);

        return allPatientsObj.toString();
    }
}
