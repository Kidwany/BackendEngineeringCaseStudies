package com.casestudies.payoutdelivery.fakebank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BankBehaviorApiTest {

    private static final String PO_9001 = """
            {"payoutId": "PO-9001", "merchantId": "M-1001", "amount": 252000, "currency": "EGP"}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void startEmpty() {
        jdbc.sql("DELETE FROM bank_payouts").update();
    }

    // The behavior lives in a bean shared with the other tests in this Spring context.
    @AfterEach
    void backToNormal() throws Exception {
        behave("""
                {"mode": "NORMAL", "delayMillis": 5000}""").andExpect(status().isOk());
    }

    @Test
    void startsInNormalModeWithTheConfiguredDelay() throws Exception {
        mvc.perform(get("/api/bank/behavior"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("NORMAL"))
                .andExpect(jsonPath("$.delayMillis").value(5000));
    }

    @Test
    void changesTheBehaviorAndKeepsTheDelayWhenNoneIsGiven() throws Exception {
        behave("""
                {"mode": "TIMEOUT", "delayMillis": 1500}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("TIMEOUT"))
                .andExpect(jsonPath("$.delayMillis").value(1500));

        behave("""
                {"mode": "RETURN_500"}""")
                .andExpect(jsonPath("$.mode").value("RETURN_500"))
                .andExpect(jsonPath("$.delayMillis").value(1500));

        mvc.perform(get("/api/bank/behavior")).andExpect(jsonPath("$.mode").value("RETURN_500"));
    }

    @Test
    void rejectsAnInvalidBehavior() throws Exception {
        behave("""
                {"mode": "NORMAL", "delayMillis": -1}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.delayMillis").exists());
        behave("""
                {"delayMillis": 100}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.mode").exists());
        behave("""
                {"mode": "EXPLODE"}""").andExpect(status().isBadRequest());

        mvc.perform(get("/api/bank/behavior")).andExpect(jsonPath("$.mode").value("NORMAL"));
    }

    @Test
    void delayResponseProcessesLate() throws Exception {
        behave("""
                {"mode": "DELAY_RESPONSE", "delayMillis": 300}""");

        long started = System.nanoTime();
        send().andExpect(status().isCreated());

        assertThat(Duration.ofNanos(System.nanoTime() - started)).isGreaterThanOrEqualTo(Duration.ofMillis(300));
        assertThat(bankPayouts()).isOne();
    }

    // The unknown-outcome case: a caller that gives up before the response sees no answer,
    // but the bank has already paid.
    @Test
    void processThenDelayResponseCommitsThePayoutBeforeResponding() throws Exception {
        behave("""
                {"mode": "PROCESS_THEN_DELAY_RESPONSE", "delayMillis": 3000}""");

        CompletableFuture<Integer> response = CompletableFuture.supplyAsync(() -> {
            try {
                return send().andReturn().getResponse().getStatus();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        await().atMost(Duration.ofSeconds(2)).until(() -> bankPayouts() == 1);
        assertThat(response).isNotDone();

        assertThat(response.get()).isEqualTo(201);
        assertThat(bankPayouts()).isOne();
    }

    @Test
    void return500PersistsNothing() throws Exception {
        behave("""
                {"mode": "RETURN_500"}""");

        send().andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("urn:problem:simulated-bank-failure"));

        assertThat(bankPayouts()).isZero();
    }

    @Test
    void timeoutHoldsTheRequestThenPersistsNothing() throws Exception {
        behave("""
                {"mode": "TIMEOUT", "delayMillis": 300}""");

        long started = System.nanoTime();
        send().andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.type").value("urn:problem:simulated-bank-timeout"));

        assertThat(Duration.ofNanos(System.nanoTime() - started)).isGreaterThanOrEqualTo(Duration.ofMillis(300));
        assertThat(bankPayouts()).isZero();
    }

    private ResultActions behave(String body) throws Exception {
        return mvc.perform(put("/api/bank/behavior").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions send() throws Exception {
        return mvc.perform(post("/api/bank/payouts")
                .header("X-Worker-Id", "worker-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content(PO_9001));
    }

    private long bankPayouts() {
        return jdbc.sql("SELECT count(*) FROM bank_payouts WHERE payout_id = 'PO-9001'").query(Long.class).single();
    }
}
