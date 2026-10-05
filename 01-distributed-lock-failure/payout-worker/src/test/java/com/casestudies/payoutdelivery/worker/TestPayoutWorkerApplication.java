package com.casestudies.payoutdelivery.worker;

import org.springframework.boot.SpringApplication;

public class TestPayoutWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.from(PayoutWorkerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
