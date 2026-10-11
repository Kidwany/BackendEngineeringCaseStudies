package com.casestudies.payoutdelivery.platform.payout.application.port.out;

public interface OutboxPublisherPort {

    void publish(OutboxMessage message);
}
