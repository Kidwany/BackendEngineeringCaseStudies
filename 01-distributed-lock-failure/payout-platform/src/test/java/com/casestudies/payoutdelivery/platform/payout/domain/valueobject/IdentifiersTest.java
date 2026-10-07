package com.casestudies.payoutdelivery.platform.payout.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IdentifiersTest {

    @Test
    void identifiersCompareByValue() {
        assertThat(PayoutId.of("PO-9001")).isEqualTo(PayoutId.of("PO-9001")).hasToString("PO-9001");
        assertThat(MerchantId.of("M-1001")).isEqualTo(MerchantId.of("M-1001")).hasToString("M-1001");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void rejectBlankValues(String blank) {
        assertThatThrownBy(() -> PayoutId.of(blank)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MerchantId.of(blank)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectNull() {
        assertThatThrownBy(() -> PayoutId.of(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> MerchantId.of(null)).isInstanceOf(NullPointerException.class);
    }
}
