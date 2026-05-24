package com.ledger.event.repo;

import com.ledger.event.model.TransactionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<TransactionEvent, String> {

    List<TransactionEvent> findByAccountIdOrderByEventTimestampAsc(String accountId);

    @Query("SELECT COALESCE(SUM(CASE WHEN e.type = 'CREDIT' THEN e.amount ELSE -e.amount END), 0) " +
            "FROM TransactionEvent e WHERE e.accountId = :accountId")
    BigDecimal computeBalanceByAccountId(@Param("accountId") String accountId);

}
