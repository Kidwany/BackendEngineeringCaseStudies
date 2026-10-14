package com.casestudies.payoutdelivery.fakebank.bank.application.port.out;

import java.time.Duration;

public interface Sleeper {

    void sleep(Duration duration);
}
