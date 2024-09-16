package dev.mcnees.moneymapper.configuration;

import java.io.File;
import java.time.format.DateTimeFormatter;

import javax.sql.DataSource;

import dev.mcnees.moneymapper.batch.TransactionIdentifyTransferProcessor;
import dev.mcnees.moneymapper.batch.MultilineQFXReader;
import dev.mcnees.moneymapper.batch.TransactionCleansingProcessor;
import dev.mcnees.moneymapper.batch.TransactionFilterDuplicateItemProcessor;
import dev.mcnees.moneymapper.batch.TransactionClassificationProcessor;
import dev.mcnees.moneymapper.domain.Transaction;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class MoneyMapperConfiguration {

	@Bean
	@StepScope
	public MultilineQFXReader qfxItemReader(@Value("#{jobParameters['input_file']}") File file) {
		return new MultilineQFXReader(new FileSystemResource(file));
	}

	@Bean
	@StepScope
	public FlatFileItemWriter<Transaction> writeFinalOutputToCSV(@Value("#{jobParameters['output_file']}") File file) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
		return new FlatFileItemWriterBuilder<Transaction>()
				.name("txWriter")
				.resource(new FileSystemResource(file))
				.lineSeparator("\r\n").delimited()
				.delimiter(",")
				.fieldExtractor(transaction -> new Object[] {
						transaction.getId(),
						transaction.getDate().format(dateTimeFormatter),
						transaction.getDescription(),
						transaction.getAmount(),
						transaction.getTag(),
						transaction.getCategory()})
				.build();
	}

	@Bean
	public JdbcCursorItemReader<Transaction> databaseTransactionReader(DataSource dataSource) {
		String sql = "select * from MONEY_MAPPER order by date desc";
		return new JdbcCursorItemReaderBuilder<Transaction>()
				.name("moneyMapperTableReader")
				.dataSource(dataSource)
				.sql(sql)
				.rowMapper(new DataClassRowMapper<>(Transaction.class))
				.build();
	}

	@Bean
	public JdbcBatchItemWriter<Transaction> transactionDataTableWriter(DataSource dataSource) {
		String sql = "insert into MONEY_MAPPER values (:id, :date, :description, :amount, :tag, :category, :transfer)";
		return new JdbcBatchItemWriterBuilder<Transaction>()
				.dataSource(dataSource)
				.sql(sql)
				.beanMapped()
				.build();
	}

	@Bean
	public JdbcBatchItemWriter<Transaction> transactionDataTableCategoryUpdate(DataSource dataSource) {
		String sql = "update MONEY_MAPPER set TAG=:tag, CATEGORY=:category where DATE=:date AND DESCRIPTION=:description AND AMOUNT=:amount";
		return new JdbcBatchItemWriterBuilder<Transaction>()
				.dataSource(dataSource)
				.sql(sql)
				.beanMapped()
				.build();
	}

	@Bean
	public JdbcBatchItemWriter<Transaction> transactionDataTableTransferUpdate(DataSource dataSource) {
		String sql = "update MONEY_MAPPER set TAG=:tag, TRANSFER=:transfer where ID=:id";
		return new JdbcBatchItemWriterBuilder<Transaction>()
				.dataSource(dataSource)
				.sql(sql)
				.beanMapped()
				.build();
	}


	@Bean
	public Step stepTransferToDatabase(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			MultilineQFXReader qfxItemReader,
			TransactionFilterDuplicateItemProcessor itemProcessor,
			@Qualifier("transactionDataTableWriter") JdbcBatchItemWriter<Transaction> itemWriter) {
		return new StepBuilder("stepReadAndStoreRawTransactions", jobRepository)
				.<Transaction, Transaction>chunk(100, transactionManager)
				.reader(qfxItemReader)
				.processor(itemProcessor)
				.writer(itemWriter)
				.build();
	}

	@Bean
	public Step stepMarkAccountTransferTransactions(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			JdbcCursorItemReader<Transaction> itemReader,
			TransactionIdentifyTransferProcessor transactionIdentifyTransferProcessor,
			@Qualifier("transactionDataTableTransferUpdate") JdbcBatchItemWriter<Transaction> itemWriter) {

		return new StepBuilder("accountTransferStep", jobRepository)
				.<Transaction, Transaction>chunk(100, transactionManager)
				.reader(itemReader)
				.processor(transactionIdentifyTransferProcessor)
				.writer(itemWriter)
				.build();
	}

	@Bean
	public Step stepCategorizeTransactions(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			JdbcCursorItemReader<Transaction> itemReader,
			TransactionClassificationProcessor itemProcessor,
			@Qualifier("transactionDataTableCategoryUpdate") JdbcBatchItemWriter<Transaction> itemWriter) {
		return new StepBuilder("processTransactions", jobRepository)
				.<Transaction, Transaction>chunk(100, transactionManager)
				.reader(itemReader)
				.processor(itemProcessor)
				.writer(itemWriter)
				.build();
	}

	@Bean
	public Step stepOutputResultsToCsv(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			JdbcCursorItemReader<Transaction> databaseReader,
			TransactionCleansingProcessor processor,
			FlatFileItemWriter<Transaction> csvItemWriter) {
		return new StepBuilder("outputResultsToCsv", jobRepository)
				.<Transaction, Transaction>chunk(100, transactionManager)
				.reader(databaseReader)
				.processor(processor)
				.writer(csvItemWriter)
				.build();
	}

	@Bean
	public Job moneyMapperJob(JobRepository jobRepository,
			Step stepTransferToDatabase,
			Step stepMarkAccountTransferTransactions,
			Step stepCategorizeTransactions,
			Step stepOutputResultsToCsv) {
		return new JobBuilder("moneyMapperJob", jobRepository)
				.incrementer(new RunIdIncrementer())
				.start(stepTransferToDatabase)
				.next(stepMarkAccountTransferTransactions)
				.next(stepCategorizeTransactions)
				.next(stepOutputResultsToCsv)
				.build();
	}
}
