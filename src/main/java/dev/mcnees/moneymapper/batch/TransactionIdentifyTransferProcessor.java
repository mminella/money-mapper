package dev.mcnees.moneymapper.batch;

import java.sql.Types;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import dev.mcnees.moneymapper.domain.Transaction;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


@Component
public class TransactionIdentifyTransferProcessor implements ItemProcessor<Transaction, Transaction> {

	private final JdbcTemplate jdbcTemplate;

	public TransactionIdentifyTransferProcessor(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Transaction process(Transaction item) throws Exception {

		List<Transaction> matchingAmountTransactions = jdbcTemplate.query("select id, date, amount from MONEY_MAPPER where AMOUNT = ?",
				new Object[] { -item.getAmount() },
				new int[] { Types.FLOAT },
				(rs, rowNum) -> new Transaction(rs.getString("id"), rs.getObject("date", LocalDate.class), null, rs.getDouble("amount"), null, null, null));


		if(matchingAmountTransactions != null && matchingAmountTransactions.size() > 0) {
			LocalDate transactionDate = matchingAmountTransactions.get(0).getDate();
			long daysBetweenPotentialTransfer = item.getDate().until(transactionDate, ChronoUnit.DAYS);
			if (Math.abs(daysBetweenPotentialTransfer) <= 7) {
				item.setTransfer(Boolean.TRUE);
				item.setTag("Automatic Add - Probable Transfer");
			}
		}
		return item;
	}
}
