# Money Mapper

Money Mapper is a [Spring Batch](https://github.com/spring-projects/spring-batch) application that aggregates and classifies personal financial transactions so you can extract your data.  It is a tool to be used in conjunction with other methods to assist with personal budgeting and financial management.  

It's likely that your personal finances are spread across more than one financial institution.  You may have credit cards, banks and/or investments with more than one company.  If you want to get a complete financial picture you need a way to aggregate several sets of data together.  While there are a lot of heavyweight applications designed to help you manage your money, there are few options to access a complete copy of the raw data.

Often times financial institutions will provide the capability of downloading financial transactions.  You may have the choice between downloading a CSV file and a QFX (Quicken File format) file.  While the CSV file is easily readable, the approach lacks any definition in standards.  Financial institutions are free to change anything about the .csv file, making it difficult or impossible to combine .csv formats from different sources.

Unlike the CSV file, a QFX file follows a set standard.  QFX is designed for the use of importing/exporting transactions and contains additional information likely not in the CSV output.  The downside is that the data is not easy to understand and you often need to import this file into financial software.

# Features

Money Mapper reads multiple QFX files as input and will output a single CSV file with all transactions aggregated.  Throughout the process, Money Mapper performs additional features:

- Classification of Transactions
- Duplicate Transaction Removal
- Identification of Account Transfers

It is also important to note what Money Mapper is not.  Money Mapper is not designed to give analysis on your spending or aggregate totals for review.  Money Mapper does not keep a persistent store of your data throughout multiple runs.  Additionally Money Mapper is not interested in user credentials.  You will need to log into your financial institution and export data as a QFX file in order to use the application.

## Classification of Transactions

A powerful feature of financial software is the ability to automatically identify and classify individual transactions to a larger category.  This may help you determine how much you are spending on utilities or eating out.  The classification methods and categories may be proprietary to a specific financial software and difficult to take complete control over.

Money Mapper allows you to create your own custom approach to classifying transactions.  While this is an optional step to executing a run of Money Mapper, it is the automatic classification that allows the output to be used in context of a larger budgeting plan.  It is the classification of individual transactions that is also the most tedious part of personal budgeting.  Money Mapper allows you to specify your common transactions and eliminate this repetitive process.

Money Mapper will read custom mappings from an `application-transactionclassification.yml` file.  This is a yaml file that is intentionally blank and is designed to be built by each individual user to their own preference.  You will need to edit this file inside of the `src/main/resources` folder.  Each item has a **description**, **tag**, and **category**.  A **description** is the phrase that Money Mapper will look for in the transaction to make a match.  The **description** provided can be a subset of the transaction description and does not need to match completely.  If Money Mapper matches on a transaction, then it will apply the corresponding **tag** and **category**.  The **tag** is meant to be the higher level abstraction of the classification.  An example of this may be 'Utility Bills'.  The **category** is meant to be a further refinement of the classification.  For example, 'Electric Bill'.  The **category** is an optional field.  How you structure this file largely depends on how you want to use the data in the context of a larger financial plan.

## Duplicate Transaction Removal

While downloading multiple QFX files it is possible that you accidentally download the same transaction data more than once.  Money Mapper will recognize this and eliminate duplicate records in the output regardless of duplicate records in the input.

## Identification of Account Transfers

When aggregating transactions from different sources, one thing that hinders further analysis is identifying which transactions are a net spend and which transactions are only a transfer of funds from one place to another.  For example using a checking account to make a credit card payment.  Money Mapper attempts to identify these transactions so they can likely be excluded from analysis in whatever the next step of your budgeting is.

Money Mapper will look at the amount and the date to guess if the transaction is a transfer of funds.  If the amount of the transaction has a matching inverse that is within a few days time, then Money Mapper will automatically apply a **tag** tag that indicates this is a likely Account Transfer.

# How to Run

Money Mapper is a [Spring Boot](https://github.com/spring-projects/spring-boot) application that uses Maven as the build tool.  Money Mapper takes 2 parameters so you can identify the input directory of the local QFX files and the output directory where you'd like the program to write to.  These are optional parameters and will default to a directory _qfx_files_ and _output_ respectively that are both located in the project root.

To run the program you can execute this command inside at the project root directory:

```
mvn spring-boot:run -Dspring-boot.run.arguments="--qfx_directory=/file/system/testDir --output_directory=/file/system/testDir"
```

Additionally you could use Maven to package the .jar file and execute it.  Note that the version number below needs to match the output as defined in the `pom.xml`.

```
mvn clean package
java -jar target/money-mapper-0.0.1-SNAPSHOT.jar --qfx_directory=/file/system/testDir --output_directory=/file/system/testDir
```

### Personal Note

I have always been very interested in finance and as an extension personal finances.  While the personal budgeting systems I've used have varied greatly, they have always been homegrown and the first step has been a custom application that is similar to what Money Mapper does.  While I could use any of the various commercial software platforms for personal financial management, I've found that I want more control than the applications allow.  Additionally, I never liked relying on or paying for software that may change or be retired.  I also don't like the thought of my user credentials and financial data being in yet another place online.  For those reasons my process always started with raw transaction data I manually download.

For my own system, I take the output of Money Mapper and copy/paste the CSV data into an Excel file that is my personal budget.  The Excel file itself has grown into something quite complex, but that is irrelevant here.  When I am asked about how I manage my personal finances, it always starts with a custom Java application to extract data.  That's about as far as the conversation gets before anyone who wants to copy me falls off.  My hope is that someone with a limited tech ability could copy my first step now.  You need to know how to write YAML and execute a maven command and then off you go.

The real benefit of Money Mapper depends on how much work you put in to creating your own `application-transactionclassification.yml`.  Starting with a blank configuration file first means a heavy startup cost.  This is the price of taking full control of the transaction classification.
Whenever I run Money Mapper I often find transactions that are not classified.  For example, it could be a new restaurant I've recently tried for the first time.  If I think I'll go to this restaurant again, I may add an entry to my `application-transactionclassification.yml` so that the classification happens automatically next time.  If I don't ever plan to go back, I will manually make the classification on that transaction in my budgeting spreadsheet and not automate it by adding an entry.  

Please file an issue if you find this useful or have suggestions for improvement!
