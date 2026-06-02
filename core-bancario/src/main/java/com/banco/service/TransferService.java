package com.banco.service;

import com.banco.model.Transaction;
import com.banco.mqtt.MqttPublisher;
import com.banco.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransferService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MqttPublisher mqttPublisher;

    @Transactional
    public Transaction processTransfer(String fromUser, String toUser, BigDecimal amount) {
        Transaction tx = new Transaction(fromUser, toUser, amount, LocalDateTime.now(), "COMPLETED");
        tx = transactionRepository.save(tx);

        if (amount.compareTo(new BigDecimal("10000")) >= 0) {
            String alertMessage = String.format("{\"txId\":\"%s\",\"from\":\"%s\",\"to\":\"%s\",\"amount\":%s}", 
                    tx.getId(), fromUser, toUser, amount);
            mqttPublisher.publish("transfers/alerts", alertMessage);
        }

        return tx;
    }
}
