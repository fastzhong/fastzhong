```java
@Service
public class AuthMatrixServiceImpl implements AuthMatrixService {
    private static final Logger log = LoggerFactory.getLogger(AuthMatrixServiceImpl.class);
    private static final String IS_LAST_AUTHORIZER = "IS_LAST_AUTHORIZER";
    private static final String USER_ACTIVE_STATUS = "A";
    private static final String INVALIDATED_AUTHZ = "INVALIDATED_AUTHZ";
    private static final String CHK_AUTH_MATRIX = "CHK_AUTH_MATRIX";
    private static final String NO_RULES_MATCHED = "No rules matched the account or company or group details provided";
    private static final String TRX_EVENT = "TRX_EVENT";
    private static final String LIMITS_SUCCESS_STATUS = "SR0001";
    private static final String LIMIT_AVLBL = "LIMIT_AVLBL";
    private static final String START = "Start";
    private static final String VERIFIER = "Verifier";
    private static final String APPROVALS_COMPLETED = "ApprovalsCompleted";
    private static final String SENDER = "Sender";
    private static final String COMPLETED = "Completed";
    private static final String PENDING_VERIFICATION = "PENDING_VERIFICATION";
    private static final String PENDING_APPROVAL = "PENDING_AUTHORIZATION";
    private static final String PENDING_SEND = "PENDING_SEND";
    private static final String CURRENT_TRX_STATUS = "CURRENT_TRX_STATUS";
    private MatrixRules matrixRules;
    private Transactions transactions;
    private AuthMatrixGraph authMatrixGraph;
    private TransactionEventToResourceActionMapper trxEventToResourceActionMapper;
    private NodeToResourceActionMapper nodeToResourceActionMapper;
    private AuthMatrixConfig authMatrixConfig;
    private DailyLimitLoader dailyLimitLoader;
    @Autowired
    FxRateService fxRateService;

    public AuthMatrixServiceImpl(MatrixRules matrixRules, Transactions transactions, AuthMatrixGraph authMatrixGraph, TransactionEventToResourceActionMapper trxEventToResourceActionMapper, AuthMatrixConfig authMatrixConfig, NodeToResourceActionMapper nodeToResourceActionMapper, DailyLimitLoader dailyLimitLoader) {
        this.matrixRules = matrixRules;
        this.transactions = transactions;
        this.authMatrixGraph = authMatrixGraph;
        this.trxEventToResourceActionMapper = trxEventToResourceActionMapper;
        this.authMatrixConfig = authMatrixConfig;
        this.nodeToResourceActionMapper = nodeToResourceActionMapper;
        this.dailyLimitLoader = dailyLimitLoader;
    }

    private MatrixContext getAuthMatrixRules(MatrixContext matrixContext, boolean reEvalRules) {
        log.info("Fetching the latest transaction details for the transactionid {}", matrixContext.getTrxDetails().getTrxId());
        TransactionDetails trxDetails = this.transactions.fetchTransactionDetails(matrixContext.getTrxDetails());
        trxDetails.setFxEnabled(this.authMatrixConfig.isFxEnabled() ? "Y" : "N");
        if (trxDetails.getTransactionStatus().getAuthorizationStatus() != null) {
            matrixContext.put("CURRENT_TRX_STATUS", trxDetails.getTransactionStatus().getAuthorizationStatus());
        }

        return this.prepareAuthMatrixRules(matrixContext, trxDetails, reEvalRules);
    }

    private MatrixContext getAuthMatrixRules(MatrixContext matrixContext, boolean reEvalRules, TransactionDetails trxDetails) {
        trxDetails.setFxEnabled(this.authMatrixConfig.isFxEnabled() ? "Y" : "N");
        if (trxDetails.getTransactionStatus().getAuthorizationStatus() != null) {
            matrixContext.put("CURRENT_TRX_STATUS", trxDetails.getTransactionStatus().getAuthorizationStatus());
        }

        return this.prepareAuthMatrixRules(matrixContext, trxDetails, reEvalRules);
    }

    private MatrixContext prepareAuthMatrixRules(MatrixContext matrixContext, TransactionDetails trxDetails, boolean reEvalRules) {
        Map<Integer, List<List<Node>>> existingRulesMap = new HashMap();
        trxDetails.setTrxAmount(matrixContext.getTrxDetails().getTrxAmount());
        trxDetails.setTrxCcy(matrixContext.getTrxDetails().getTrxCcy());
        matrixContext.setTrxDetails(trxDetails);

```
