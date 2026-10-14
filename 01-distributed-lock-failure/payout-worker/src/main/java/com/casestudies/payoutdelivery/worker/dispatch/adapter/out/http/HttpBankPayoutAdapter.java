package com.casestudies.payoutdelivery.worker.dispatch.adapter.out.http;

import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.casestudies.payoutdelivery.worker.dispatch.application.port.out.BankPayoutPort;
import com.casestudies.payoutdelivery.worker.dispatch.application.port.out.BankPayoutReceipt;
import com.casestudies.payoutdelivery.worker.dispatch.application.port.out.BankPayoutRequest;
import com.casestudies.payoutdelivery.worker.dispatch.application.port.out.BankRejectedException;
import com.casestudies.payoutdelivery.worker.dispatch.application.port.out.BankTimeoutException;
import com.casestudies.payoutdelivery.worker.dispatch.application.port.out.BankUnavailableException;

@Component
class HttpBankPayoutAdapter implements BankPayoutPort {

    static final String PAYOUTS_PATH = "/api/bank/payouts";
    static final String WORKER_ID_HEADER = "X-Worker-Id";

    private final RestClient bank;
    private final Duration readTimeout;

    HttpBankPayoutAdapter(RestClient.Builder builder,
                          @Value("${fake-bank.base-url}") String baseUrl,
                          @Value("${fake-bank.connect-timeout}") Duration connectTimeout,
                          @Value("${fake-bank.read-timeout}") Duration readTimeout) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(http);
        requestFactory.setReadTimeout(readTimeout);
        this.bank = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.readTimeout = readTimeout;
    }

    @Override
    public BankPayoutReceipt send(BankPayoutRequest request) {
        try {
            CreateBankPayoutHttpResponse response = bank.post()
                    .uri(PAYOUTS_PATH)
                    .header(WORKER_ID_HEADER, request.workerId().value())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(CreateBankPayoutHttpRequest.from(request))
                    .retrieve()
                    .body(CreateBankPayoutHttpResponse.class);
            if (response == null) {
                throw new BankUnavailableException(request.payoutId(), "the bank answered without a body", null);
            }
            return new BankPayoutReceipt(response.bankPayoutId());
        } catch (HttpClientErrorException e) {
            throw new BankRejectedException(
                    request.payoutId(), e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        } catch (HttpServerErrorException e) {
            throw new BankUnavailableException(
                    request.payoutId(), "the bank answered " + e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            if (isReadTimeout(e)) {
                throw new BankTimeoutException(request.payoutId(), readTimeout, e);
            }
            throw new BankUnavailableException(request.payoutId(), "the bank could not be reached", e);
        }
    }

    private static boolean isReadTimeout(ResourceAccessException e) {
        return e.getCause() instanceof HttpTimeoutException && !(e.getCause() instanceof HttpConnectTimeoutException);
    }
}
