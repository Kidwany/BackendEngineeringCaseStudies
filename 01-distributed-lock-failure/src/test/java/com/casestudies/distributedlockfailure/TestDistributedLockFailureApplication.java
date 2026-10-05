package com.casestudies.distributedlockfailure;

import org.springframework.boot.SpringApplication;

public class TestDistributedLockFailureApplication {

	public static void main(String[] args) {
		SpringApplication.from(DistributedLockFailureApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
