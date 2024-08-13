/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ehospitalws.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * This class configured as controller using annotation and mapped with the URL of
 * 'module/${rootArtifactid}/${rootArtifactid}Link.form'.
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/ehospital")
public class eHospitalWebServicesController {
	
	private static final String DB_CONTAINER = "ehospital20-db-1";
	
	private static final String DB_USER = "root";
	
	private static final String DB_PASS = "openmrs";
	
	private static final String DB_NAME = "openmrs";
	
	private static final String BACKUP_DIR = "/opt";
	
	private static final String LOCAL_BACKUP_DIR = System.getProperty("user.home") + "/Data_Backup";
	
	public static final String IMPRESSION_DIAGNOSIS_CONCEPT_UUID = "759bf916-5549-4fe1-a588-a399ba04dfd5";
	
	public static final String IMNCI_DIAGNOSIS_CONCEPT_UUID = "7e0cb443-eece-40da-9acd-94888a7695b1";
	
	public static final String DIAGNOSIS_CONCEPT_UUID = "aa295620-4576-4459-93ae-00bac0de4c77";
	
	public enum filterCategory {
		CHILDREN_ADOLESCENTS,
		DIAGNOSIS,
		ADULTS
	};
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	static SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy");
	
	static SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
	
	private static final Logger logger = Logger.getLogger(eHospitalWebServicesController.class.getName());
	
	/**
	 * Gets a list of available/completed forms for a patient
	 * 
	 * @param request
	 * @param patientUuid
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/forms")
	// gets all visit forms for a patient
	@ResponseBody
	public Object getAllAvailableFormsForVisit(HttpServletRequest request, @RequestParam("patientUuid") String patientUuid) {
		if (StringUtils.isBlank(patientUuid)) {
			return new ResponseEntity<Object>("You must specify patientUuid in the request!", new HttpHeaders(),
			        HttpStatus.BAD_REQUEST);
		}
		
		Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
		
		if (patient == null) {
			return new ResponseEntity<Object>("The provided patient was not found in the system!", new HttpHeaders(),
			        HttpStatus.NOT_FOUND);
		}
		
		List<Visit> activeVisits = Context.getVisitService().getActiveVisitsByPatient(patient);
		ArrayNode formList = JsonNodeFactory.instance.arrayNode();
		ObjectNode allFormsObj = JsonNodeFactory.instance.objectNode();
		
		if (!activeVisits.isEmpty()) {
			Visit patientVisit = activeVisits.get(0);
			
			/**
			 * {uuid: string; encounterType?: EncounterType; name: string; display: string; version: string;
			 * published: boolean; retired: boolean;}
			 */
			
