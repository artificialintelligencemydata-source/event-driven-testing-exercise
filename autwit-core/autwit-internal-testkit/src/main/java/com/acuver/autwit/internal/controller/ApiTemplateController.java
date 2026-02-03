//package com.acuver.autwit.internal.controller;
//import com.acuver.autwit.core.domain.ApiTemplateEntities;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * REST API Controller for API Template Management.
// *
// * <h2>LAYER ARCHITECTURE</h2>
// * <pre>
// * ApiTemplateController (autwit-testkit)
// *        ↓
// * ApiTemplateService (autwit-engine)
// *        ↓
// * ApiTemplatePort (autwit-domain)
// *        ↓
// * PostgresApiTemplateAdapter (autwit-adapters)
// * </pre>
// *
// * <h2>ENDPOINTS</h2>
// * <ul>
// *   <li>GET    /api/templates              - List all templates</li>
// *   <li>GET    /api/templates/{apiName}    - Get template by name</li>
// *   <li>POST   /api/templates              - Create template</li>
// *   <li>PUT    /api/templates/{apiName}    - Update template</li>
// *   <li>DELETE /api/templates/{apiName}    - Delete template</li>
// *   <li>POST   /api/templates/{apiName}/build - Build request</li>
// * </ul>
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/templates")
//@RequiredArgsConstructor
//public class ApiTemplateController {
//
//    private final ApiTemplateService apiTemplateService;
//
//    /**
//     * Get all API templates.
//     *
//     * @return List of all templates
//     */
//    @GetMapping
//    public ResponseEntity<List<ApiTemplateEntities>> getAllTemplates() {
//        log.info("GET /api/templates - Fetching all templates");
//        List<ApiTemplateEntities> templates = apiTemplateService.getAllTemplates();
//        log.info("Found {} templates", templates.size());
//        return ResponseEntity.ok(templates);
//    }
//
//    /**
//     * Get template by API name.
//     *
//     * @param apiName The API name
//     * @return Template if found, 404 otherwise
//     */
//    @GetMapping("/{apiName}")
//    public ResponseEntity<ApiTemplate> getTemplate(@PathVariable String apiName) {
//        log.info("GET /api/templates/{}", apiName);
//        return apiTemplateService.getTemplate(apiName)
//                .map(template -> {
//                    log.info("Template found: {}", apiName);
//                    return ResponseEntity.ok(template);
//                })
//                .orElseGet(() -> {
//                    log.warn("Template not found: {}", apiName);
//                    return ResponseEntity.notFound().build();
//                });
//    }
//
//    /**
//     * Create new API template.
//     *
//     * @param template Template to create
//     * @return Created template with 201 status
//     */
//    @PostMapping
//    public ResponseEntity<ApiTemplate> createTemplate(@RequestBody ApiTemplate template) {
//        log.info("POST /api/templates - Creating template: {}", template.getApiName());
//
//        // Check if already exists
//        if (apiTemplateService.getTemplate(template.getApiName()).isPresent()) {
//            log.warn("Template already exists: {}", template.getApiName());
//            return ResponseEntity.status(HttpStatus.CONFLICT).build();
//        }
//
//        ApiTemplate saved = apiTemplateService.saveTemplate(template);
//        log.info("Template created: {}", saved.getApiName());
//        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
//    }
//
//    /**
//     * Update existing template.
//     *
//     * @param apiName  API name to update
//     * @param template Updated template data
//     * @return Updated template
//     */
//    @PutMapping("/{apiName}")
//    public ResponseEntity<ApiTemplate> updateTemplate(
//            @PathVariable String apiName,
//            @RequestBody ApiTemplate template) {
//        log.info("PUT /api/templates/{}", apiName);
//
//        // Check if exists
//        if (!apiTemplateService.getTemplate(apiName).isPresent()) {
//            log.warn("Template not found: {}", apiName);
//            return ResponseEntity.notFound().build();
//        }
//
//        // Ensure apiName matches
//        template.setApiName(apiName);
//
//        ApiTemplate updated = apiTemplateService.saveTemplate(template);
//        log.info("Template updated: {}", apiName);
//        return ResponseEntity.ok(updated);
//    }
//
//    /**
//     * Delete template.
//     *
//     * @param apiName API name to delete
//     * @return 204 No Content on success
//     */
//    @DeleteMapping("/{apiName}")
//    public ResponseEntity<Void> deleteTemplate(@PathVariable String apiName) {
//        log.info("DELETE /api/templates/{}", apiName);
//
//        if (!apiTemplateService.getTemplate(apiName).isPresent()) {
//            log.warn("Template not found: {}", apiName);
//            return ResponseEntity.notFound().build();
//        }
//
//        apiTemplateService.deleteTemplate(apiName);
//        log.info("Template deleted: {}", apiName);
//        return ResponseEntity.noContent().build();
//    }
//
//    /**
//     * Build parameterized request from template.
//     *
//     * @param apiName API name
//     * @param params  Parameters for substitution
//     * @return Built request payload
//     */
//    @PostMapping("/{apiName}/build")
//    public ResponseEntity<Map<String, String>> buildRequest(
//            @PathVariable String apiName,
//            @RequestBody Map<String, String> params) {
//        log.info("POST /api/templates/{}/build with {} params", apiName, params.size());
//
//        try {
//            String request = apiTemplateService.buildRequest(apiName, params);
//
//            Map<String, String> response = Map.of(
//                    "apiName", apiName,
//                    "request", request
//            );
//
//            log.info("Request built successfully");
//            return ResponseEntity.ok(response);
//        } catch (IllegalArgumentException e) {
//            log.error("Error building request: {}", e.getMessage());
//            return ResponseEntity.notFound().build();
//        }
//    }
//}
//
//
