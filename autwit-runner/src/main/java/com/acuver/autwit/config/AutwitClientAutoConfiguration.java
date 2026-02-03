package com.acuver.autwit.config;

import com.acuver.autwit.client.sdk.Autwit;
import com.acuver.autwit.client.sdk.contextAccessor.AutwitImpl;
import com.acuver.autwit.core.ports.EventMatcherPort;
import com.acuver.autwit.core.ports.ScenarioStatePort;
import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutwitClientAutoConfiguration {
    @Bean
    public Autwit autwit(
            EventMatcherPort matcher,
            ScenarioStatePort scenarioState,
            RuntimeContextPort runtimeContext
    ) {
        return new AutwitImpl(
                matcher,
                scenarioState,
                runtimeContext
        );
    }
}
