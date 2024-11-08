## Validation 

### Initial User & Resource Feature Validation

```
validateUserResourceFeatureActions():
- Validates user permissions
- Checks resource (e.g., CROSS_BORDER_UOBSEND) access
- Validates feature permissions
- Verifies CREATE action permission
```

```java
private void validateUserResourceFeatureActions(
    PwsFileUpload fileUpload,
    PaymentInformation paymentInformation,
    List<String> validationFieldErrorList,
    Map<String, Object> headers,
    String encryptedUserId) {

    // 1. Call Resource Features API
    UserResourceFeaturesActionsData resourceFeaturesActionsData = commonUtil
        .getResourcesAndFeaturesByUserId(encryptedUserId, headers);
    log.info("Resource Features ActionsData: {} ", resourceFeaturesActionsData);

    // 2. Check if Resources/Features exist
    if (ObjectUtils.isEmpty(resourceFeaturesActionsData)) {
        // Handle no resource features found
        paymentInformation.setBulkstatus(ZERO_TWO);  // Set rejection status
        validationFieldErrorList.add("Given user there is no Resource Features Actions found");
    } else {
        // 3. Validate Resource, Feature and Action permissions
        Optional<Resource> resourceFeaturesActions = resourceFeaturesActionsData
            .getUserResourceAndFeatureAccess()
            .getResources()
            .stream()
            // Check Resource ID matches
            .filter(resourceId -> fileUpload.getResourceId().equals(resourceId.getResourceId()))
            // Check Feature ID matches
            .filter(resourceId -> resourceId.getFeatures().stream()
                .anyMatch(feature -> fileUpload.getFeatureId().equals(feature.getFeatureId())))
            // Check CREATE action exists
            .filter(resourceId -> resourceId.getActions().contains(CREATE))
            .findAny();

        // 4. Handle validation failure
        if (resourceFeaturesActions.isEmpty()) {
            paymentInformation.setBulkstatus(ZERO_TWO);
            validationFieldErrorList.add("Given user Resource Features Actions not matched");
        }
    }
}
```

### Company & Account Entitlement Validation

```
validateCompanyAccounts():
- Validates company exists and is authorized
- Checks account belongs to company
- Verifies account permissions
- Validates account-resource-feature combination
- Returns AccountResource for further processing
```

```java
private AccountResource validateCompanyAccounts(
    PwsFileUpload fileUpload, 
    PaymentInformation paymentInformation,
    List<String> validationFieldErrorList, 
    Map<String, Object> headers, 
    String encryptedUserId,
    String accountNumber) {

    // 1. Call v3 Entitlement Service
    CompanyAndAccountsForResourceFeaturesResp v3EntitlementServiceResponse = 
        commonUtil.v3EntitlementEndPointCall(
            encryptedUserId,                 // Encrypted user ID
            fileUpload.getResourceId(),      // Resource ID (e.g., CROSS_BORDER_UOBSEND)
            fileUpload.getFeatureId(),       // Feature ID
            headers                          // API headers
        );
    log.info("EntitlementServiceResponse: {} ", v3EntitlementServiceResponse);

    AccountResource accountResource = null;

    // 2. Check if Entitlement Response is Empty
    if (ObjectUtils.isEmpty(v3EntitlementServiceResponse)) {
        paymentInformation.setBulkstatus(ZERO_TWO);  // Set status as rejected
        validationFieldErrorList.add("Resource and Features is not matched with company accounts");
        return null;
    }

    // 3. Extract Company Accounts
    List<CompanyAccountforUser> companyAccountforUsers = v3EntitlementServiceResponse
        .getCompaniesAccountResourceFeature()
        .stream()
        .map(CompanyAndAccountsForUser::getCompanies)
        .findAny()
        .orElse(null);

    // 4. Validate Company Accounts
    if (CollectionUtils.isNotEmpty(companyAccountforUsers)) {
        // Find accounts matching the company ID
        List<AccountResource> accountResources = companyAccountforUsers.stream()
            .filter(company -> validateComapnyId(company, fileUpload))  // Check company ID match
            .map(CompanyAccountforUser::getAccounts)
            .findAny()
            .orElse(null);

        // 5. Find Matching Account
        if (CollectionUtils.isNotEmpty(accountResources)) {
            // Find the specific account matching the account number
            accountResource = accountResources.stream()
                .filter(resource -> resource.getAccountNumber().equals(accountNumber))
                .findAny()
                .orElse(null);
        }
    }

    // 6. Handle Invalid Account
    if (Objects.isNull(accountResource)) {
        paymentInformation.setBulkstatus(ZERO_TWO);
        validationFieldErrorList.add("Given Account Number is not match with company accounts");
    }

    return accountResource;
}

// Helper method to validate company ID
private boolean validateComapnyId(CompanyAccountforUser company, PwsFileUpload fileUpload) {
    String companyId = transactionUtils.getDecrypted(company.getCompanyId());
    return fileUpload.getCompanyId().equals(Long.parseLong(companyId));
}
```

