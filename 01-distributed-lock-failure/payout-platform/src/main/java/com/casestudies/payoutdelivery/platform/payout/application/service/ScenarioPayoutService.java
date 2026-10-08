package com.casestudies.payoutdelivery.platform.payout.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.casestudies.payoutdelivery.platform.payout.application.port.in.ResetScenarioPayoutUseCase;
import com.casestudies.payoutdelivery.platform.payout.application.port.in.SeedScenarioPayoutUseCase;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.PayoutRepository;

public class ScenarioPayoutService implements SeedScenarioPayoutUseCase, ResetScenarioPayoutUseCase {

    private final PayoutRepository payouts;

    public ScenarioPayoutService(PayoutRepository payouts) {
        this.payouts = payouts;
    }

    @Override
    @Transactional
    public void seed() {
        if (payouts.findById(ScenarioPayout.PAYOUT_ID).isEmpty()) {
            payouts.save(ScenarioPayout.initialState());
        }
    }

    @Override
    @Transactional
    public void reset() {
        payouts.save(ScenarioPayout.initialState());
    }
}
