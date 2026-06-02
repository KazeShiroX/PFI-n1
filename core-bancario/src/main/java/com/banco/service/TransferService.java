package com.banco.service;

import com.banco.model.Cuenta;
import com.banco.model.Transaction;
import com.banco.repository.CuentaRepository;
import com.banco.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

@Service
public class TransferService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Value("${alert.api.url:}")
    private String alertApiUrl;

    @Transactional
    public Cuenta initializeAccount(String username, BigDecimal initialBalance) {
        Cuenta cuenta = new Cuenta(username, initialBalance);
        return cuentaRepository.save(cuenta);
    }

    public BigDecimal getBalance(String username) {
        return cuentaRepository.findById(username)
                .map(Cuenta::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional
    public Transaction processTransfer(String fromUser, String toUser, BigDecimal amount) {
        Cuenta sender = cuentaRepository.findById(fromUser).orElse(null);
        Cuenta receiver = cuentaRepository.findById(toUser).orElse(null);
        
        if (sender != null) {
            sender.setBalance(sender.getBalance().subtract(amount));
            cuentaRepository.save(sender);
        }
        if (receiver != null) {
            receiver.setBalance(receiver.getBalance().add(amount));
            cuentaRepository.save(receiver);
        }

        Transaction tx = new Transaction(fromUser, toUser, amount, LocalDateTime.now(), "COMPLETED");
        tx = transactionRepository.save(tx);

        if (amount.compareTo(new BigDecimal("10000")) >= 0) {
            String alertMessage = String.format("{\"txId\":\"%s\",\"from\":\"%s\",\"to\":\"%s\",\"amount\":%s}", 
                    tx.getId(), fromUser, toUser, amount);
            sendAlert(alertMessage);
        }

        return tx;
    }

    private void sendAlert(String alertMessage) {
        if (alertApiUrl == null || alertApiUrl.trim().isEmpty()) {
            System.out.println("[TransferService] URL de alertas no configurada, se descarta la alerta.");
            return;
        }
        try {
            System.out.println("[TransferService] Enviando alerta a: " + alertApiUrl);
            HttpClient client = HttpClient.newHttpClient();
            
            String payload;
            if (alertApiUrl.contains("/publish")) {
                payload = "{\"topic\":\"transfers/alerts\",\"message\":\"" + alertMessage.replace("\"", "\\\"") + "\"}";
            } else {
                payload = alertMessage;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(alertApiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                  .thenAccept(response -> {
                      if (response.statusCode() == 200 || response.statusCode() == 201) {
                          System.out.println("[TransferService] Alerta enviada con éxito.");
                      } else {
                          System.err.println("[TransferService] Error al enviar alerta. Código: " + response.statusCode());
                      }
                  })
                  .exceptionally(ex -> {
                      System.err.println("[TransferService] Fallo al enviar alerta: " + ex.getMessage());
                      return null;
                  });
        } catch (Exception e) {
            System.err.println("[TransferService] Error al enviar alerta: " + e.getMessage());
        }
    }
}

