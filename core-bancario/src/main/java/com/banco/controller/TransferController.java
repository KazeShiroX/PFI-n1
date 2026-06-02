package com.banco.controller;

import com.banco.model.Transaction;
import com.banco.service.TransferService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/transfer")
public class TransferController {

    @Autowired
    private TransferService transferService;

    @PostMapping
    public ResponseEntity<?> transfer(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {

        String toUser  = (String) body.get("toUser");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());

        // El fromUser viene del token JWT seteado por JwtFilter en los atributos de la petición
        String fromUser = (String) request.getAttribute("username");

        if (fromUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized: Missing user in token"));
        }

        Transaction tx = transferService.processTransfer(fromUser, toUser, amount);

        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "txId", tx.getId(),
            "amount", tx.getAmount()
        ));
    }
}