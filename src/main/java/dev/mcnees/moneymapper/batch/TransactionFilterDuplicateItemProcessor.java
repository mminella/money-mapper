package dev.mcnees.moneymapper.batch;

import java.util.HashSet;
import java.util.Set;

import dev.mcnees.moneymapper.domain.Transaction;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class TransactionFilterDuplicateItemProcessor implements ItemProcessor<Transaction, Transaction> {

	private final Set<Transaction> seenTransactions = new HashSet<>();

	@Override
	public Transaction process(Transaction item) throws Exception {
		if (seenTransactions.contains(item)) {
			return null;
		}

		seenTransactions.add(item);
		return item;
	}
}
