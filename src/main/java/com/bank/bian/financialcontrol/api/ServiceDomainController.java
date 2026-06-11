package com.bank.bian.financialcontrol.api;

import com.bank.bian.financialcontrol.model.ControlRecord;
import com.bank.bian.financialcontrol.service.ControlRecordStore;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * BIAN semantic API for the "Financial Control" service domain.
 *
 * Endpoints follow the BIAN action-term style:
 *   GET  /v1/service-domain                          → who am I (SD metadata)
 *   POST /v1/financial-control-record-management-plan/initiate                    → Initiate a control record
 *   GET  /v1/financial-control-record-management-plan                             → Retrieve (list)
 *   GET  /v1/financial-control-record-management-plan/{crId}/retrieve             → Retrieve (single)
 *   PUT  /v1/financial-control-record-management-plan/{crId}/update               → Update
 *   PUT  /v1/financial-control-record-management-plan/{crId}/control              → Control (suspend|resume|terminate)
 */
@RestController
@RequestMapping("/v1")
public class ServiceDomainController {

    private final ControlRecordStore store;

    public ServiceDomainController(ControlRecordStore store) {
        this.store = store;
    }

    @GetMapping("/service-domain")
    public Map<String, String> serviceDomain() {
        return Map.of(
                "serviceDomain", "Financial Control",
                "businessArea", "Business Support",
                "businessDomain", "Finance",
                "functionalPattern", "Manage",
                "assetType", "Financial Control Record",
                "controlRecord", "Financial Control Record Management Plan",
                "version", "0.1.0",
                "phase", "1-shallow"
        );
    }

    @PostMapping("/financial-control-record-management-plan/initiate")
    @CircuitBreaker(name = "serviceDomain")
    public ResponseEntity<ControlRecord> initiate(@RequestBody(required = false) Map<String, Object> properties) {
        return ResponseEntity.status(HttpStatus.CREATED).body(store.initiate(properties));
    }

    @GetMapping("/financial-control-record-management-plan")
    public Collection<ControlRecord> list() {
        return store.list();
    }

    @GetMapping("/financial-control-record-management-plan/{crId}/retrieve")
    public ResponseEntity<ControlRecord> retrieve(@PathVariable String crId) {
        return store.retrieve(crId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/financial-control-record-management-plan/{crId}/update")
    public ResponseEntity<ControlRecord> update(@PathVariable String crId,
                                                @RequestBody Map<String, Object> properties) {
        return store.update(crId, properties)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/financial-control-record-management-plan/{crId}/control")
    public ResponseEntity<?> control(@PathVariable String crId,
                                     @RequestBody Map<String, String> body) {
        try {
            return store.control(crId, body.get("action"))
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
