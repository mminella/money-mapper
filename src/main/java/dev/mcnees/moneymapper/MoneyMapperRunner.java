package dev.mcnees.moneymapper;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MoneyMapperRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MoneyMapperRunner.class);

	private final JobLauncher jobLauncher;

	private final Job moneyMapperJob;

	private final JdbcTemplate jdbcTemplate;

	@Value("${qfx_directory:qfx_files}")
	private String qfx_directory_parameter;

	@Value("${output_directory:output}")
	private String output_directory_parameter;

	public MoneyMapperRunner(JobLauncher jobLauncher, Job moneyMapperJob, DataSource dataSource) {
		this.jobLauncher = jobLauncher;
		this.moneyMapperJob = moneyMapperJob;
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {

		clearProcessingData();

		File qfx_directory = new File(qfx_directory_parameter);
		if (!qfx_directory.isDirectory()) {
			log.error("Specified qfx input location of " + qfx_directory_parameter + " is not a directory");
			throw new Exception("Must specify a directory for QFX file processing.");
		}

		File output_directory = new File(output_directory_parameter);
		if (!output_directory.isDirectory()) {
			log.error("Specified output location of " + output_directory_parameter + " is not a directory");
			throw new Exception("Must specify a valid file for output.");
		}
		File output_file = new File(output_directory + "/money_mapper_output.csv");

		List<File> allQuickenFiles = getAllQuickenFiles(qfx_directory);

		JobParameters jobParameters;
		for (File quickenFile : allQuickenFiles) {
			jobParameters = new JobParametersBuilder()
					.addJobParameter("input_file", new JobParameter<>(quickenFile, File.class))
					.addJobParameter("output_file", new JobParameter<>(output_file, File.class))
					.addJobParameter("datetime", new JobParameter<>(Instant.now().toEpochMilli(), Long.class))
					.toJobParameters();

			jobLauncher.run(moneyMapperJob, jobParameters);
		}
	}

	private void clearProcessingData() {
		jdbcTemplate.execute("delete from MONEY_MAPPER");
	}

	private List<File> getAllQuickenFiles(File folder) {
		List<File> fileList = new ArrayList<>();

		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			if (file.isFile() && file.getName().endsWith(".QFX")) {
				log.info("Parser::getAllQuickenFiles -> found Quicken file: " + file.getName());
				fileList.add(file);
			}
		}
		return fileList;
	}

}
