```java
I have following java method:
public void processPabAccountChange(String userId, List<PwsValidateTransactions> transactionsList) {
        CompanyAndAccountsForUserResourceFeaturesResp companyAccounts = apisClient
                .getV1CompanyAndAccountsForUserResourceFeatures(getV1CompanyRequest(userId,
                        transactionsList.get(ZERO).getResourceId(), transactionsList.get(ZERO).getFeatureId()));
        Map<String, Account> accounts = getV2ValidAccountId(companyAccounts.getResourceFeatureAndCompanies(),
                transactionsList.get(ZERO).getCompanyGroupId());
        transactionsList.parallelStream()
                .forEach(transaction -> mapPabAccountChange(accounts.get(transaction.getAccountNumber()), transaction));
        transactionsList.parallelStream()
                .forEach(transaction -> validateDormantAccount(accounts.get(transaction.getAccountNumber()),
                        transaction));
        transactionsList.parallelStream()
                .forEach(transaction -> validateNraAccount(accounts.get(transaction.getAccountNumber()), transaction));
    }

how do i unit-test this method and make sure parallelStream has not concurrency issue? 
```
