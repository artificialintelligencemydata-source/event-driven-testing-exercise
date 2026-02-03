package com.acuver.autwit.client.sdk.contextAccessor;

import com.acuver.autwit.client.sdk.Autwit;
import com.acuver.autwit.core.domain.ApiContextEntities;
import com.acuver.autwit.core.ports.ApiContextPort;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * API Context Implementation.
 *
 * <h2>DELEGATES TO</h2>
 * ApiContextPort (autwit-core port)
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
class ApiContextImpl implements Autwit.ContextAccessor.ApiContext {

    private final ApiContextPort apiContextPort;

    ApiContextImpl(ApiContextPort apiContextPort) {
        this.apiContextPort = apiContextPort;
    }

    // ==========================================================================
    // STEP-LEVEL QUERIES
    // ==========================================================================

    @Override
    public Optional<String> getLastResponseFromStep(String stepKey, String apiName) {
        return apiContextPort.findLastByStepKeyAndApiName(stepKey, apiName)
                .map(ApiContextEntities::getResponsePayload);
    }

    @Override
    public List<String> getAllResponsesFromStep(String stepKey) {
        return apiContextPort.findAllByStepKey(stepKey).stream()
                .map(ApiContextEntities::getResponsePayload)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<String> getResponseFromPreviousStep(
            String scenarioKey, String stepName, String apiName) {
        return apiContextPort
                .findLastByScenarioKeyAndStepNameAndApiName(
                        scenarioKey, stepName, apiName)
                .map(ApiContextEntities::getResponsePayload);
    }

    @Override
    public boolean hasCalledApi(String stepKey, String apiName) {
        return apiContextPort
                .findLastByStepKeyAndApiName(stepKey, apiName)
                .isPresent();
    }

    @Override
    public long getCallCount(String stepKey) {
        return apiContextPort.findAllByStepKey(stepKey).size();
    }

    // ==========================================================================
    // ORDER-LEVEL QUERIES
    // ==========================================================================

    @Override
    public Optional<String> getResponseByOrderNo(String orderNo) {
        return apiContextPort.findByOrderNo(orderNo)
                .map(ApiContextEntities::getResponsePayload);
    }

    @Override
    public List<String> getAllResponsesForOrder(String orderNo) {
        return apiContextPort.findAllByOrderNo(orderNo).stream()
                .map(ApiContextEntities::getResponsePayload)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<String> getOrderNoFromLastCall(String stepKey, String apiName) {
        return apiContextPort.findLastByStepKeyAndApiName(stepKey, apiName)
                .map(ApiContextEntities::getOrderNo);
    }

    @Override
    public List<ApiContextEntities> trackOrderLifecycle(String orderNo) {
        return apiContextPort.findAllByOrderNo(orderNo);
    }

    // ==========================================================================
    // SCENARIO-LEVEL QUERIES
    // ==========================================================================

    @Override
    public Optional<String> getLastResponseFromScenario(String scenarioKey, String apiName) {
        return apiContextPort
                .findAllByScenarioKeyAndApiName(scenarioKey, apiName)
                .stream()
                .reduce((first, second) -> second) // take last
                .map(ApiContextEntities::getResponsePayload);
    }


    @Override
    public List<String> getAllResponsesFromScenario(String scenarioKey) {
        return apiContextPort.findByScenarioKey(scenarioKey).stream()
                .map(ApiContextEntities::getResponsePayload)
                .collect(Collectors.toList());
    }

    // ==========================================================================
    // CONTEXT PERSISTENCE
    // ==========================================================================

    @Override
    public ApiContextEntities save(ApiContextEntities context) {
        return apiContextPort.save(context);
    }

    @Override
    public Optional<ApiContextEntities> getContext(String stepKey, String apiName) {
        return apiContextPort.findLastByStepKeyAndApiName(stepKey, apiName);
    }

    @Override
    public List<ApiContextEntities> getAllContexts(String stepKey) {
        return apiContextPort.findAllByStepKey(stepKey);
    }
}