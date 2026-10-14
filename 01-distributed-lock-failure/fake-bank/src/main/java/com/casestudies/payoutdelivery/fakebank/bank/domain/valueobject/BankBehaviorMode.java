package com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject;

public enum BankBehaviorMode {
    NORMAL,
    DELAY_RESPONSE,
    PROCESS_THEN_DELAY_RESPONSE,
    RETURN_500,
    TIMEOUT
}
