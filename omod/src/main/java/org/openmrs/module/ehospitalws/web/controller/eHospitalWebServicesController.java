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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.ehospitalws.web.constants.Constants;
import org.openmrs.module.ehospitalws.web.constants.GeneratePatientListObj;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

import static org.openmrs.module.ehospitalws.web.constants.Constants.*;
import static org.openmrs.module.ehospitalws.web.constants.SharedConcepts.*;

/**
 * This class configured as controller using annotation and mapped with the URL of
 * 'module/${rootArtifactid}/${rootArtifactid}Link.form'.
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/ehospital")
public class eHospitalWebServicesController {
	
	private final GeneratePatientListObj generatePatientListObj;
	
	public eHospitalWebServicesController(GeneratePatientListObj generatePatientListObj) {
		this.generatePatientListObj = generatePatientListObj;
	}
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	static SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy");
	
	private static final Logger logger = Logger.getLogger(eHospitalWebServicesController.class.getName());
	
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
		
		return generatePatientListObj.generatePatientListObj(new HashSet<>(paginatedPatients), startDate, endDate,
		    filterCategory);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/outPatientClients")
	@ResponseBody
	public Object getAllOutPatientsClients(HttpServletRequest request, @RequestParam("startDate") String qStartDate,
	        @RequestParam("endDate") String qEndDate,
	        @RequestParam(required = false, value = "filter") filterCategory filterCategory,
	        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) throws ParseException {
		
		Date startDate = dateTimeFormatter.parse(qStartDate);
		Date endDate = dateTimeFormatter.parse(qEndDate);
		
		if (startDate == null || endDate == null) {
			return ResponseEntity.badRequest().body("Start date and end date must not be null.");
		}
		
		List<Patient> opdPatients = getOpdPatients(startDate, endDate);
		
		int totalOpdVisits = countOpdVisits(startDate, endDate);
		int totalOpdRevisits = countOpdRevisits(startDate, endDate);
		
		int startIndex = page * size;
		int endIndex = Math.min(startIndex + size, opdPatients.size());
		
		if (startIndex > opdPatients.size()) {
			return ResponseEntity.badRequest().body("Page index out of bounds.");
		}
		
		List<Patient> paginatedPatients = opdPatients.subList(startIndex, endIndex);
		
		ObjectNode allPatientsObj = JsonNodeFactory.instance.objectNode();
		allPatientsObj.put("totalOpdVisits", totalOpdVisits);
		allPatientsObj.put("totalOpdRevisits", totalOpdRevisits);
		
		return generatePatientListObj.generatePatientListObj(new HashSet<>(paginatedPatients), startDate, endDate,
		    filterCategory, allPatientsObj);
	}
	
	@GetMapping(value = "/{type}")
	@ResponseBody
	public Object getOpd(HttpServletRequest request, @RequestParam("startDate") String qStartDate,
	        @RequestParam("endDate") String qEndDate,
	        @RequestParam(required = false, value = "filter") Optional<Constants.filterCategory> filterCategory,
	        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size,
	        @PathVariable String type) throws ParseException {
		
		BiFunction<Patient, DateRange, Boolean> encounterTypeFilter;
		
		switch (type.toLowerCase()) {
			case "consultation":
				encounterTypeFilter = Constants::isConsultation;
				break;
			case "dental":
				encounterTypeFilter = Constants::isDental;
				break;
			case "ultrasound":
				encounterTypeFilter = Constants::isUltrasound;
				break;
			default:
				throw new IllegalArgumentException("Invalid OPD type: " + type);
		}
		
		return handleOpdPatientsRequest(qStartDate, qEndDate, filterCategory.orElse(null), page, size,
		    (BiPredicate<Patient, DateRange>) encounterTypeFilter);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/opdVisits")
	@ResponseBody
	public Object getOpdVisits(HttpServletRequest request, @RequestParam("startDate") String qStartDate,
	        @RequestParam("endDate") String qEndDate,
	        @RequestParam(required = false, value = "filter") filterCategory filterCategory,
	        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) throws ParseException {
		
		Date startDate = dateTimeFormatter.parse(qStartDate);
		Date endDate = dateTimeFormatter.parse(qEndDate);
		
		if (startDate == null || endDate == null) {
			return ResponseEntity.badRequest().body("Start date and end date must not be null.");
		}
		
		// Fetch OPD visits directly based on visits within the date range
		List<Patient> opdVisitPatients = Context.getVisitService()
		        .getVisits(null, null, null, null, startDate, endDate, null, null, null, true, false).stream()
		        .filter(visit -> OPD_VISIT_UUID.equals(visit.getVisitType().getUuid())).map(Visit::getPatient).distinct()
		        .collect(Collectors.toList());
		
		int startIndex = page * size;
		int endIndex = Math.min(startIndex + size, opdVisitPatients.size());
		
		List<Patient> outpatientVisitsClients = opdVisitPatients.subList(startIndex, endIndex);
		
		ObjectNode allPatientsObj = JsonNodeFactory.instance.objectNode();
		
		return generatePatientListObj.generatePatientListObj(new HashSet<>(outpatientVisitsClients), startDate, endDate,
		    filterCategory, allPatientsObj);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/opdRevisits")
	@ResponseBody
	public Object getOpdReVisits(HttpServletRequest request, @RequestParam("startDate") String qStartDate,
	        @RequestParam("endDate") String qEndDate,
	        @RequestParam(required = false, value = "filter") filterCategory filterCategory,
	        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) throws ParseException {
		
		Date startDate = dateTimeFormatter.parse(qStartDate);
		Date endDate = dateTimeFormatter.parse(qEndDate);
		
		if (startDate == null || endDate == null) {
			return ResponseEntity.badRequest().body("Start date and end date must not be null.");
		}
		
		// Fetch OPD revisits directly based on visits within the date range
		List<Patient> opdRevisitPatients = Context.getVisitService()
		        .getVisits(null, null, null, null, startDate, endDate, null, null, null, true, false).stream()
		        .filter(visit -> OPD_REVISIT_UUID.equals(visit.getVisitType().getUuid())).map(Visit::getPatient).distinct()
		        .collect(Collectors.toList());
		
		int startIndex = page * size;
		int endIndex = Math.min(startIndex + size, opdRevisitPatients.size());
		
		List<Patient> outpatientRevisitsClients = opdRevisitPatients.subList(startIndex, endIndex);
		
		ObjectNode allPatientsObj = JsonNodeFactory.instance.objectNode();
		
		return generatePatientListObj.generatePatientListObj(new HashSet<>(outpatientRevisitsClients), startDate, endDate,
		    filterCategory, allPatientsObj);
	}
	
	private Object handleOpdPatientsRequest(String qStartDate, String qEndDate, filterCategory filterCategory, int page,
	        int size, BiPredicate<Patient, DateRange> typeFilter) throws ParseException {
		
		Date startDate = dateTimeFormatter.parse(qStartDate);
		Date endDate = dateTimeFormatter.parse(qEndDate);
		
		if (startDate == null || endDate == null) {
			return ResponseEntity.badRequest().body("Start date and end date must not be null.");
		}
		
		List<Patient> opdPatients = getOpdPatients(startDate, endDate, typeFilter);
		
		int startIndex = page * size;
		int endIndex = Math.min(startIndex + size, opdPatients.size());
		
		if (startIndex >= opdPatients.size()) {
			return ResponseEntity.badRequest().body("Page index out of bounds.");
		}
		
		List<Patient> outpatientClients = opdPatients.subList(startIndex, endIndex);
		
		ObjectNode allPatientsObj = JsonNodeFactory.instance.objectNode();
		
		return generatePatientListObj.generatePatientListObj(new HashSet<>(outpatientClients), startDate, endDate,
		    filterCategory, allPatientsObj);
	}
}
