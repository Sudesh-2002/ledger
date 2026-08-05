package com.sudesh.ledger.query.api;

import com.sudesh.ledger.query.projection.AccountProjectionRebuildService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/projections")
public class ProjectionAdminController {

    private final AccountProjectionRebuildService rebuildService;

    public ProjectionAdminController(AccountProjectionRebuildService rebuildService) {
        this.rebuildService = rebuildService;
    }

    @PostMapping("/rebuild")
    public ResponseEntity<Void> rebuild() {
        rebuildService.rebuildAll();
        return ResponseEntity.ok().build();
    }
}