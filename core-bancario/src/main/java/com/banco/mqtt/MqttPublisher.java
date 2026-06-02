package com.banco.mqtt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class MqttPublisher {

    @Value("${mqtt.broker.url:}")
    private String brokerUrl;

    @PostConstruct
    public void init() {

        System.out.println("=== DEBUG MQTT ===");
        System.out.println("Valor de brokerUrl en Bean: " + brokerUrl);
        System.out.println("Is empty? " + (brokerUrl == null || brokerUrl.trim().isEmpty()));
        System.out.println("=================");

        if (brokerUrl != null && !brokerUrl.trim().isEmpty()) {
            System.out.println("[MQTT] Conectando al broker: " + brokerUrl);
        } else {
            System.out.println("[MQTT] Inactivo en este nodo (sin URL configurada)");
        }
    }

    public void publish(String topic, String message) {
        if (brokerUrl != null && !brokerUrl.trim().isEmpty()) {
            System.out.println("[MQTT] Publicando en " + topic + ": " + message);
        } else {
            System.out.println("[MQTT Mock] Evitando publicación en " + topic + " (nodo inactivo): " + message);
        }
    }
}
