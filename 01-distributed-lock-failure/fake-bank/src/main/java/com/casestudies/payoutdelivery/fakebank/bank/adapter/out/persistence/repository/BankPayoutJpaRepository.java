package com.casestudies.payoutdelivery.fakebank.bank.adapter.out.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casestudies.payoutdelivery.fakebank.bank.adapter.out.persistence.entity.BankPayoutJpaEntity;

interface BankPayoutJpaRepository extends JpaRepository<BankPayoutJpaEntity, UUID> {
}
