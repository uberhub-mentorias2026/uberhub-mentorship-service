package br.com.uberhub.mentorship.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    @Column(name = "schedule_date_time")
    private Instant dateTime;

    @Column(name = "schedule_confirmed_by_mentee")
    private Boolean confirmedByMentee;
}
