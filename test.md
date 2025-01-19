# AdviceSummaryImplTest

```java
@ExtendWith(MockitoExtension.class)
class AdviceSummaryImplTest {

    @InjectMocks
    private AdviceSummaryImpl adviceSummaryImpl;

    @Mock
    private TransactionDetailsDao transactionDAO;

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testWithoutPayrollSetup() throws JsonProcessingException {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String adviceSummaryRequest = TemplateJunitUtils.getJsonReosurce("mock/advice_summary_list_request.json");
        AdviceSummaryReq adviceSummaryReq = objectMapper.readValue(adviceSummaryRequest, AdviceSummaryReq.class);
        
        com.uob.gwb.txn.model.Resource resource = new com.uob.gwb.txn.model.Resource();
        resource.setResourceId("Cross-Border-UOBPay");
        resource.setFeatures(Arrays.asList("Single-Transaction"));
        adviceSummaryReq.setResources(List.of(resource));
        
        ConfigDetails configDetails = new ConfigDetails();
        configDetails.setResourceId("Cross-Border-UOBPay");
        configDetails.setConfigValue("UOBPay");
        
        when(transactionDAO.getPwsResourceConfigurationsByResourcesAndCode(Mockito.any(), Mockito.anyString()))
                .thenReturn(List.of(configDetails));
                
        adviceSummaryImpl.buildAdviceSummaryRequest(RequestArg.builder()
                .mapResIdAccountId(Map.of("Cross-Border-UOBPay", List.of("2345445", "76765456")))
                .build(), adviceSummaryReq, new PDRAdviceListReq());
                
        verify(transactionDAO).getPwsResourceConfigurationsByResourcesAndCode(Mockito.any(), Mockito.anyString());
    }

    @Test
    void testWithPayrollSetup() throws JsonProcessingException {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String adviceSummaryRequest = TemplateJunitUtils.getJsonReosurce("mock/advice_summary_list_request.json");
        AdviceSummaryReq adviceSummaryReq = objectMapper.readValue(adviceSummaryRequest, AdviceSummaryReq.class);
        
        com.uob.gwb.txn.model.Resource resource = new com.uob.gwb.txn.model.Resource();
        resource.setResourceId("Payroll-Setup");
        resource.setFeatures(Arrays.asList("Single-Transaction"));
        adviceSummaryReq.setResources(List.of(resource));
        
        ConfigDetails configDetails = new ConfigDetails();
        configDetails.setResourceId("Payroll-Setup");
        configDetails.setConfigValue("PayrollProduct");
        
        // Mock payroll product configuration
        when(transactionDAO.getPwsResourceConfigurationsByResourcesAndCode(Mockito.any(), Mockito.anyString()))
                .thenReturn(List.of(configDetails));
        when(transactionDAO.getPwsPayrollProductConfiguration(Mockito.any()))
                .thenReturn(Map.of("PAYROLL_CODE", "PayrollProduct"));

        Map<String, List<String>> mapResIdAccountId = new HashMap<>();
        mapResIdAccountId.put("Payroll-Setup", List.of("1234567", "7654321"));

        Map<String, String> mapPayrollSetupAccountId = new HashMap<>();
        mapPayrollSetupAccountId.put("Payroll-Setup", "PAYROLL_CODE");

        RequestArg requestArg = RequestArg.builder()
                .mapResIdAccountId(mapResIdAccountId)
                .mapPayrollSetupAccountId(mapPayrollSetupAccountId)
                .build();

        PDRAdviceListReq pdrAdviceListReq = new PDRAdviceListReq();
        adviceSummaryImpl.buildAdviceSummaryRequest(requestArg, adviceSummaryReq, pdrAdviceListReq);

        verify(transactionDAO).getPwsResourceConfigurationsByResourcesAndCode(Mockito.any(), Mockito.anyString());
        verify(transactionDAO).getPwsPayrollProductConfiguration(Mockito.any());
        
        // Verify the result contains both debit and credit advice types
        assertNotNull(pdrAdviceListReq.getUserEntitlements());
        assertNotNull(pdrAdviceListReq.getUserEntitlements().getProductAccountCombinations());
        assertEquals(4, pdrAdviceListReq.getUserEntitlements().getProductAccountCombinations().size());
    }

    @Test
    void testWithEmptyPayrollSetup() throws JsonProcessingException {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String adviceSummaryRequest = TemplateJunitUtils.getJsonReosurce("mock/advice_summary_list_request.json");
        AdviceSummaryReq adviceSummaryReq = objectMapper.readValue(adviceSummaryRequest, AdviceSummaryReq.class);
        
        ConfigDetails configDetails = new ConfigDetails();
        configDetails.setResourceId("Cross-Border-UOBPay");
        configDetails.setConfigValue("UOBPay");
        
        when(transactionDAO.getPwsResourceConfigurationsByResourcesAndCode(Mockito.any(), Mockito.anyString()))
                .thenReturn(List.of(configDetails));

        RequestArg requestArg = RequestArg.builder()
                .mapResIdAccountId(Map.of("Cross-Border-UOBPay", List.of("2345445", "76765456")))
                .mapPayrollSetupAccountId(new HashMap<>()) // Empty payroll setup
                .build();

        PDRAdviceListReq pdrAdviceListReq = new PDRAdviceListReq();
        adviceSummaryImpl.buildAdviceSummaryRequest(requestArg, adviceSummaryReq, pdrAdviceListReq);

        verify(transactionDAO).getPwsResourceConfigurationsByResourcesAndCode(Mockito.any(), Mockito.anyString());
        verify(transactionDAO, never()).getPwsPayrollProductConfiguration(Mockito.any());
    }
}
```

