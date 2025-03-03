package org.openmrs.module.ehospitalws.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Data
@Table(name = "llm_messages")
public class LLMMessages {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column(name = "patient_uuid", nullable = false)
	private String patientUuid;
	
	@Column(name = "phone_number", nullable = false)
	private String phoneNumber;
	
	@Column(name = "message", nullable = false, columnDefinition = "TEXT")
	private String message;
	
	@Column(name = "status", nullable = false)
	private String status;
	
	@Column(name = "edited", nullable = false)
	private String edited;
	
	@Column(name = "reason_edited", nullable = false, columnDefinition = "TEXT")
	private String reasonEdited;
	
	@Column(name = "regenerated", nullable = false)
	private String regenerated;
	
	@Column(name = "reason_regenerated", nullable = false, columnDefinition = "TEXT")
	private String reasonRegenerated;
	
	@Column(name = "created_at", nullable = false, updatable = false)
	@CreationTimestamp
	private Timestamp createdTimestamp;
	
	@Column(name = "sent_at", updatable = false)
	private Timestamp sentTimestamp;
	
	@Column(name = "success_or_error_message", columnDefinition = "TEXT")
	private String successOrErrorMessage;
}
