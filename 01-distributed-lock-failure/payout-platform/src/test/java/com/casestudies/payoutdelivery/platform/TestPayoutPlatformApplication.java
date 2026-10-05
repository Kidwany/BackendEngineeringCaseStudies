package com.casestudies.payoutdelivery.platform;

import org.springframework.boot.SpringApplication;

public class TestPayoutPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.from(PayoutPlatformApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
