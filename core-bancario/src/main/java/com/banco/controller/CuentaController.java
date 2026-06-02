package com.banco.controller;

import com.banco.model.Cuenta;
import com.banco.service.TransferService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class CuentaController {

    @Autowired
    private TransferService transferService;

    @PostMapping("/initialize")
    public ResponseEntity<?> initialize(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        BigDecimal initialBalance = new BigDecimal(body.get("initialBalance").toString());
        
        Cuenta cuenta = transferService.initializeAccount(username, initialBalance);
        return ResponseEntity.ok(cuenta);
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No autorizado"));
        }
        
        BigDecimal balance = transferService.getBalance(username);
        return ResponseEntity.ok(Map.of("username", username, "balance", balance));
    }
}