The entitlement checking process follows this sequence:

1. Initial API Call:

Makes a call to the v3 Entitlement Service with:
```
- Encrypted user ID
- Resource ID (e.g., CROSS_BORDER_UOBSEND)
- Feature ID
- Required headers
```

2. First Level Validation:

```
- Checks if the entitlement response exists
- If no response, marks transaction as rejected (ZERO_TWO)
- Adds error message to validation list
```

3. Company Level Validation:

```
- Extracts company accounts from response
- Validates company ID matches the file upload company ID
- Uses decryption for company ID comparison
```

4. Account Level Validation:
```
- For matching company, gets list of entitled accounts
- Checks if provided account number exists in entitled accounts
- Validates account permissions
```

5. Response Handling:

```
- Returns AccountResource if valid
- Returns null if invalid (with appropriate error messages)
- Updates payment information status if validation fails
```

6. Error Cases:

```java
if (Objects.isNull(accountResource)) {
    // Case 1: No matching account found
    paymentInformation.setBulkstatus(ZERO_TWO);
    validationFieldErrorList.add("Given Account Number is not match with company accounts");
}

if (ObjectUtils.isEmpty(v3EntitlementServiceResponse)) {
    // Case 2: No entitlements found
    paymentInformation.setBulkstatus(ZERO_TWO);
    validationFieldErrorList.add("Resource and Features is not matched with company accounts");
}
```

## Transaction Level Validations

```
validateTransactionDetails():

a) IBAN and Batch Booking:
- IBAN validation for account
- Batch booking settings validation
- Payment code validation for specific currencies
- Transaction amount validation

b) Bank and Contact Details:
- Bank code validation
- SWIFT code validation
- Phone number validation for specific currencies
- Creditor name validation
- Address line validation

c) Currency and Country:
- Currency code validation
- Country code validation
- Purpose code validation for specific currencies
- Transaction currency validation
- Contract ID validation for FX transactions
```

### Validates IBAN and batch booking setting

```java
private boolean ibanValidation(PwsFileUpload fileUpload, 
                             LazyBatchSqlSessionTemplate lazyBatch,
                             List<String> validationFieldErrorList, 
                             String rejectFileOnError, 
                             CountriesResp countriesResp,
                             CreditTransferTransactionInformation creditTransferTransaction) {
    // Get destination country and account number
    String destinationCountry = creditTransferTransaction.getCreditorAgent()
            .getFinancialInstitutionIdentification()
            .getPostalAddress()
            .getCountry();
            
    String ibanAccount = creditTransferTransaction.getCreditorAccount()
            .getIdentification()
            .getOther()
            .getIdentification();

    // Create validation request
    IBanValidationReqSchema reqSchema = new IBanValidationReqSchema();
    reqSchema.setAccountNo(ibanAccount);
    reqSchema.setDestinationCountry(destinationCountry);
    reqSchema.setResourceId(fileUpload.getResourceId());

    // Validate IBAN
    boolean isIbanValidation = commonUtil.getIbanValidation(reqSchema, countriesResp);
    
    // Handle validation failure
    if (!isIbanValidation) {
        handleIbanValidationFailure(fileUpload, lazyBatch, validationFieldErrorList, 
                                  rejectFileOnError, creditTransferTransaction);
        return false;
    }

    return true;
}
```

