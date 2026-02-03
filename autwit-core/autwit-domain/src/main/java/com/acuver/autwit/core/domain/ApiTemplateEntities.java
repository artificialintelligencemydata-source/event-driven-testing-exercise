package com.acuver.autwit.core.domain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiTemplateEntities {
    private Long id;
    private String apiName;
    private String httpMethod;
    private String endpointTemplate;
    private String requestTemplate;
    private String dataRepresentation;
    private Boolean isService;
    private String serviceName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
