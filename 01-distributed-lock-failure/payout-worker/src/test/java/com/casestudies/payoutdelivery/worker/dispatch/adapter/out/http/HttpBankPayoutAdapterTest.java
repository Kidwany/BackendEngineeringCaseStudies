package com.casestudies.payoutdelivery.worker.dispatch.adapter.out.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

import com.casestudies.payoutdelivery.worker.dispatch.application.port.out.BankPayoutReceipt;
import com.casestudies.payoutdelivery.worker.dispatch.application.port.out.BankPayoutRequest;
import com.casestudies.payoutdelivery.worker.dispatch.application.port.out.BankRejectedException;
import com.casestudies.payoutdelivery.worker.dispatch.application.port.out.BankTimeoutException;
import com.casestudies.payoutdelivery.worker.dispatch.application.port.out.BankUnavailableException;
import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.Money;
import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.PayoutId;
import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.WorkerId;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class HttpBankPayoutAdapterTest {

    private static final Duration READ_TIMEOUT = Duration.ofMillis(300);

    private static final BankPayoutRequest PO_9001 = new BankPayoutRequest(
            PayoutId.of("PO-9001"), MerchantId.of("M-1001"), Money.of("252000", "EGP"), WorkerId.of("worker-a"));

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final AtomicReference<HttpExchange> received = new AtomicReference<>();
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private HttpServer bank;
    private HttpBankPayoutAdapter adapter;

    @BeforeEach
    void startBank() throws IOException {
        bank = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        bank.setExecutor(executor);
        bank.start();
        adapter = adapterFor("http://localhost:" + bank.getAddress().getPort());
    }

    @AfterEach
    void stopBank() {
        bank.stop(0);
        executor.shutdownNow();
    }

    @Test
    void sendsThePayoutAndReturnsTheBankPayoutId() {
        UUID bankPayoutId = UUID.randomUUID();
        answer(201, """
                {"bankPayoutId": "%s", "payoutId": "PO-9001", "status": "CREATED"}""".formatted(bankPayoutId));

        BankPayoutReceipt receipt = adapter.send(PO_9001);

        assertThat(receipt.bankPayoutId()).isEqualTo(bankPayoutId);
        assertThat(received.get().getRequestMethod()).isEqualTo("POST");
        assertThat(received.get().getRequestURI().getPath()).isEqualTo("/api/bank/payouts");
        assertThat(received.get().getRequestHeaders().getFirst("X-Worker-Id")).isEqualTo("worker-a");
        assertThat(received.get().getRequestHeaders().getFirst("Content-Type")).startsWith("application/json");

        JsonNode body = JsonMapper.builder().build().readTree(receivedBody.get());
        assertThat(body.get("payoutId").asString()).isEqualTo("PO-9001");
        assertThat(body.get("merchantId").asString()).isEqualTo("M-1001");
        assertThat(body.get("amount").decimalValue()).isEqualByComparingTo("252000");
        assertThat(body.get("currency").asString()).isEqualTo("EGP");
        assertThat(body.properties()).hasSize(4);
    }

    @Test
    void aClientErrorIsARejection() {
        answer(400, """
                {"title": "Invalid request", "detail": "Header X-Worker-Id is required."}""");

        assertThatThrownBy(() -> adapter.send(PO_9001))
                .isInstanceOf(BankRejectedException.class)
                .hasMessageContaining("PO-9001")
                .hasMessageContaining("400")
                .hasMessageContaining("Header X-Worker-Id is required.");
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 504})
    void aServerErrorMeansTheBankIsUnavailable(int status) {
        answer(status, """
                {"title": "Bank failure"}""");

        assertThatThrownBy(() -> adapter.send(PO_9001))
                .isInstanceOf(BankUnavailableException.class)
                .hasMessageContaining("PO-9001")
                .hasMessageContaining(String.valueOf(status));
    }

    @Test
    void noAnswerInTimeIsAnUnknownOutcome() {
        bank.createContext("/api/bank/payouts", exchange -> {
            try {
                Thread.sleep(READ_TIMEOUT.multipliedBy(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });

        assertThatThrownBy(() -> adapter.send(PO_9001))
                .isInstanceOf(BankTimeoutException.class)
                .hasMessageContaining("PO-9001")
                .hasMessageContaining("outcome is unknown");
    }

    @Test
    void anUnreachableBankIsUnavailable() {
        int closedPort = bank.getAddress().getPort();
        bank.stop(0);

        assertThatThrownBy(() -> adapterFor("http://localhost:" + closedPort).send(PO_9001))
                .isInstanceOf(BankUnavailableException.class)
                .hasMessageContaining("could not be reached");
    }

    private HttpBankPayoutAdapter adapterFor(String baseUrl) {
        return new HttpBankPayoutAdapter(RestClient.builder(), baseUrl, Duration.ofSeconds(1), READ_TIMEOUT);
    }

    private void answer(int status, String json) {
        bank.createContext("/api/bank/payouts", exchange -> {
            received.set(exchange);
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }
}
