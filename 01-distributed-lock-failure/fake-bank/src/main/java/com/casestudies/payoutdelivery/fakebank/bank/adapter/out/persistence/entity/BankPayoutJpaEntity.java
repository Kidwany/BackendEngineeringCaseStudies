package com.casestudies.payoutdelivery.fakebank.bank.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankPayoutStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bank_payouts")
@Getter
public class BankPayoutJpaEntity {

    @Id
    private UUID id;

    @Column(name = "payout_id", nullable = false, length = 64)
    private String payoutId;

    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "worker_id", nullable = false, length = 64)
    private String workerId;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BankPayoutStatus status;

    protected BankPayoutJpaEntity() {
    }

    public BankPayoutJpaEntity(UUID id, String payoutId, String merchantId, BigDecimal amount, String currency,
                               String workerId, Instant receivedAt, BankPayoutStatus status) {
        this.id = id;
        this.payoutId = payoutId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.workerId = workerId;
        this.receivedAt = receivedAt;
        this.status = status;
    }
}
