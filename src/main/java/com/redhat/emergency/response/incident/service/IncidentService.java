package com.redhat.emergency.response.incident.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.emergency.response.incident.entity.Incident;
import com.redhat.emergency.response.incident.model.IncidentStatus;
import com.redhat.emergency.response.incident.repository.IncidentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    @Inject
    IncidentRepository repository;

    @Inject
    MeterRegistry meterRegistry;

    private Timer createTimer;

    private Timer updateTimer;

    void onStart(@Observes StartupEvent e) {
        String name = "incident.service.data.access";
        createTimer = Timer.builder(name).tag("operation", "create").register(meterRegistry);
        updateTimer = Timer.builder(name).tag("operation", "update").register(meterRegistry);
    }

    @Transactional
    public JsonArray incidents() {
        return new JsonArray(repository.findAll().stream().map(this::fromEntity).collect(Collectors.toList()));
    }

    public JsonObject create(JsonObject incident) {
        try {
            return createTimer.recordCallable(() -> doCreate(incident));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public JsonObject doCreate(JsonObject incident) {
        Incident created = repository.create(toEntity(incident));
        return fromEntity(created);
    }

    public JsonObject updateIncident(JsonObject incident) {
        try {
            return updateTimer.recordCallable(() -> doUpdateIncident(incident));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public JsonObject doUpdateIncident(JsonObject incident) {
        Incident current = repository.findByIncidentId(incident.getString("id"));
        if (current == null) {
            log.warn("Incident with id '" + incident.getString("id") + "' not found in the database");
            return null;
        }
        if (incident.getDouble("lat") != null && !BigDecimal.valueOf(incident.getDouble("lat")).toString().equals(current.getLatitude())) {
            current.setLatitude(BigDecimal.valueOf(incident.getDouble("lat")).toString());
        }
        if (incident.getDouble("lon") != null && !BigDecimal.valueOf(incident.getDouble("lon")).toString().equals(current.getLongitude())) {
            current.setLongitude(BigDecimal.valueOf(incident.getDouble("lon")).toString());
        }
        if (incident.getInteger("numberOfPeople") != null && !incident.getInteger("numberOfPeople").equals(current.getNumberOfPeople())) {
            current.setNumberOfPeople(incident.getInteger("numberOfPeople"));
        }
        if (incident.getBoolean("medicalNeeded") != null && !incident.getBoolean("medicalNeeded").equals(current.isMedicalNeeded())) {
            current.setMedicalNeeded(incident.getBoolean("medicalNeeded"));
        }
        if (incident.getString("victimName") != null && !incident.getString("victimName").equals(current.getVictimName())) {
            current.setVictimName(incident.getString("victimName"));
        }
        if (incident.getString("victimPhoneNumber") != null && !incident.getString("victimPhoneNumber").equals(current.getVictimPhoneNumber())) {
            current.setVictimPhoneNumber(incident.getString("victimPhoneNumber"));
        }
        if (incident.getString("status") != null && !incident.getString("status").equals(current.getStatus())) {
            current.setStatus(incident.getString("status"));
        }
        return fromEntity(current);
    }

    @Transactional
    public JsonObject incidentByIncidentId(String incidentId) {
        return fromEntity(repository.findByIncidentId(incidentId));
    }

    @Transactional
    public JsonArray incidentsByStatus(String status) {
        return new JsonArray(repository.findByStatus(status).stream().map(this::fromEntity).collect(Collectors.toList()));
    }

    @Transactional
    public JsonArray incidentsByVictimName(String name) {
        return new JsonArray(repository.findByName(name).stream().map(this::fromEntity).collect(Collectors.toList()));
    }

    @Transactional
    public void reset() {
        repository.deleteAll();
    }

    private JsonObject fromEntity(Incident r) {
        if (r == null) {
            return null;
        }
        return new JsonObject().put("id", r.getIncidentId())
                .put("lat", new BigDecimal(r.getLatitude()).doubleValue())
                .put("lon", new BigDecimal(r.getLongitude()).doubleValue())
                .put("medicalNeeded", r.isMedicalNeeded())
                .put("numberOfPeople", r.getNumberOfPeople())
                .put("victimName", r.getVictimName())
                .put("victimPhoneNumber", r.getVictimPhoneNumber())
                .put("status", r.getStatus())
                .put("timestamp", r.getTimestamp());
    }

    private Incident toEntity(JsonObject incident) {

        String incidentId = UUID.randomUUID().toString();
        long reportedTimestamp = System.currentTimeMillis();

        Incident entity = new Incident();
        entity.setIncidentId(incidentId);
        entity.setLatitude(incident.getDouble("lat") != null ? BigDecimal.valueOf(incident.getDouble("lat")).setScale(5, RoundingMode.HALF_UP).toString() : null);
        entity.setLongitude(incident.getDouble("lon") != null ? BigDecimal.valueOf(incident.getDouble("lon")).setScale(5, RoundingMode.HALF_UP).toString() : null);
        entity.setMedicalNeeded(incident.getBoolean("medicalNeeded"));
        entity.setNumberOfPeople(incident.getInteger("numberOfPeople"));
        entity.setVictimName(incident.getString("victimName"));
        entity.setVictimPhoneNumber(incident.getString("victimPhoneNumber"));
        entity.setReportedTime(Instant.ofEpochMilli(reportedTimestamp));
        entity.setStatus(IncidentStatus.REPORTED.name());
        return entity;
    }

}
