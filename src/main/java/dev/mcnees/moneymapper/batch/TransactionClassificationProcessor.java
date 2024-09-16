package dev.mcnees.moneymapper.batch;

import java.util.List;

import dev.mcnees.moneymapper.configuration.TransactionClassificationProperties;
import dev.mcnees.moneymapper.domain.Transaction;
import dev.mcnees.moneymapper.domain.TransactionClassification;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class TransactionClassificationProcessor implements ItemProcessor<Transaction, Transaction> {

	private final List<TransactionClassification> transactionClassificationList;

	public TransactionClassificationProcessor(TransactionClassificationProperties transactionClassificationProperties) {
		this.transactionClassificationList = transactionClassificationProperties.getConfigurations();
	}

	@Override
	public Transaction process(Transaction item) throws Exception {

		// If the transaction was identified as a transfer don't categorize it
		if(item.getTransfer() == Boolean.TRUE) {
			return item;
		}

		for (TransactionClassification transactionClassification : transactionClassificationList) {
			if (item.getDescription().toUpperCase().contains(transactionClassification.description().toUpperCase())) {
				item.setTag(transactionClassification.tag());
				item.setCategory(transactionClassification.category());
				return item;
			}
		}

		item.setTag("General");
		return item;
	}
}
