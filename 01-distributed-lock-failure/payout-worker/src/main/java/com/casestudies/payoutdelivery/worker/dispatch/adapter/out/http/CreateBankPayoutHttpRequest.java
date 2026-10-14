package com.casestudies.payoutdelivery.worker.dispatch.adapter.out.http;

import java.math.BigDecimal;

import com.casestudies.payoutdelivery.worker.dispatch.application.port.out.BankPayoutRequest;

record CreateBankPayoutHttpRequest(String payoutId, String merchantId, BigDecimal amount, String currency) {

    static CreateBankPayoutHttpRequest from(BankPayoutRequest request) {
        return new CreateBankPayoutHttpRequest(
                request.payoutId().value(),
                request.merchantId().value(),
                request.amount().amount(),
                request.amount().currency().getCurrencyCode());
    }
}
