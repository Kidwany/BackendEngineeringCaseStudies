package com.casestudies.payoutdelivery.worker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PayoutWorkerApplicationTests {

	@Test
	void contextLoads() {
	}

}
