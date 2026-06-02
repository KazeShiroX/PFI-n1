package com.banco.repository;

import com.banco.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByFromUser(String fromUser);
    List<Transaction> findByAmountGreaterThanEqual(BigDecimal amount);
}
