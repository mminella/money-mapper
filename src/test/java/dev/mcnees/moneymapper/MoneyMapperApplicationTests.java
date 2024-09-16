package dev.mcnees.moneymapper;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

@SpringBatchTest
@SpringBootTest
class MoneyMapperApplicationTests {

	private static final File OUTPUT_FILE = new File("output/test_output.csv");

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	public void setUp() {
		jobRepositoryTestUtils.removeJobExecutions();
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "MONEY_MAPPER");
	}

	@AfterEach
	public void cleanUp() {
		if (OUTPUT_FILE.exists()) {
			OUTPUT_FILE.delete();
		}
	}

	@Test
	void testJobExecution() throws Exception {
		// given
		File inputFile = new File("qfx_files/quickenExport-sample.QFX");

		JobParameters jobParameters = new JobParametersBuilder()
				.addJobParameter("input_file", inputFile, File.class)
				.addJobParameter("output_file", OUTPUT_FILE, File.class)
				.toJobParameters();

		// when
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		Assertions.assertTrue(OUTPUT_FILE.exists());
	}

}
