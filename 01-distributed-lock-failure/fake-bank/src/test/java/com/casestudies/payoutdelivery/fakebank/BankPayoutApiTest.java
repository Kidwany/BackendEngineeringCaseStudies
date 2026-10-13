package com.casestudies.payoutdelivery.fakebank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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

import com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.dto.BankPayoutResponse;

import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BankPayoutApiTest {

    private static final String PO_9001 = """
            {"payoutId": "PO-9001", "merchantId": "M-1001", "amount": 252000, "currency": "EGP"}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private JsonMapper json;

    @BeforeEach
    void startEmpty() {
        jdbc.sql("DELETE FROM bank_payouts").update();
    }

    @Test
    void createsAndPersistsABankPayout() throws Exception {
        Instant before = Instant.now();

        BankPayoutResponse response = created(send("worker-b", PO_9001));

        assertThat(response.bankPayoutId()).isNotNull();
        assertThat(response.payoutId()).isEqualTo("PO-9001");
        assertThat(response.merchantId()).isEqualTo("M-1001");
        assertThat(response.amount()).isEqualByComparingTo("252000");
        assertThat(response.currency()).isEqualTo("EGP");
        assertThat(response.workerId()).isEqualTo("worker-b");
        assertThat(response.receivedAt()).isAfterOrEqualTo(before);
        assertThat(response.status()).isEqualTo("CREATED");

        Map<String, Object> row = jdbc.sql("SELECT * FROM bank_payouts WHERE id = :id")
                .param("id", response.bankPayoutId()).query().singleRow();
        assertThat(row.get("payout_id")).isEqualTo("PO-9001");
        assertThat(row.get("merchant_id")).isEqualTo("M-1001");
        assertThat((BigDecimal) row.get("amount")).isEqualByComparingTo("252000");
        assertThat(row.get("currency")).isEqualTo("EGP");
        assertThat(row.get("worker_id")).isEqualTo("worker-b");
        assertThat(row.get("received_at")).isNotNull();
        assertThat(row.get("status")).isEqualTo("CREATED");
    }

    @Test
    void theSamePayoutSentTwiceIsPaidTwice() throws Exception {
        BankPayoutResponse first = created(send("worker-b", PO_9001));
        BankPayoutResponse second = created(send("worker-a", PO_9001));

        assertThat(first.bankPayoutId()).isNotEqualTo(second.bankPayoutId());

        List<Map<String, Object>> rows = jdbc.sql("""
                SELECT worker_id, amount FROM bank_payouts WHERE payout_id = 'PO-9001' ORDER BY received_at""")
                .query().listOfRows();
        assertThat(rows).extracting(r -> r.get("worker_id")).containsExactly("worker-b", "worker-a");
        assertThat(rows).allSatisfy(r -> assertThat((BigDecimal) r.get("amount")).isEqualByComparingTo("252000"));
        assertThat(jdbc.sql("SELECT sum(amount) FROM bank_payouts WHERE payout_id = 'PO-9001'")
                .query(BigDecimal.class).single()).isEqualByComparingTo("504000");
    }

    @Test
    void rejectsARequestWithoutAWorkerId() throws Exception {
        mvc.perform(post("/api/bank/payouts").contentType(MediaType.APPLICATION_JSON).content(PO_9001))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Header X-Worker-Id is required."));

        assertNothingPersisted();
    }

    @Test
    void rejectsAnInvalidBody() throws Exception {
        send("worker-b", """
                {"payoutId": "", "merchantId": "M-1001", "amount": -1, "currency": "EGP"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.payoutId").exists())
                .andExpect(jsonPath("$.errors.amount").exists());

        send("worker-b", "{not json").andExpect(status().isBadRequest());

        assertNothingPersisted();
    }

    @Test
    void rejectsAnAmountTheCurrencyCannotHold() throws Exception {
        send("worker-b", """
                {"payoutId": "PO-9001", "merchantId": "M-1001", "amount": 10.005, "currency": "EGP"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("10.005 has more decimal places than EGP allows"));

        assertNothingPersisted();
    }

    private ResultActions send(String workerId, String body) throws Exception {
        return mvc.perform(post("/api/bank/payouts")
                .header("X-Worker-Id", workerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private BankPayoutResponse created(ResultActions result) throws Exception {
        String body = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readValue(body, BankPayoutResponse.class);
    }

    private void assertNothingPersisted() {
        assertThat(jdbc.sql("SELECT count(*) FROM bank_payouts").query(Long.class).single()).isZero();
    }
}