## Validates bank code

```java
private boolean bankCodeAndSwiftCodeValidation(PwsFileUpload fileUpload, 
                                             LazyBatchSqlSessionTemplate lazyBatch,
                                             List<String> validationFieldErrorList, 
                                             String rejectFileOnError, 
                                             BankListLookUpResp bankListLookUpResp,
                                             CreditTransferTransactionInformation creditTransferTransaction) {
    // Get country code
    String countryCode = creditTransferTransaction.getCreditorAgent()
            .getFinancialInstitutionIdentification()
            .getPostalAddress()
            .getCountry();

    // Find matching bank details
    BankListSchema bankListSchema = findMatchingBankDetails(bankListLookUpResp, 
                                                          fileUpload, countryCode);

    // Get bank code or SWIFT code based on currency
    String bankCodeAndSwiftCode = getBankcodeAndSwiftcode(creditTransferTransaction);

    // Validate bank details
    if (ObjectUtils.isEmpty(bankListSchema)) {
        handleBankCodeValidationError(fileUpload, lazyBatch, validationFieldErrorList, 
                                    rejectFileOnError, creditTransferTransaction, 
                                    bankCodeAndSwiftCode);
        return false;
    }

    return true;
}
```

###  Validates country codes

```java
private boolean countryCodeValidation(PwsFileUpload fileUpload, 
                                    LazyBatchSqlSessionTemplate lazyBatch,
                                    List<String> validationFieldErrorList, 
                                    String rejectFileOnError,
                                    CreditTransferTransactionInformation creditTransferTransaction, 
                                    CountriesResp countriesResp,
                                    String residencyStatus) {
    // Get country and currency codes
    String countryCode = creditTransferTransaction.getCreditorAgent()
            .getFinancialInstitutionIdentification()
            .getPostalAddress()
            .getCountry();
            
    String currencyCode = creditTransferTransaction.getAmount()
            .getInstructedAmount()
            .getCurrency();

    // Validate country exists in allowed list
    Optional<CountryLists> countryList = countriesResp.getCountryList()
            .stream()
            .filter(country -> country.getCountryAlpha2Code()
            .equalsIgnoreCase(countryCode))
            .findAny();

    if (countryList.isEmpty()) {
        handleCountryCodeValidationError(fileUpload, lazyBatch, validationFieldErrorList, 
                                       rejectFileOnError, creditTransferTransaction, 
                                       countryCode);
        return false;
    }

    // Special validation for Ireland and Indonesian Rupiah
    if (COUNTRY_CODE_IE.equals(countryCode) && 
        COUNTRY_CURRENCY_IDR.equals(currencyCode) && 
        ObjectUtils.allNull(residencyStatus)) {
        
        handleResidencyValidationError(fileUpload, lazyBatch, validationFieldErrorList, 
                                     rejectFileOnError, creditTransferTransaction, 
                                     countryCode);
        return false;
    }

    return true;
}
```

## Duplicate Check Validation

```
getDuplicateCheckValidation():
- File level duplicate check
- Transaction level duplicate check
- Handles partial and full rejections
- Updates transaction statuses
```

## Enrichment

```
- Total Amount: Sum of all child transactions
- Highest Amount: Maximum amount among child transactions
- Transaction Currency: From instruction
- Transaction Status: ACCEPTED, DELETED, PENDING 
- Account Currency: From account resource
- Equivalent Amount: Computed from FX rates if applicable
```


