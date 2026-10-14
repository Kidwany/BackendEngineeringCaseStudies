package com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.dto.BankBehaviorResponse;
import com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.dto.ChangeBankBehaviorRequest;
import com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.mapper.BankBehaviorApiMapper;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.ChangeBankBehaviorUseCase;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.GetBankBehaviorUseCase;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bank/behavior")
class BankBehaviorController {

    private final GetBankBehaviorUseCase getBehavior;
    private final ChangeBankBehaviorUseCase changeBehavior;

    BankBehaviorController(GetBankBehaviorUseCase getBehavior, ChangeBankBehaviorUseCase changeBehavior) {
        this.getBehavior = getBehavior;
        this.changeBehavior = changeBehavior;
    }

    @GetMapping
    BankBehaviorResponse current() {
        return BankBehaviorApiMapper.toResponse(getBehavior.current());
    }

    @PutMapping
    BankBehaviorResponse change(@Valid @RequestBody ChangeBankBehaviorRequest request) {
        return BankBehaviorApiMapper.toResponse(changeBehavior.change(BankBehaviorApiMapper.toCommand(request)));
    }
}
