package com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.dto.BankPayoutResponse;
import com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.dto.CreateBankPayoutRequest;
import com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.mapper.BankPayoutApiMapper;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.CreateBankPayoutUseCase;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bank/payouts")
class BankPayoutController {

    static final String WORKER_ID_HEADER = "X-Worker-Id";

    private final CreateBankPayoutUseCase createBankPayout;

    BankPayoutController(CreateBankPayoutUseCase createBankPayout) {
        this.createBankPayout = createBankPayout;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BankPayoutResponse create(@RequestHeader(WORKER_ID_HEADER) String workerId,
                              @Valid @RequestBody CreateBankPayoutRequest request) {
        return BankPayoutApiMapper.toResponse(
                createBankPayout.create(BankPayoutApiMapper.toCommand(request, workerId)));
    }
}
