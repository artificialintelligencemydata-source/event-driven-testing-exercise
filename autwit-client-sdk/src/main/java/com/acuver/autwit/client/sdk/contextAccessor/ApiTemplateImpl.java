//package com.acuver.autwit.client.sdk.contextAccessor;
//
//import com.acuver.autwit.client.sdk.Autwit;
//import com.acuver.autwit.core.ports.ApiTemplatePort;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//class ApiTemplateImpl implements Autwit.ContextAccessor.ApiTemplate {
//
//
//    private final ApiTemplateService apiTemplateService;
//    ApiTemplateImpl(ApiTemplatePort apiTemplatePort) {
//        this.apiTemplatePort = apiTemplatePort;
//    }
//
//    @Override
//    public Optional<String> buildRequest(String apiName, Map<String, String> parameters) {
//        return apiTemplatePort.buildRequest(apiName, parameters);
//    }
//
//    @Override
//    public boolean validateTemplate(String apiName, Map<String, String> parameters) {
//        return apiTemplatePort.validateTemplate(apiName, parameters);
//    }
//
//    @Override
//    public List<String> getMissingParameters(String apiName, Map<String, String> parameters) {
//        return apiTemplatePort.getMissingParameters(apiName, parameters);
//    }
//}
