package br.com.uberhub.mentorship.model;

public enum MentorshipStatus {
    PENDING,            // criada (matchmaking feito), aguardando agendamento
    SCHEDULED,          // mentor propôs data/hora
    CONFIRMED,          // mentorado confirmou
    COMPLETED,          // sessão realizada + feedback do mentor
    CHANGE_REQUESTED,   // mentorado pediu troca de mentor
    CANCELLED
}