			/*
			 * FormManager formManager =
			 * CoreContext.getInstance().getManager(FormManager.class); List<FormDescriptor>
			 * uncompletedFormDescriptors =
			 * formManager.getAllUncompletedFormsForVisit(patientVisit);
			 *
			 * if (!uncompletedFormDescriptors.isEmpty()) {
			 *
			 * for (FormDescriptor descriptor : uncompletedFormDescriptors) {
			 * if(!descriptor.getTarget().getRetired()) { ObjectNode formObj =
			 * generateFormDescriptorPayload(descriptor); formObj.put("formCategory",
			 * "available"); formList.add(formObj); } } PatientWrapper patientWrapper = new
			 * PatientWrapper(patient); Encounter lastMchEnrollment =
			 * patientWrapper.lastEncounter(MetadataUtils.existing(EncounterType.class,
			 * MchMetadata._EncounterType.MCHMS_ENROLLMENT)); if(lastMchEnrollment != null)
			 * { ObjectNode delivery = JsonNodeFactory.instance.objectNode();
			 * delivery.put("uuid", MCH_DELIVERY_FORM_UUID); delivery.put("name",
			 * "Delivery"); delivery.put("display", "MCH Delivery Form");
			 * delivery.put("version", "1.0"); delivery.put("published", true);
			 * delivery.put("retired", false); formList.add(delivery); } CalculationResult
			 * eligibleForDischarge =
			 * EmrCalculationUtils.evaluateForPatient(EligibleForMchmsDischargeCalculation.
			 * class, null, patient); if((Boolean) eligibleForDischarge.getValue() == true)
			 * { ObjectNode discharge = JsonNodeFactory.instance.objectNode();
			 * discharge.put("uuid", MCH_DISCHARGE_FORM_UUID); discharge.put("name",
			 * "Discharge"); discharge.put("display", "MCH Discharge Form");
			 * discharge.put("version", "1.0"); discharge.put("published", true);
			 * discharge.put("retired", false); formList.add(discharge); } ObjectNode
			 * labOrder = JsonNodeFactory.instance.objectNode(); labOrder.put("uuid",
			 * LAB_ORDERS_FORM_UUID); labOrder.put("name", "Laboratory Test Orders");
			 * labOrder.put("display", "Laboratory Test Orders"); labOrder.put("version",
			 * "1.0"); labOrder.put("published", true); labOrder.put("retired", false);
			 * formList.add(labOrder); }
			 */
		}
		
		allFormsObj.put("results", formList);
		
		return allFormsObj.toString();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/allClients")
	@ResponseBody
	public Object getAllPatients(HttpServletRequest request, @RequestParam("startDate") String qStartDate,
	        @RequestParam("endDate") String qEndDate,
	        @RequestParam(required = false, value = "filter") filterCategory filterCategory,
	        @RequestParam(value = "page", defaultValue = "0") int page,
	        @RequestParam(value = "size", defaultValue = "50") int size) throws ParseException {
		Date startDate = dateTimeFormatter.parse(qStartDate);
		Date endDate = dateTimeFormatter.parse(qEndDate);
		
		List<Patient> allPatients = Context.getPatientService().getAllPatients(false);
		
		// Filter patients by creation date
		List<Patient> filteredPatients = allPatients.stream()
		        .filter(patient -> patient.getDateCreated().after(startDate) && patient.getDateCreated().before(endDate))
		        .collect(Collectors.toList());
		
		// Ensure pagination indexes are within range
		int startIndex = Math.max(0, page * size);
		int endIndex = Math.min(startIndex + size, filteredPatients.size());
		
		// Paginate the filtered list
		List<Patient> paginatedPatients = filteredPatients.subList(startIndex, endIndex);
		
		return generatePatientListObj(new HashSet<>(paginatedPatients), startDate, endDate, filterCategory);
	}
	
	private Object generatePatientListObj(HashSet<Patient> allPatients, Date startDate, Date endDate,
	        filterCategory filterCategory) {
		return generatePatientListObj(allPatients, new Date());
	}
	
	private Object generatePatientListObj(HashSet<Patient> allPatients) {
		return generatePatientListObj(allPatients, new Date());
	}
	
	public Object generatePatientListObj(HashSet<Patient> allPatients, Date endDate) {
		return generatePatientListObj(allPatients, new Date(), endDate);
	}
	
	public Object generatePatientListObj(HashSet<Patient> allPatients, Date startDate, Date endDate) {
		return generatePatientListObj(allPatients, startDate, endDate, null);
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
	        filterCategory filterCategory, ObjectNode allPatientsObj) {
		
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
		
		Map<String, Map<String, Integer>> summary = generateSummary(patientDates);
		
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
	
	private Map<String, Map<String, Integer>> generateSummary(List<Date> dates) {
		String[] months = new String[] { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov",
		        "Dec" };
		String[] days = new String[] { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
		
		Map<String, Integer> monthlySummary = new HashMap<>();
		Map<String, Integer> weeklySummary = new HashMap<>();
		Map<String, Integer> dailySummary = new HashMap<>();
		
		for (Date date : dates) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			
			String month = months[calendar.get(Calendar.MONTH)];
			monthlySummary.put(month, monthlySummary.getOrDefault(month, 0) + 1);
			
			int week = calendar.get(Calendar.WEEK_OF_MONTH);
			String weekOfTheMonth = String.format("%s_Week%s", month, week);
			weeklySummary.put(weekOfTheMonth, weeklySummary.getOrDefault(weekOfTheMonth, 0) + 1);
			
			int day = calendar.get(Calendar.DAY_OF_WEEK);
			String dayInWeek = String.format("%s_%s", month, days[day - 1]);
			dailySummary.put(dayInWeek, dailySummary.getOrDefault(dayInWeek, 0) + 1);
		}
		
		// Sorting the summaries
		Map<String, Integer> sortedMonthlySummary = monthlySummary.entrySet().stream()
		        .sorted(Comparator.comparingInt(e -> Arrays.asList(months).indexOf(e.getKey())))
		        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		
		Map<String, Integer> sortedWeeklySummary = weeklySummary.entrySet().stream().sorted((e1, e2) -> {
			String[] parts1 = e1.getKey().split("_Week");
			String[] parts2 = e2.getKey().split("_Week");
			int monthCompare = Arrays.asList(months).indexOf(parts1[0]) - Arrays.asList(months).indexOf(parts2[0]);
			if (monthCompare != 0) {
				return monthCompare;
			} else {
				return Integer.parseInt(parts1[1]) - Integer.parseInt(parts2[1]);
			}
		}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		
		Map<String, Integer> sortedDailySummary = dailySummary.entrySet().stream().sorted((e1, e2) -> {
			String[] parts1 = e1.getKey().split("_");
			String[] parts2 = e2.getKey().split("_");
			int monthCompare = Arrays.asList(months).indexOf(parts1[0]) - Arrays.asList(months).indexOf(parts2[0]);
			if (monthCompare != 0) {
				return monthCompare;
			} else {
				return Arrays.asList(days).indexOf(parts1[1]) - Arrays.asList(days).indexOf(parts2[1]);
			}
		}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		
		Map<String, Map<String, Integer>> summary = new HashMap<>();
		summary.put("groupYear", sortedMonthlySummary);
		summary.put("groupMonth", sortedWeeklySummary);
		summary.put("groupWeek", sortedDailySummary);
		
		return summary;
	}
	
	private static ObjectNode generatePatientObject(Date startDate, Date endDate, filterCategory filterCategory,
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
		
		String diagnosis = getPatientDiagnosis(patient, startDate, endDate);
		
		patientObj.put("uuid", patient.getUuid());
		patientObj.put("name", patient.getPersonName() != null ? patient.getPersonName().toString() : "");
		patientObj.put("sex", patient.getGender());
		patientObj.put("age", age);
		patientObj.put("identifiers", identifiersArray);
		patientObj.put("address", fullAddress);
		patientObj.put("contact", contact);
		patientObj.put("alternateContact", alternateContact);
		patientObj.put("dateRegistered", dateTimeFormatter.format(patient.getPersonDateCreated()));
		patientObj.put("timeRegistered", timeFormatter.format(patient.getPersonDateCreated()));
		patientObj.put("diagnosis", diagnosis);
		
		// check filter category and filter patients based on the category
		if (filterCategory != null) {
			switch (filterCategory) {
				case DIAGNOSIS:
					if (diagnosis != null && diagnosis.toLowerCase().contains(filterCategory.toString())) {
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
	
	@RequestMapping(method = RequestMethod.POST, value = "/backupDatabase")
	@ResponseBody
	public String backupDatabase(HttpServletRequest request) {
		String backupFileName = "SJH_DATA_BACKUP_" + System.currentTimeMillis() + ".sql";
		String backupFilePath = BACKUP_DIR + "/" + backupFileName;
		
		// Execute Docker command to create database backup
		String[] dockerCommand = { "docker", "exec", DB_CONTAINER, "mysqldump", "-u", DB_USER, "-p" + DB_PASS, DB_NAME, ">",
		        backupFilePath };
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(dockerCommand);
			Process process = processBuilder.start();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				return "Failed to create backup.";
			}
		}
		catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return "Failed to create backup: " + e.getMessage();
		}
		
		// Copy backup file from Docker container to local directory
		String[] copyCommand = { "docker", "cp", DB_CONTAINER + ":" + backupFilePath, LOCAL_BACKUP_DIR };
		try {
			ProcessBuilder copyProcessBuilder = new ProcessBuilder(copyCommand);
			Process copyProcess = copyProcessBuilder.start();
			int copyExitCode = copyProcess.waitFor();
			if (copyExitCode != 0) {
				return "Failed to copy backup file to local directory.";
			}
		}
		catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return "Failed to copy backup file: " + e.getMessage();
		}
		
		return "Backup completed: " + backupFileName + " and file copied to " + LOCAL_BACKUP_DIR + " successfully.";
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/downloadBackup")
	@ResponseBody
	public void downloadBackup(HttpServletRequest request, HttpServletResponse response) {
		File localBackupDir = new File(LOCAL_BACKUP_DIR);
		File[] files = localBackupDir.listFiles();
		if (files != null && files.length > 0) {
			// Find the latest backup file
			File latestBackup = Arrays.stream(files).filter(file -> file.getName().startsWith("SJH_DATA_BACKUP_"))
			        .max(Comparator.comparing(File::lastModified)).orElse(null);
			
			if (latestBackup != null) {
				try {
					response.setContentType("application/octet-stream");
					response.setHeader("Content-Disposition", "attachment; filename=\"" + latestBackup.getName() + "\"");
					Files.copy(Paths.get(latestBackup.getPath()), response.getOutputStream());
					response.getOutputStream().flush();
				}
				catch (IOException e) {
					e.printStackTrace();
					// Handle download error
				}
			} else {
				// Handle no backup file found
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				try {
					response.getWriter().println("No backup file found.");
				}
				catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			try {
				response.getWriter().println("No backup files available for download.");
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/deleteBackup/{fileName}")
	@ResponseBody
	public String deleteBackup(@PathVariable String fileName) {
		File fileToDelete = new File(LOCAL_BACKUP_DIR + "/" + fileName);
		if (fileToDelete.exists()) {
			try {
				Files.deleteIfExists(Paths.get(fileToDelete.getPath()));
				return "Deleted backup file: " + fileName + " successfully.";
			}
			catch (IOException e) {
				e.printStackTrace();
				return "Failed to delete backup file: " + fileName + ". Reason: " + e.getMessage();
			}
		} else {
			return "Backup file " + fileName + " does not exist.";
		}
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/outPatientClients")
	@ResponseBody
	public Object getAllOutPatientsClients(HttpServletRequest request, @RequestParam("startDate") String qStartDate,
	        @RequestParam("endDate") String qEndDate,
	        @RequestParam(required = false, value = "filter") filterCategory filterCategory) throws ParseException {

		Date startDate = dateTimeFormatter.parse(qStartDate);
		Date endDate = dateTimeFormatter.parse(qEndDate);
		
		if (startDate == null || endDate == null) {
			return ResponseEntity.badRequest().body("Start date and end date must not be null.");
		}
		
		List<Patient> allOpdPatients = Context.getPatientService().getAllPatients();
		
		List<Patient> opdPatients = new ArrayList<>();
		int totalOpdVisits = 0;
		int totalOpdRevisits = 0;
		
		for (Patient patient : allOpdPatients) {
			boolean isVisit = isOpdVisit(patient, startDate, endDate);
			boolean isRevisit = isOpdRevisit(patient, startDate, endDate);
			
			if (isVisit || isRevisit) {
				opdPatients.add(patient);
				if (isVisit) {
					totalOpdVisits++;
				}
				if (isRevisit) {
					totalOpdRevisits++;
				}
			}
		}
		
		ObjectNode allPatientsObj = JsonNodeFactory.instance.objectNode();
		allPatientsObj.put("totalOpdPatients", opdPatients.size());
		allPatientsObj.put("totalOpdVisits", totalOpdVisits);
		allPatientsObj.put("totalOpdRevisits", totalOpdRevisits);
		
		return generatePatientListObj(new HashSet<>(opdPatients), startDate, endDate, filterCategory, allPatientsObj);
	}
	
	private boolean isOpdVisit(Patient patient, Date startDate, Date endDate) {
		if (startDate == null || endDate == null) {
			return Context.getVisitService().getVisitsByPatient(patient).stream()
			        .anyMatch(visit -> "OPD Visit".equalsIgnoreCase(visit.getVisitType().getName()));
		}
		
		return Context.getVisitService().getVisitsByPatient(patient).stream()
		        .anyMatch(visit -> visit.getStartDatetime().after(startDate) && visit.getStartDatetime().before(endDate)
		                && "OPD Visit".equalsIgnoreCase(visit.getVisitType().getName()));
	}
	
	private boolean isOpdRevisit(Patient patient, Date startDate, Date endDate) {
		if (startDate == null || endDate == null) {
			return Context.getVisitService().getVisitsByPatient(patient).stream()
			        .anyMatch(visit -> "OPD Revisit".equalsIgnoreCase(visit.getVisitType().getName()));
		}
		
		return Context.getVisitService().getVisitsByPatient(patient).stream()
		        .anyMatch(visit -> visit.getStartDatetime().after(startDate) && visit.getStartDatetime().before(endDate)
		                && "OPD Revisit".equalsIgnoreCase(visit.getVisitType().getName()));
	}
	
	private static String getPatientDiagnosis(Patient patient, Date startDate, Date endDate) {
		
		List<Concept> diagnosisConcept = new ArrayList<>();
		diagnosisConcept.add(Context.getConceptService().getConceptByUuid(IMPRESSION_DIAGNOSIS_CONCEPT_UUID));
		diagnosisConcept.add(Context.getConceptService().getConceptByUuid(IMNCI_DIAGNOSIS_CONCEPT_UUID));
		diagnosisConcept.add(Context.getConceptService().getConceptByUuid(DIAGNOSIS_CONCEPT_UUID));
		
		List<Obs> obsList = Context.getObsService().getObservations(Collections.singletonList(patient), null,
		    diagnosisConcept, null, null, null, null, null, null, startDate, endDate, false);
		
		if (!obsList.isEmpty()) {
			Obs diagnosisObs = obsList.get(0);
			return diagnosisObs.getValueText();
		}
		
		return "";
	}
}
