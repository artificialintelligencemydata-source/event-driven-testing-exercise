package com.acuver.autwit.adapter.kafka;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "autwit.kafka.enabled", havingValue = "true")
public class KafkaProducer {

    private static final Logger log = LogManager.getLogger(KafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaProperties props;

    public KafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, KafkaProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    public void publishEvent(String key, Object payload) {
        log.debug("Publishing event to topic={} | key={} | payload={}",
                props.getTopic(), key, payload);

        kafkaTemplate.send(props.getTopic(), key, payload);
    }
}
