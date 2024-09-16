package dev.mcnees.moneymapper.batch;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.mcnees.moneymapper.domain.Transaction;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper;
import org.springframework.batch.item.file.transform.DefaultFieldSetFactory;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.FieldSetFactory;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.PatternMatchingCompositeLineTokenizer;
import org.springframework.core.io.Resource;


public class MultilineQFXReader implements ItemStreamReader<Transaction> {

	private FlatFileItemReader<FieldSet> delegate;

	private final StatementTransactionTokenizer statementTransactionTokenizer = new StatementTransactionTokenizer();

	private static final DateTimeFormatter QFX_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

	public MultilineQFXReader(Resource resource) {
		delegate = new FlatFileItemReader<>();
		
		delegate.setResource(resource);

		DefaultLineMapper lineMapper = new DefaultLineMapper();

		PassThroughFieldSetMapper fieldSetMapper = new PassThroughFieldSetMapper();
		lineMapper.setFieldSetMapper(fieldSetMapper);

		lineMapper.setLineTokenizer(getPatternMatchingTokenizer());

		delegate.setLineMapper(lineMapper);

	}

	private LineTokenizer getPatternMatchingTokenizer() {
		PatternMatchingCompositeLineTokenizer lineTokenizer = new PatternMatchingCompositeLineTokenizer();

		Map<String, LineTokenizer> tokenizerMap = new HashMap<>();

		tokenizerMap.put("<STMTTRN>", statementTransactionTokenizer);
		tokenizerMap.put("<TRNAMT>*", statementTransactionTokenizer);
		tokenizerMap.put("<NAME>*", statementTransactionTokenizer);
		tokenizerMap.put("<FITID>*", statementTransactionTokenizer);
		tokenizerMap.put("<DTPOSTED>*", statementTransactionTokenizer);
		tokenizerMap.put("</STMTTRN>", statementTransactionTokenizer);
		tokenizerMap.put("*", new DefaultTokenizer()); //ignore anything that isn't a tag of interest

		lineTokenizer.setTokenizers(tokenizerMap);

		return lineTokenizer;
	}

	@Override
	public Transaction read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		Transaction transaction = new Transaction();
		for (FieldSet line; (line = this.delegate.read()) != null; ) {

			String controlKey = line.readString(0);

			if (controlKey.equals("qfx-data")) {
				String dataField = line.readString(1);
				if (dataField.equals("TRNAMT")) {
					transaction.setAmount(line.readDouble(2));
				}
				else if (dataField.equals("NAME")) {
					transaction.setDescription(line.readString(2));
				}
				else if (dataField.equals("FITID")) {
					transaction.setId(line.readString(2));
				}
				else if(dataField.equals("DTPOSTED")) {
					String dateTimeString = line.readString(2);
					String dateString = dateTimeString.substring(0,8);
					LocalDate transactionDate = LocalDate.parse(dateString, QFX_DATE_FORMAT);
					transaction.setDate(transactionDate);
				}
			}
			else if (controlKey.equals("qfx-end")) {
				return transaction;
			}

		}
		return null;
		//if null is not returned this method will continue forever
	}


	@Override
	public void close() throws ItemStreamException {
		this.delegate.close();
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		this.delegate.open(executionContext);
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		this.delegate.update(executionContext);
	}


	class StatementTransactionTokenizer implements LineTokenizer {

		private FieldSetFactory fieldSetFactory = new DefaultFieldSetFactory();

		@Override
		public FieldSet tokenize(String line) {
			if (line == null) {
				line = "";
			}

			List<String> tokens = new ArrayList<>(doTokenize(line));

			String[] values = tokens.toArray(new String[tokens.size()]);

			return fieldSetFactory.create(values);
		}

		private List<String> doTokenize(String line) {
			ArrayList<String> tokens = new ArrayList<String>();

			if (line.equals("</STMTTRN>")) {
				tokens.add("qfx-end");
				return tokens;
			}

			String[] splitLine = line.split(">");

			//if > 1 then there is a value associated to the tag
			if (splitLine.length > 1) {
				tokens.add("qfx-data");
				// strip off the < at the beginning of the tag
				tokens.add(splitLine[0].substring(1));
				tokens.add(splitLine[1]);
			}
			else {
				tokens.add("qfx-nodata");
			}

			return tokens;
		}
	}

	class DefaultTokenizer implements LineTokenizer {

		private FieldSetFactory fieldSetFactory = new DefaultFieldSetFactory();

		@Override
		public FieldSet tokenize(String line) {
			if (line == null) {
				line = "";
			}

			List<String> tokens = new ArrayList<>(Arrays.asList("qfx-ignore"));

			String[] values = tokens.toArray(new String[tokens.size()]);

			return fieldSetFactory.create(values);
		}
	}

}


