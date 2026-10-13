package com.casestudies.payoutdelivery.fakebank.bank.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.CreateBankPayoutCommand;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.out.BankPayoutRepository;
import com.casestudies.payoutdelivery.fakebank.bank.domain.aggregate.BankPayout;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankPayoutStatus;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.Money;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.PayoutId;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.WorkerId;

class CreateBankPayoutServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    private final List<BankPayout> saved = new ArrayList<>();
    private final BankPayoutRepository repository = saved::add;
    private final CreateBankPayoutService service =
            new CreateBankPayoutService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsAndStoresABankPayout() {
        BankPayout created = service.create(command("worker-b"));

        assertThat(saved).containsExactly(created);
        assertThat(created.payoutId()).isEqualTo(PayoutId.of("PO-9001"));
        assertThat(created.workerId()).isEqualTo(WorkerId.of("worker-b"));
        assertThat(created.receivedAt()).isEqualTo(NOW);
        assertThat(created.status()).isEqualTo(BankPayoutStatus.CREATED);
    }

    @Test
    void theSamePayoutSentTwiceCreatesTwoBankPayouts() {
        BankPayout first = service.create(command("worker-b"));
        BankPayout second = service.create(command("worker-a"));

        assertThat(saved).hasSize(2);
        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(first.payoutId()).isEqualTo(second.payoutId());
    }

    private static CreateBankPayoutCommand command(String workerId) {
        return new CreateBankPayoutCommand(PayoutId.of("PO-9001"), MerchantId.of("M-1001"),
                Money.of("252000", "EGP"), WorkerId.of(workerId));
    }
}
