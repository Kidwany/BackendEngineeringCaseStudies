package com.casestudies.payoutdelivery.worker.dispatch.application.port.in;

public interface HandlePayoutReadyUseCase {

    void handle(PayoutReadyCommand command);
}
