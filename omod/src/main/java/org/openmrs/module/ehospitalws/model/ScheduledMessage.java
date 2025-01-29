package org.openmrs.module.ehospitalws.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table(name = "scheduled_messages")
public class ScheduledMessage {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "patient_uuid", nullable = false)
	private String patientUuid;
	
	@Column(name = "phone_number", nullable = false)
	private String phoneNumber;
	
	@Column(name = "message", nullable = false, columnDefinition = "TEXT")
	private String message;
	
	@Column(name = "scheduled_date", nullable = false)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date scheduledDate;
	
	@Column(name = "status", nullable = false)
	private String status;
	
	@Column(name = "sent_timestamp")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Timestamp sentTimestamp;
	
	// Constructors
	public ScheduledMessage() {
	}
	
	public ScheduledMessage(Long id, String patientUuid, String phoneNumber, String message, Date scheduledDate,
	    String status, Timestamp sentTimestamp) {
		this.id = id;
		this.patientUuid = patientUuid;
		this.phoneNumber = phoneNumber;
		this.message = message;
		this.scheduledDate = scheduledDate;
		this.status = status;
		this.sentTimestamp = sentTimestamp;
	}
	
	// Getters and Setters
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getPatientUuid() {
		return patientUuid;
	}
	
	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}
	
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public java.sql.Date getScheduledDate() {
		return (java.sql.Date) scheduledDate;
	}
	
	public void setScheduledDate(Date scheduledDate) {
		this.scheduledDate = scheduledDate;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public Timestamp getSentTimestamp() {
		return sentTimestamp;
	}
	
	public void setSentTimestamp(Timestamp sentTimestamp) {
		this.sentTimestamp = sentTimestamp;
	}
}
