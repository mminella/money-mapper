package dev.mcnees.moneymapper.domain;

import java.time.LocalDate;

public class Transaction {

	public Transaction() {

	}

	public Transaction(String id, LocalDate date, String description, Double amount, String tag, String category, Boolean transfer) {
		this.id = id;
		this.date = date;
		this.description = description;
		this.amount = amount;
		this.tag = tag;
		this.category = category;
		this.transfer = transfer;
	}

	private String id;

	private LocalDate date;

	private String description;

	private Double amount;

	private String tag;

	private String category;

	private Boolean transfer;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Boolean getTransfer() {
		return transfer;
	}

	public void setTransfer(Boolean transfer) {
		this.transfer = transfer;
	}


	@Override
	public int hashCode() {
		return (int) date.hashCode() * description.hashCode() * amount.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof Transaction)) {
			return false;
		}

		Transaction other = (Transaction) obj;

		return date.equals(other.getDate()) && description.equals(other.getDescription()) && amount.equals(other.getAmount());
	}
}
