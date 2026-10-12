package com.casestudies.payoutdelivery.worker.dispatch.application.service;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import com.casestudies.payoutdelivery.worker.dispatch.application.port.in.HandlePayoutReadyUseCase;
import com.casestudies.payoutdelivery.worker.dispatch.application.port.in.PayoutReadyCommand;

public class HandlePayoutReadyService implements HandlePayoutReadyUseCase {

    private static final Logger LOG = System.getLogger(HandlePayoutReadyService.class.getName());

    @Override
    public void handle(PayoutReadyCommand command) {
        LOG.log(Level.INFO, "Received PayoutReady {0} for payout {1}, merchant {2}, {3}",
                command.eventId(), command.payoutId(), command.merchantId(), command.amount());
    }
}
