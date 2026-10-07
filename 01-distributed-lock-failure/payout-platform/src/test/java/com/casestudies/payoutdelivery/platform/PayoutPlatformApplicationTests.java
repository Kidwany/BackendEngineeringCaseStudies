package com.casestudies.payoutdelivery.platform;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PayoutPlatformApplicationTests {

	@Autowired
	private Flyway flyway;

	@Test
	void contextLoads() {
	}

	// Context startup already ran Flyway and then Hibernate's validate; this pins down that
	// the schema came from the migrations, with nothing pending or failed.
	@Test
	void flywayAppliesAllMigrations() {
		var info = flyway.info();

		assertThat(info.applied()).isNotEmpty()
				.allSatisfy(m -> assertThat(m.getState()).isEqualTo(MigrationState.SUCCESS));
		assertThat(info.pending()).isEmpty();
	}

}
