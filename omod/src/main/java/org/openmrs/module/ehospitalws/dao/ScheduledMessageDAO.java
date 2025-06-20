package org.openmrs.module.ehospitalws.dao;

import org.openmrs.module.ehospitalws.model.ScheduledMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ScheduledMessageDAO {
	
	private final JdbcTemplate jdbcTemplate;
	
	public ScheduledMessageDAO(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	public List<ScheduledMessage> getScheduledAndSentMessages() {
		LocalDate today = LocalDate.now();
		LocalDateTime startOfToday = today.atStartOfDay();
		LocalDateTime endOfToday = startOfToday.plusDays(1).minusNanos(1);
		
		String sql = "SELECT id, patient_uuid, phone_number, message, scheduled_date, status, sent_timestamp "
		        + "FROM scheduled_messages " + "WHERE scheduled_date = ? OR (sent_timestamp BETWEEN ? AND ?)";
		
		return jdbcTemplate.query(sql, this::mapRowToScheduledMessage, Date.valueOf(today), Timestamp.valueOf(startOfToday),
		    Timestamp.valueOf(endOfToday));
	}
	
	// Save a new scheduled message
	public void saveScheduledMessage(ScheduledMessage message) {
		String sql = "INSERT INTO scheduled_messages (patient_uuid, phone_number, message, scheduled_date, status, sent_timestamp) "
		        + "VALUES (?, ?, ?, ?, ?, ?)";
		
		jdbcTemplate.update(sql, (PreparedStatement ps) -> {
			ps.setString(1, message.getPatientUuid());
			ps.setString(2, message.getPhoneNumber());
			ps.setString(3, message.getMessage());
			ps.setDate(4, message.getScheduledDate());
			ps.setString(5, message.getStatus());
			ps.setTimestamp(6, message.getSentTimestamp());
		});
	}
	
	public List<ScheduledMessage> getMessagesScheduledForTomorrow() {
		LocalDate tomorrow = LocalDate.now().plusDays(1);
		
		String sql = "SELECT id, patient_uuid, phone_number, message, scheduled_date, status, sent_timestamp "
		        + "FROM scheduled_messages " + "WHERE status = 'SCHEDULED' AND scheduled_date = ?";
		
		return jdbcTemplate.query(sql, this::mapRowToScheduledMessage, Date.valueOf(tomorrow));
	}
	
	public void updateMessageStatus(Long messageId, String status, Timestamp sentTimestamp) {
		String sql = "UPDATE scheduled_messages SET status = ?, sent_timestamp = ? WHERE id = ?";
		jdbcTemplate.update(sql, status, sentTimestamp, messageId);
	}
	
	private ScheduledMessage mapRowToScheduledMessage(ResultSet rs, int rowNum) throws SQLException {
		return new ScheduledMessage(rs.getLong("id"), rs.getString("patient_uuid"), rs.getString("phone_number"),
		        rs.getString("message"), rs.getDate("scheduled_date"), rs.getString("status"),
		        rs.getTimestamp("sent_timestamp"));
	}
}
