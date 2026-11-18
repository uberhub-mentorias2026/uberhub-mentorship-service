package br.com.uberhub.mentorship.mapper;

import org.springframework.stereotype.Component;

import br.com.uberhub.mentorship.dto.response.MentorshipScheduleResponse;
import br.com.uberhub.mentorship.model.Mentorship;
import br.com.uberhub.mentorship.model.Schedule;

@Component
public class MentorshipScheduleMapper {

    public MentorshipScheduleResponse toResponse(Mentorship mentorship) {
        Schedule schedule = mentorship.getSchedule();
        return new MentorshipScheduleResponse(
                mentorship.getId(),
                mentorship.getMentorId(),
                mentorship.getMenteeId(),
                mentorship.getStatus(),
                schedule != null ? schedule.getDateTime() : null,
                schedule != null ? schedule.getConfirmedByMentee() : null,
                mentorship.getUpdatedAt());
    }
}