# ListAdvicesServiceImplTest

```java
@Test
void getPayrollSetupTest() {
    CompanyAccountforUser companyAccountforUser = new CompanyAccountforUser();
    companyAccountforUser.setCompanyId("testCompanyId");
    companyAccountforUser.setCompanyName("testCompanyName");

    // Test case 1: Empty setup details
    RequestArg requestArg = RequestArg.builder().build();
    String result = listAdvicesService.getPayrollSetup(companyAccountforUser);
    assertEquals("", result);

    // Test case 2: With valid payroll setup
    List<CompanySetupDetails> setupDetails = new ArrayList<>();
    CompanySetupDetails detail = new CompanySetupDetails();
    detail.setCode("PAYROLL_SETUP");
    detail.setValue("Y");
    setupDetails.add(detail);
    companyAccountforUser.setCompanySetupDetails(setupDetails);
    
    result = listAdvicesService.getPayrollSetup(companyAccountforUser);
    assertEquals("PAYROLL_SETUP", result);

    // Test case 3: With non-payroll setup
    setupDetails = new ArrayList<>();
    detail = new CompanySetupDetails();
    detail.setCode("OTHER_SETUP");
    detail.setValue("Y");
    setupDetails.add(detail);
    companyAccountforUser.setCompanySetupDetails(setupDetails);
    
    result = listAdvicesService.getPayrollSetup(companyAccountforUser);
    assertEquals("", result);

    // Test case 4: With payroll setup but value not 'Y'
    setupDetails = new ArrayList<>();
    detail = new CompanySetupDetails();
    detail.setCode("PAYROLL_SETUP");
    detail.setValue("N");
    setupDetails.add(detail);
    companyAccountforUser.setCompanySetupDetails(setupDetails);
    
    result = listAdvicesService.getPayrollSetup(companyAccountforUser);
    assertEquals("", result);
}

@Test
void getAccountDataStreamWithAccountNumberReqTest() {
    CompanyAccountforUser companyAccountforUser = new CompanyAccountforUser();
    AccountReq accountReq = new AccountReq();
    RequestArg requestArg = RequestArg.builder().build();
    
    // Setup test data
    AccountResource accountResource1 = new AccountResource();
    accountResource1.setAccountNumber("123456");
    AccountResource accountResource2 = new AccountResource();
    accountResource2.setAccountNumber("789012");
    
    companyAccountforUser.setAccounts(Arrays.asList(accountResource1, accountResource2));
    
    // Test case 1: With matching account number in request
    requestArg.setAccountNumberReq(Arrays.asList("123456"));
    Stream<AccountResource> result = listAdvicesService.getAccountDataStream(companyAccountforUser, accountReq, requestArg);
    assertEquals(1, result.count());
    
    // Test case 2: With no matching account number
    requestArg.setAccountNumberReq(Arrays.asList("999999"));
    result = listAdvicesService.getAccountDataStream(companyAccountforUser, accountReq, requestArg);
    assertEquals(0, result.count());
}

@Test
void populateAccountResourceWithPayrollSetupTest() {
    RequestArg requestArg = RequestArg.builder().build();
    AccountResource accountResource = new AccountResource();
    accountResource.setAccountNumber("123456");
    accountResource.setAccountId("ACC001");
    
    // Setup resource
    AccountWithRes accountWithRes = new AccountWithRes();
    accountWithRes.setResourceId("PAYROLL_INTER_ACCOUNT_FUND_TRANSFER");
    accountResource.setResource(Arrays.asList(accountWithRes));
    
    // Test case 1: With valid payroll setup
    listAdvicesService.populateAccountResource(accountResource, requestArg, "PAYROLL_SETUP");
    
    assertTrue(requestArg.getMapPayrollSetupAccountId().containsKey("PAYROLL_SETUP"));
    assertEquals("123456", requestArg.getMapPayrollSetupAccountId().get("PAYROLL_SETUP").get(0));
    
    // Test case 2: Without payroll setup should throw exception
    RequestArg requestArg2 = RequestArg.builder().build();
    assertThrows(ApplicationException.class, 
        () -> listAdvicesService.populateAccountResource(accountResource, requestArg2, ""));
    
    // Test case 3: With non-payroll resource
    RequestArg requestArg3 = RequestArg.builder().build();
    AccountWithRes normalRes = new AccountWithRes();
    normalRes.setResourceId("NORMAL_RESOURCE");
    accountResource.setResource(Arrays.asList(normalRes));
    
    listAdvicesService.populateAccountResource(accountResource, requestArg3, "");
    assertTrue(requestArg3.getMapResIdAccountId().containsKey("NORMAL_RESOURCE"));
}

@Test
void getBulkAdviceListWithAccountsTest() throws Exception {
    AdviceSummaryReq adviceSummaryReq = getAdviceSummaryReq();
    
    // Add accounts to request
    List<AccountReq> accounts = new ArrayList<>();
    AccountReq account1 = new AccountReq();
    account1.setAccountNumber("123456");
    AccountReq account2 = new AccountReq();
    account2.setAccountNumber("789012");
    accounts.add(account1);
    accounts.add(account2);
    adviceSummaryReq.setAccounts(accounts);
    
    when(resourceFeatureUtil.validateBulkResourceAndFeaturesFromJWT(any(), any()))
        .thenReturn(new HashMap<>());
    when(debitAdvicePDRUtils.getV3CompanyAndAccountsForUserData(any()))
        .thenReturn(new CompanyAndAccountsForResourceFeaturesResp());
    when(adviceSummaryReqMapper.mapAdviceSummaryRequest(any(), any()))
        .thenReturn(new PDRAdviceListReq());
    when(debitAdvicePDRUtils.getBulkPDRAdviceSummaryList(any(), any()))
        .thenReturn(loadPDRAdviceSummaryResponse());
    
    AdviceSummaryResp result = listAdvicesService.getBulkAdviceList(adviceSummaryReq);
    
    assertNotNull(result);
    verify(adviceSummaryAdapter).buildAdviceSummaryRequest(any(), any(), any());
}
```
