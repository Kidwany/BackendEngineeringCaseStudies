package com.casestudies.payoutdelivery.platform.payout.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void normalisesToTheCurrencyScale() {
        Money money = Money.of("252000", "EGP");

        assertThat(money.amount()).isEqualTo(new BigDecimal("252000.00"));
        assertThat(money).isEqualTo(Money.of("252000.00", "EGP"));
        assertThat(money).hasToString("252000.00 EGP");
    }

    @Test
    void sameAmountInDifferentCurrenciesIsDifferentMoney() {
        assertThat(Money.of("100", "EGP")).isNotEqualTo(Money.of("100", "USD"));
    }

    @Test
    void rejectsMorePrecisionThanTheCurrencyHas() {
        assertThatThrownBy(() -> Money.of("10.005", "EGP"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EGP");
    }

    @Test
    void rejectsUnknownCurrency() {
        assertThatThrownBy(() -> Money.of("10", "XYZ1")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void knowsWhetherItIsPositive() {
        assertThat(Money.of("0.01", "EGP").isPositive()).isTrue();
        assertThat(Money.of("0", "EGP").isPositive()).isFalse();
        assertThat(Money.of("-5", "EGP").isPositive()).isFalse();
    }
}
