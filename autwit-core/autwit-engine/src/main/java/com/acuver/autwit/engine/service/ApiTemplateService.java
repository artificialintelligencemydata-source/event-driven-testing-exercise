package com.acuver.autwit.engine.service;

import com.acuver.autwit.core.domain.ApiTemplateEntities;
import com.acuver.autwit.core.ports.ApiTemplatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for managing API templates and payload parameterization.
 */
@Service
@RequiredArgsConstructor
public class ApiTemplateService {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    private final ApiTemplatePort templatePort;

    /**
     * Get API template by name.
     */
    public Optional<ApiTemplateEntities> getTemplate(String apiName) {
        return templatePort.findByApiName(apiName);
    }

    /**
     * Build parameterized request payload from template.
     *
     * @param apiName API name
     * @param params  Parameters to substitute (e.g., {"orderNo": "ORD123"})
     * @return Parameterized request payload
     */
    public String buildRequest(String apiName, Map<String, String> params) {
        Optional<ApiTemplateEntities> templateOpt = templatePort.findByApiName(apiName);

        if (templateOpt.isEmpty()) {
            throw new IllegalArgumentException("Template not found for API: " + apiName);
        }

        ApiTemplateEntities template = templateOpt.get();
        String requestTemplate = template.getRequestTemplate();

        // Replace placeholders with actual values
        String result = requestTemplate;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(requestTemplate);

        while (matcher.find()) {
            String placeholder = matcher.group(0);  // {{orderNo}}
            String paramName = matcher.group(1);     // orderNo

            String value = params.get(paramName);
            if (value == null) {
//                logger.warn("Missing parameter: {} for API: {}", paramName, apiName);
                value = "";  // or throw exception based on your requirements
            }

            result = result.replace(placeholder, value);
        }

//        logger.debug("Built request for {}: {}", apiName, result);
        return result;
    }

    /**
     * Create or update API template.
     */
    public ApiTemplateEntities saveTemplate(ApiTemplateEntities template) {
        return templatePort.save(template);
    }

    /**
     * Delete API template.
     */
    public void deleteTemplate(String apiName) {
        templatePort.deleteByApiName(apiName);
    }
}
