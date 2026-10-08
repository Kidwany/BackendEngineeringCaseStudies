package com.casestudies.payoutdelivery.platform.payout.adapter.in.startup;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.casestudies.payoutdelivery.platform.payout.application.port.in.SeedScenarioPayoutUseCase;

@Component
class ScenarioPayoutSeeder implements ApplicationRunner {

    private final SeedScenarioPayoutUseCase seedScenarioPayout;

    ScenarioPayoutSeeder(SeedScenarioPayoutUseCase seedScenarioPayout) {
        this.seedScenarioPayout = seedScenarioPayout;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedScenarioPayout.seed();
    }
}
