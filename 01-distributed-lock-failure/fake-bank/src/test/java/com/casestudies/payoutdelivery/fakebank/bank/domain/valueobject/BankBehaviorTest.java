package com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class BankBehaviorTest {

    @Test
    void acceptsAZeroDelay() {
        assertThat(BankBehavior.normal(Duration.ZERO).delay()).isZero();
    }

    @Test
    void rejectsANegativeDelay() {
        assertThatThrownBy(() -> new BankBehavior(BankBehaviorMode.TIMEOUT, Duration.ofMillis(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new BankBehavior(null, Duration.ZERO)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new BankBehavior(BankBehaviorMode.NORMAL, null))
                .isInstanceOf(NullPointerException.class);
    }
}
