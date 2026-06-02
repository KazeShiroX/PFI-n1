package com.banco.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cuentas")
public class Cuenta {

    @Id
    @Column(name = "username", nullable = false)
    private String username;

    @Column(nullable = false)
    private BigDecimal balance;

    public Cuenta() {}

    public Cuenta(String username, BigDecimal balance) {
        this.username = username;
        this.balance = balance;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
