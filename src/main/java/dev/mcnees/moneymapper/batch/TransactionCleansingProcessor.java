package dev.mcnees.moneymapper.batch;

import dev.mcnees.moneymapper.domain.Transaction;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class TransactionCleansingProcessor implements ItemProcessor<Transaction, Transaction> {

	@Override
	public Transaction process(Transaction item) throws Exception {
		item.setDescription(item.getDescription().replaceAll(",",""));
		item.setAmount(-item.getAmount());
		return item;
	}
}
