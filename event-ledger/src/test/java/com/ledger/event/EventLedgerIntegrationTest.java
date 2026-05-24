package com.ledger.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.event.dto.EventRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventLedgerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private EventRequest buildRequest(String eventId, String accountId, String type,
                                      double amount, String timestamp) {
        EventRequest req = new EventRequest();
        req.setEventId(eventId);
        req.setAccountId(accountId);
        req.setType(type);
        req.setAmount(BigDecimal.valueOf(amount));
        req.setCurrency("USD");
        req.setEventTimestamp(timestamp);
        return req;
    }

    private void postEvent(EventRequest req) throws Exception {
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }

    // -------------------------------------------------------------------------
    // Idempotency
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("First submission returns 201 Created")
    void firstSubmission_returns201() throws Exception {
        EventRequest req = buildRequest("idem-evt-001", "idem-acct-001", "CREDIT", 100.0, "2026-05-15T10:00:00Z");

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("idem-evt-001"));
    }

    @Test
    @DisplayName("Duplicate submission returns 200 OK with the original event")
    void duplicateSubmission_returns200() throws Exception {
        EventRequest req = buildRequest("idem-evt-002", "idem-acct-002", "CREDIT", 100.0, "2026-05-15T10:00:00Z");

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("idem-evt-002"))
                .andExpect(jsonPath("$.amount").value(100.0));
    }

    @Test
    @DisplayName("Duplicate submission does not change the account balance")
    void duplicateSubmission_doesNotChangeBalance() throws Exception {
        EventRequest req = buildRequest("idem-evt-003", "idem-acct-003", "CREDIT", 100.0, "2026-05-15T10:00:00Z");

        postEvent(req);
        postEvent(req);
        postEvent(req);

        mockMvc.perform(get("/accounts/idem-acct-003/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.0));
    }

    // -------------------------------------------------------------------------
    // Out-of-order arrival
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Events are returned sorted by eventTimestamp regardless of arrival order")
    void getEventsByAccount_sortedByEventTimestamp() throws Exception {
        postEvent(buildRequest("sort-evt-003", "sort-acct-001", "CREDIT", 30.0, "2026-05-15T12:00:00Z"));
        postEvent(buildRequest("sort-evt-001", "sort-acct-001", "CREDIT", 10.0, "2026-05-15T10:00:00Z"));
        postEvent(buildRequest("sort-evt-002", "sort-acct-001", "CREDIT", 20.0, "2026-05-15T11:00:00Z"));

        mockMvc.perform(get("/events").param("account", "sort-acct-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].eventId").value("sort-evt-001"))
                .andExpect(jsonPath("$[1].eventId").value("sort-evt-002"))
                .andExpect(jsonPath("$[2].eventId").value("sort-evt-003"));
    }

    @Test
    @DisplayName("Balance is correct even when events arrive out of order")
    void balance_correctWithOutOfOrderArrival() throws Exception {
        postEvent(buildRequest("ooo-evt-debit",  "ooo-acct-001", "DEBIT",   40.0, "2026-05-15T11:00:00Z"));
        postEvent(buildRequest("ooo-evt-credit", "ooo-acct-001", "CREDIT", 100.0, "2026-05-15T10:00:00Z"));

        mockMvc.perform(get("/accounts/ooo-acct-001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(60.0));
    }

    // -------------------------------------------------------------------------
    // Balance computation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Balance equals total credits minus total debits")
    void balance_creditMinusDebit() throws Exception {
        postEvent(buildRequest("bal-evt-c1", "bal-acct-001", "CREDIT", 200.0, "2026-05-15T09:00:00Z"));
        postEvent(buildRequest("bal-evt-c2", "bal-acct-001", "CREDIT",  50.0, "2026-05-15T10:00:00Z"));
        postEvent(buildRequest("bal-evt-d1", "bal-acct-001", "DEBIT",   75.0, "2026-05-15T11:00:00Z"));

        mockMvc.perform(get("/accounts/bal-acct-001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("bal-acct-001"))
                .andExpect(jsonPath("$.balance").value(175.0));
    }

    @Test
    @DisplayName("Balance for an account with no events returns 0")
    void balance_noEvents_returnsZero() throws Exception {
        mockMvc.perform(get("/accounts/bal-acct-empty/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    @DisplayName("Balance is isolated per account")
    void balance_isolatedPerAccount() throws Exception {
        postEvent(buildRequest("iso-evt-a1", "iso-acct-A", "CREDIT", 500.0, "2026-05-15T09:00:00Z"));
        postEvent(buildRequest("iso-evt-b1", "iso-acct-B", "CREDIT", 200.0, "2026-05-15T09:00:00Z"));
        postEvent(buildRequest("iso-evt-b2", "iso-acct-B", "DEBIT",   50.0, "2026-05-15T10:00:00Z"));

        mockMvc.perform(get("/accounts/iso-acct-A/balance"))
                .andExpect(jsonPath("$.balance").value(500.0));

        mockMvc.perform(get("/accounts/iso-acct-B/balance"))
                .andExpect(jsonPath("$.balance").value(150.0));
    }

    // -------------------------------------------------------------------------
    // Input validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Missing eventId returns 400 with field error")
    void validation_missingEventId() throws Exception {
        EventRequest req = buildRequest(null, "val-acct-001", "CREDIT", 100.0, "2026-05-15T10:00:00Z");

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.eventId").value("eventId is required"));
    }

    @Test
    @DisplayName("Missing accountId returns 400 with field error")
    void validation_missingAccountId() throws Exception {
        EventRequest req = buildRequest("val-evt-001", null, "CREDIT", 100.0, "2026-05-15T10:00:00Z");

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.accountId").value("accountId is required"));
    }

    @Test
    @DisplayName("Invalid type returns 400 with field error")
    void validation_invalidType() throws Exception {
        EventRequest req = buildRequest("val-evt-002", "val-acct-002", "TRANSFER", 100.0, "2026-05-15T10:00:00Z");

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.type").value("type must be either CREDIT or DEBIT"));
    }

    @Test
    @DisplayName("Zero amount returns 400 with field error")
    void validation_zeroAmount() throws Exception {
        EventRequest req = buildRequest("val-evt-003", "val-acct-003", "CREDIT", 100.0, "2026-05-15T10:00:00Z");
        req.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").value("amount must be greater than 0"));
    }

    @Test
    @DisplayName("Negative amount returns 400 with field error")
    void validation_negativeAmount() throws Exception {
        EventRequest req = buildRequest("val-evt-004", "val-acct-004", "CREDIT", 100.0, "2026-05-15T10:00:00Z");
        req.setAmount(BigDecimal.valueOf(-50.0));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").value("amount must be greater than 0"));
    }

    @Test
    @DisplayName("Missing currency returns 400 with field error")
    void validation_missingCurrency() throws Exception {
        EventRequest req = buildRequest("val-evt-005", "val-acct-005", "CREDIT", 100.0, "2026-05-15T10:00:00Z");
        req.setCurrency(null);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.currency").value("currency is required"));
    }

    @Test
    @DisplayName("Missing eventTimestamp returns 400 with field error")
    void validation_missingTimestamp() throws Exception {
        EventRequest req = buildRequest("val-evt-006", "val-acct-006", "CREDIT", 100.0, null);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.eventTimestamp").value("eventTimestamp is required"));
    }

    // -------------------------------------------------------------------------
    // GET /events/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Get event by ID returns the saved event")
    void getEventById_returnsEvent() throws Exception {
        postEvent(buildRequest("get-evt-001", "get-acct-001", "DEBIT", 25.0, "2026-05-15T08:00:00Z"));

        mockMvc.perform(get("/events/get-evt-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("get-evt-001"))
                .andExpect(jsonPath("$.type").value("DEBIT"))
                .andExpect(jsonPath("$.amount").value(25.0));
    }

    @Test
    @DisplayName("Get event by unknown ID returns 404 with error message")
    void getEventById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/events/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EventId does-not-exist not Found"));
    }

    @Test
    @DisplayName("Get events for account with no events returns empty list")
    void getEventsByAccount_noEvents_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/events").param("account", "get-acct-empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}