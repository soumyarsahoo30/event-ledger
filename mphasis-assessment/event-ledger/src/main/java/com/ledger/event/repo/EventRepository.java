package com.ledger.event.repo;

import com.ledger.event.model.TransactionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<TransactionEvent, String> {


}
