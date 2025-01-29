package org.openmrs.module.ehospitalws.task;

import org.openmrs.scheduler.tasks.AbstractTask;

public class AppointmentSmsReminderTask extends AbstractTask {
	
	private final ScheduledAppointmentReminderTask scheduledTask;
	
	public AppointmentSmsReminderTask(ScheduledAppointmentReminderTask scheduledTask) {
		this.scheduledTask = scheduledTask;
	}
	
	@Override
	public void execute() {
		scheduledTask.sendAppointmentReminders();
	}
}
