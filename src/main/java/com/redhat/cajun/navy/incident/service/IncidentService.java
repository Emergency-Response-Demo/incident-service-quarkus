package com.redhat.cajun.navy.incident.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.cajun.navy.incident.dao.IncidentDao;
import com.redhat.cajun.navy.incident.model.Incident;
import com.redhat.cajun.navy.incident.model.IncidentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    @Inject
    IncidentDao incidentDao;

    @Transactional
    public List<Incident> incidents() {
        return incidentDao.findAll().stream().map(this::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public Incident create(Incident incident) {
        com.redhat.cajun.navy.incident.entity.Incident created = incidentDao.create(toEntity(incident));

        return fromEntity(created);
    }

    @Transactional
    public Incident updateIncident(Incident incident) {
        com.redhat.cajun.navy.incident.entity.Incident current = incidentDao.findByIncidentId(incident.getId());
        if (current == null) {
            log.warn("Incident with id '" + incident.getId() + "' not found in the database");
            return null;
        }
        com.redhat.cajun.navy.incident.entity.Incident toUpdate = toEntity(incident, current);
        com.redhat.cajun.navy.incident.entity.Incident merged = null;
        try {
            merged = incidentDao.merge(toUpdate);
        } catch (Exception e) {
            log.warn("Exception '" + e.getClass() + "' when updating Incident with id '" + incident.getId() + "'. Incident record is not updated.");
        }
        return fromEntity(merged);
    }

    @Transactional
    public Incident incidentByIncidentId(String incidentId) {
        return fromEntity(incidentDao.findByIncidentId(incidentId));
    }

    @Transactional
    public List<Incident> incidentsByStatus(String status) {
        return incidentDao.findByStatus(status).stream().map(this::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public List<Incident> incidentsByVictimName(String name) {
        return incidentDao.findByName(name).stream().map(this::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public void reset() {
        incidentDao.deleteAll();
    }

    private Incident fromEntity(com.redhat.cajun.navy.incident.entity.Incident r) {

        if (r == null) {
            return null;
        }
        return new Incident.Builder(r.getIncidentId())
                .lat(r.getLatitude())
                .lon(r.getLongitude())
                .medicalNeeded(r.isMedicalNeeded())
                .numberOfPeople(r.getNumberOfPeople())
                .victimName(r.getVictimName())
                .victimPhoneNumber(r.getVictimPhoneNumber())
                .status(r.getStatus())
                .timestamp(r.getTimestamp())
                .build();
    }

    private com.redhat.cajun.navy.incident.entity.Incident toEntity(Incident incident) {

        String incidentId = UUID.randomUUID().toString();
        long reportedTimestamp = System.currentTimeMillis();

        return new com.redhat.cajun.navy.incident.entity.Incident.Builder()
                        .incidentId(incidentId)
                        .latitude(incident.getLat())
                        .longitude(incident.getLon())
                        .medicalNeeded(incident.isMedicalNeeded())
                        .numberOfPeople(incident.getNumberOfPeople())
                        .victimName(incident.getVictimName())
                        .victimPhoneNumber(incident.getVictimPhoneNumber())
                        .reportedTime(reportedTimestamp)
                        .status(IncidentStatus.REPORTED.name())
                        .build();
    }

    private com.redhat.cajun.navy.incident.entity.Incident toEntity(Incident incident, com.redhat.cajun.navy.incident.entity.Incident current) {

        if (incident == null) {
            return null;
        }
        return new com.redhat.cajun.navy.incident.entity.Incident.Builder(current.getId(), current.getVersion())
                .incidentId(incident.getId())
                .latitude(incident.getLat() == null? current.getLatitude() : incident.getLat())
                .longitude(incident.getLon() == null? current.getLongitude() : incident.getLon())
                .medicalNeeded(incident.isMedicalNeeded() == null? current.isMedicalNeeded() : incident.isMedicalNeeded())
                .numberOfPeople(incident.getNumberOfPeople() == null ? current.getNumberOfPeople() : incident.getNumberOfPeople())
                .victimName(incident.getVictimName() == null? current.getVictimName() : incident.getVictimName())
                .victimPhoneNumber(incident.getVictimPhoneNumber() == null? current.getVictimPhoneNumber() : incident.getVictimPhoneNumber())
                .status(incident.getStatus() == null? current.getStatus() : incident.getStatus())
                .reportedTime(current.getTimestamp())
                .build();
    }

}
