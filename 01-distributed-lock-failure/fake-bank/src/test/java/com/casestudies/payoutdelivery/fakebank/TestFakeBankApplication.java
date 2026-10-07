package com.casestudies.payoutdelivery.fakebank;

import org.springframework.boot.SpringApplication;

public class TestFakeBankApplication {

	public static void main(String[] args) {
		SpringApplication.from(FakeBankApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
