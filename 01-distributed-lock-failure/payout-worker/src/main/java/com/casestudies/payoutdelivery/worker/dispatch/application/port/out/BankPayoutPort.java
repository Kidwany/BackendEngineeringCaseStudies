package com.casestudies.payoutdelivery.worker.dispatch.application.port.out;

public interface BankPayoutPort {
    BankPayoutReceipt send(BankPayoutRequest request);
}
