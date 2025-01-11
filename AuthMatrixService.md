```java
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
        log.info("Checking if the transaction authz matrix is already available in the db");
        TransactionAuthState transactionAuthState = new TransactionAuthState();
        transactionAuthState.setTrxId(matrixContext.getTrxDetails().getTrxId());
        List<TransactionAuthState> existingTransactionAuthStates = this.transactions.fetchTransactionAuthState(transactionAuthState);
        if (!CollectionUtils.isEmpty(existingTransactionAuthStates)) {
            log.info("Returning existing transaction state");
            log.debug("Transaction State is {}", existingTransactionAuthStates);
            existingTransactionAuthStates.stream().forEach((existingTrxState) -> {
                existingRulesMap.put(existingTrxState.getRuleId(), (List)((Map)JsonUtil.fromJsonString(new TypeReference<Map<Integer, List<List<Node>>>>() {
                }, existingTrxState.getTrxState())).get(existingTrxState.getRuleId()));
            });
            if (reEvalRules) {
                Map<Integer, List<List<Node>>> newRulesMap = this.fetchMatchingRules(matrixContext, trxDetails);
                this.compareRulesMapAndUpdateIfRequired(existingRulesMap, newRulesMap, matrixContext, existingTransactionAuthStates);
            }

            return matrixContext;
        } else {
            log.info("Existing trx state not found . Fetching auth matrix rules for the passed criteria");
            matrixContext.setRulesMap(this.fetchMatchingRules(matrixContext, trxDetails));
            return matrixContext;
        }
    }

    private MatrixContext compareRulesMapAndUpdateIfRequired(Map<Integer, List<List<Node>>> existingRulesMap, Map<Integer, List<List<Node>>> newRulesMap, MatrixContext matrixContext, List<TransactionAuthState> existingTransactionAuthStates) {
        log.info("Existing Rules Map {}", existingRulesMap.toString());
        log.info("New Rules Map {}", newRulesMap.toString());
        Map<Long, List<String>> invalidAuthzDataPositions = (Map)matrixContext.get("INVALIDATED_AUTHZ");
        List<Node> tmpApprovalNodes = new ArrayList();
        newRulesMap.entrySet().stream().forEach((matrix) -> {
            if (existingRulesMap.containsKey(matrix.getKey())) {
                List<List<Node>> existingRuleNodes = (List)existingRulesMap.get(matrix.getKey());
                List<List<Node>> newRuleNodes = (List)matrix.getValue();
                log.info(newRuleNodes.toString());
                log.info(existingRuleNodes.toString());
                existingRuleNodes.stream().forEach((existingNodes) -> {
                    if (invalidAuthzDataPositions != null && invalidAuthzDataPositions.containsKey((long)(Integer)matrix.getKey())) {
                        Stream var10000 = existingNodes.stream().filter((en) -> {
                            return en.getAction() != null && en.getAction().equals("APPROVAL") && !tmpApprovalNodes.contains(en);
                        });
                        Objects.requireNonNull(tmpApprovalNodes);
                        var10000.forEach(tmpApprovalNodes::add);
                    }

                });
                log.info("Invalid Authz Data Positions {}. Matrix Key : {}. Tmp Approval nodes {} ", new Object[]{invalidAuthzDataPositions, matrix.getKey(), tmpApprovalNodes});
                if (!CollectionUtils.isEmpty(tmpApprovalNodes) && invalidAuthzDataPositions.containsKey((long)(Integer)matrix.getKey())) {
                    ((List)invalidAuthzDataPositions.get((long)(Integer)matrix.getKey())).forEach((val) -> {
                        Optional<Node> node = tmpApprovalNodes.stream().filter((a) -> {
                            return (a.getName() + a.getCompletedByUser()).equals(val);
                        }).findFirst();
                        if (node.isPresent()) {
                            ((Node)node.get()).setState(NodeState.IN_PROGRESS);
                            ((Node)node.get()).setCompletedByUser(0L);
                        }

                    });
                }

                newRuleNodes.stream().forEach((newNodes) -> {
                    newNodes.stream().forEach((newNode) -> {
                        existingRuleNodes.stream().forEach((existingNodes) -> {
                            if (invalidAuthzDataPositions != null && invalidAuthzDataPositions.containsKey((long)(Integer)matrix.getKey())) {
                                Stream var10000 = existingNodes.stream().filter((en) -> {
                                    return en.getAction() != null && en.getAction().equals("APPROVAL") && !tmpApprovalNodes.contains(en);
                                });
                                Objects.requireNonNull(tmpApprovalNodes);
                                var10000.forEach(tmpApprovalNodes::add);
                            }

                            if (existingNodes.contains(newNode)) {
                                Node existingNode = (Node)existingNodes.get(existingNodes.indexOf(newNode));
                                newNode.setState(existingNode.getState());
                                newNode.setCompletedByUser(existingNode.getCompletedByUser());
                                log.info("New Node {}", newNode.toString());
                                log.info("Existing Node {}", existingNode.toString());
                            }

                        });
                    });
                });
            }

            TransactionAuthState trxAuthState = new TransactionAuthState();
            trxAuthState.setTrxId(matrixContext.getTrxDetails().getTrxId());
            trxAuthState.setRuleId((Integer)matrix.getKey());
            if (existingTransactionAuthStates.contains(trxAuthState)) {
                trxAuthState = (TransactionAuthState)existingTransactionAuthStates.get(existingTransactionAuthStates.indexOf(trxAuthState));
                trxAuthState.setTrxState(JsonUtil.toJsonString(matrix));
                trxAuthState.setNewChangeToken(System.currentTimeMillis());
            } else {
                trxAuthState.setTrxState(JsonUtil.toJsonString(matrix));
                trxAuthState.setNewChangeToken(System.currentTimeMillis());
                existingTransactionAuthStates.add(trxAuthState);
            }

        });
        matrixContext.setRulesMap(newRulesMap);
        matrixContext.setTransactionAuthStates(existingTransactionAuthStates);
        return matrixContext;
    }

    private Map<Integer, List<List<Node>>> fetchMatchingRules(MatrixContext matrixContext, TransactionDetails trxDetails) {
        List<MatrixRule> matrixRlz = this.matrixRules.fetchAllMatchingRules(trxDetails);
        log.info("Rules fetched {}", matrixRlz);
        List<MatrixRule> filteredMatrixRlz = null;
        if (matrixRlz != null && !matrixRlz.isEmpty()) {
            filteredMatrixRlz = this.fetchExactAccountorCompanyndResourceMatch(matrixRlz, trxDetails.getResourceId());
            log.info("Filtering based account or company and exact resource match resulted in {}", filteredMatrixRlz);
            if (filteredMatrixRlz.isEmpty()) {
                filteredMatrixRlz = this.fetchExactAccountorCompanyMatchAndResourceMatchByHierarchy(matrixRlz);
            }

            log.info("Filtering based account or company match and resource match by hierarchy resulted in {}", filteredMatrixRlz);
            if (filteredMatrixRlz != null && !filteredMatrixRlz.isEmpty()) {
                filteredMatrixRlz = this.fetchFeatureIdMatch(filteredMatrixRlz, trxDetails.getFeatureId());
                log.info("Filtering based on feature id {} resulted in {}", trxDetails.getFeatureId(), filteredMatrixRlz);
                if (filteredMatrixRlz != null && !filteredMatrixRlz.isEmpty()) {
                    if (this.authMatrixConfig.isFxEnabled()) {
                        filteredMatrixRlz = this.fetchRulesByAmountMatch(filteredMatrixRlz, trxDetails);
                    } else {
                        log.warn("WARNING: FX CHECKS ARE NOT ENABLED. CROSS CURRENCY CASES WILL PRODUCE INVALID RESULTS. PLEASE CHECK IF YOUR CONFIGURATION IS CORRECT BEFORE TAKING THIS CHANGE TO TESTING/LIVE");
                    }

                    log.info("Filtering transaction amount {} {}  based limit amount resulted in {}", new Object[]{trxDetails.getTrxAmount(), trxDetails.getTrxCcy(), filteredMatrixRlz});
                    if (filteredMatrixRlz != null && !filteredMatrixRlz.isEmpty()) {
                        Map<Integer, List<List<Node>>> rulesMap = new HashMap();
                        filteredMatrixRlz.forEach((rule) -> {
                            List<List<Node>> nodes = this.authMatrixGraph.getMatrixPath(rule, matrixContext);
                            rulesMap.put(rule.getRuleId(), nodes);
                        });
                        matrixContext.setApplicableRules(filteredMatrixRlz);
                        return rulesMap;
                    } else {
                        throw new NoRulesMatchedException("No rules matched the account or company or group details provided");
                    }
                } else {
                    throw new NoRulesMatchedException("No rules matched the account or company or group details provided");
                }
            } else {
                throw new NoRulesMatchedException("No rules matched the account or company or group details provided");
            }
        } else {
            throw new NoRulesMatchedException("No rules matched the account or company or group details provided");
        }
    }

    private List<MatrixRule> fetchExactAccountorCompanyndResourceMatch(List<MatrixRule> matrixRlz, String trxResourceId) {
        List<MatrixRule> matchingRules = (List)matrixRlz.stream().filter(RulePredicates.isAccountCompanyResourceMatch(trxResourceId)).collect(Collectors.toList());
        return this.eliminateCompanyMatchesIfAccountMatchFound(matchingRules);
    }

    private List<MatrixRule> eliminateCompanyMatchesIfAccountMatchFound(List<MatrixRule> matchingRules) {
        log.info("Filter resulted in {}. This is before the exact account match check.", matchingRules);
        if (matchingRules.stream().anyMatch(RulePredicates.isAccountMatch())) {
            log.info("Filtered result contains an account match. Retaining only account match rules as it takes highest priority");
            return (List)matchingRules.stream().filter(RulePredicates.isAccountMatch()).collect(Collectors.toList());
        } else {
            return matchingRules;
        }
    }

    private List<MatrixRule> fetchFeatureIdMatch(List<MatrixRule> matrixRlz, String featureId) {
        return (List)matrixRlz.stream().filter(RulePredicates.isFeatureIdMatch(featureId)).collect(Collectors.toList());
    }

    private List<MatrixRule> fetchExactAccountorCompanyMatchAndResourceMatchByHierarchy(List<MatrixRule> matrixRlz) {
        List<MatrixRule> matchingRules = (List)matrixRlz.stream().filter(RulePredicates.isAccountMatch().or(RulePredicates.isCompanyMatch())).collect(Collectors.toList());
        matchingRules = this.eliminateCompanyMatchesIfAccountMatchFound(matchingRules);
        Optional<MatrixRule> omr = matchingRules.stream().min((r1, r2) -> {
            return Integer.valueOf(r1.getResourceHierarchy()).compareTo(r2.getResourceHierarchy());
        });
        if (omr.isPresent()) {
            matchingRules = (List)matchingRules.stream().filter(RulePredicates.isResourceMatch(omr)).collect(Collectors.toList());
        }

        return matchingRules;
    }

    private List<MatrixRule> fetchRulesByAmountMatch(List<MatrixRule> matrixRlz, TransactionDetails trxDetails) {
        String trxId = trxDetails.getTrxId();
        String trxCurrency = trxDetails.getTrxCcy();
        BigDecimal trxAmount = trxDetails.getTrxAmount();
        String resourceId = trxDetails.getResourceId();
        String bankEntityId = trxDetails.getBankEntityId();
        return (List)matrixRlz.stream().filter((rule) -> {
            if (rule.getApprovalAmountCCY().equals(trxCurrency) && rule.getApprovalMinAmount().compareTo(trxAmount) < 1 && rule.getApprovalMaxAmount().compareTo(trxAmount) >= 0) {
                return true;
            } else if (!rule.getApprovalAmountCCY().equals(trxCurrency)) {
                CcyAndAmtsToCompare ccyAndAmtsToCompare = new CcyAndAmtsToCompare();
                ccyAndAmtsToCompare.setAmount(trxAmount);
                ccyAndAmtsToCompare.setAmountCurrencyCode(trxCurrency);
                ccyAndAmtsToCompare.setCompareToCurrencyCode(rule.getApprovalAmountCCY());
                ccyAndAmtsToCompare.setCompareToAmount(rule.getApprovalMinAmount());
                ccyAndAmtsToCompare.setCompareToCurrencyCode2(rule.getApprovalAmountCCY());
                ccyAndAmtsToCompare.setCompareToAmount2(rule.getApprovalMaxAmount());
                DerivedCurrencyAndAmount derivedCcyAndAmount = this.fxRateService.convert(resourceId, bankEntityId, ccyAndAmtsToCompare);
                log.info("Trx Id {}, Trx Ccy {}, Trx Amount {}, Rule Id {}, Rule Ccy {},  Rule Min Amount {}, Rule Max Amount {}, Derived Trx CCy {}, Derived Trx Amount {}, Derived Rule Ccy {}, Derived Rule Min Amount {}, Derived Rule Max Amount {}", new Object[]{trxId, trxCurrency, trxAmount, rule.getRuleId(), rule.getApprovalAmountCCY(), rule.getApprovalMinAmount(), rule.getApprovalMaxAmount(), derivedCcyAndAmount.getCurrency(), derivedCcyAndAmount.getAmount(), derivedCcyAndAmount.getCompareToCurrency(), derivedCcyAndAmount.getCompareToAmount(), derivedCcyAndAmount.getCompareToAmount2()});
                if (derivedCcyAndAmount.getAmount().compareTo(derivedCcyAndAmount.getCompareToAmount()) >= 0 && derivedCcyAndAmount.getAmount().compareTo(derivedCcyAndAmount.getCompareToAmount2()) <= 0) {
                    log.info("Amount is in the range. Rule Id {} amounts matched amount for Trx with Trx Id {}", rule.getRuleId(), trxId);
                    return true;
                } else {
                    log.info("Amount is not in the range. Rule Id {} amounts did not match amount for Trx with Trx Id {}", rule.getRuleId(), trxId);
                    return false;
                }
            } else {
                return false;
            }
        }).collect(Collectors.toList());
    }

    public MatrixProcessingResult evaluateAuthMatrixForTransactionEvent(TransactionEvent transactionEvent, MatrixContext matrixContext) {
        matrixContext.setTrxEventActions(this.trxEventToResourceActionMapper.fetchTransactionEventToResourceActionMap());
        matrixContext.setNodeActions(this.nodeToResourceActionMapper.fetchNodeToResourceActionMap());
        matrixContext.getAdditionalContextData().put("TRX_EVENT", transactionEvent);
        Map<Integer, List<List<Node>>> rulesMap = this.getAuthMatrixRules(matrixContext, true).getRulesMap();
        if (this.authMatrixConfig.isLimitsCheckEnabled()) {
            this.checkDailyLimitForUser(matrixContext);
        } else {
            log.warn("WARNING: DAILY LIMIT CHECK IS BEING BYPASSED. PLEASE CHECK IF YOUR CONFIGURATION BEFORE TAKING THIS CHANGE TO TESTING/LIVE");
        }

        return this.authMatrixGraph.processTransactionEvent(transactionEvent, matrixContext, rulesMap);
    }

    public MatrixProcessingResult checkAuthMatrixForAllowedActions(MatrixContext matrixContext) {
        matrixContext.setTrxEventActions(this.trxEventToResourceActionMapper.fetchTransactionEventToResourceActionMap());
        matrixContext.setNodeActions(this.nodeToResourceActionMapper.fetchNodeToResourceActionMap());
        log.info("Fetching the latest transaction details for the transactionid {}", matrixContext.getTrxDetails().getTrxId());
        TransactionDetails trxDetails = this.transactions.fetchTransactionDetails(matrixContext.getTrxDetails());
        trxDetails = this.invalidateAuthorizersIfRequired(trxDetails, matrixContext);
        Map<Integer, List<List<Node>>> rulesMap = this.getAuthMatrixRules(matrixContext, true, trxDetails).getRulesMap();
        matrixContext.getAdditionalContextData().put("CHK_AUTH_MATRIX", "Y");
        if (this.authMatrixConfig.isLimitsCheckEnabled()) {
            this.checkDailyLimitForUser(matrixContext);
        } else {
            log.warn("WARNING: DAILY LIMIT CHECK IS BEING BYPASSED. PLEASE CHECK IF YOUR CONFIGURATION BEFORE TAKING THIS CHANGE TO TESTING/LIVE");
        }

        MatrixProcessingResult result = this.authMatrixGraph.processTransaction(matrixContext, rulesMap);
        if (null == result.getMatrixContext().getAdditionalContextData().get("IS_LAST_AUTHORIZER")) {
            result.getMatrixContext().getAdditionalContextData().put("IS_LAST_AUTHORIZER", "N");
        }

        return result;
    }

    private TransactionDetails invalidateAuthorizersIfRequired(TransactionDetails trxDetails, MatrixContext matrixContext) {
        if (this.preCheckInvalidation(trxDetails) != null) {
            return trxDetails;
        } else {
            long companyId = Long.parseLong(trxDetails.getCompanyId());
            long companyGroupId = Long.parseLong(trxDetails.getCompanyGroupId());
            String bankEntityId = trxDetails.getBankEntityId();
            String externalIdType = "GEB";
            List<Long> userIds = new ArrayList();
            trxDetails.getAuthorizations().stream().filter((a) -> {
                return a.getAuthorizerId() > 0L;
            }).forEach((a) -> {
                userIds.add(a.getAuthorizerId());
            });
            if (CollectionUtils.isEmpty(userIds)) {
                log.info("No valid authorizers have approved this transaction yet");
                return trxDetails;
            } else {
                List<ExternalUserInfo> externalUserInfo = this.fetchExternalUserInfo(companyId, companyGroupId, bankEntityId, externalIdType, userIds);
                if (CollectionUtils.isEmpty(externalUserInfo)) {
                    return trxDetails;
                } else {
                    List<TransactionAuthorizations> invalidatedAuthorizations = new ArrayList();
                    Map<Long, List<String>> invalidAuthzDataPositions = new HashMap();
                    trxDetails.getAuthorizations().stream().filter((a) -> {
                        return a.getAuthorizerId() > 0L;
                    }).forEach((a) -> {
                        Optional<ExternalUserInfo> extUsrInfo = externalUserInfo.stream().filter((e) -> {
                            return e.getUserId() == a.getAuthorizerId() && e.getCompanyId() == companyId && e.getGroupId() == companyGroupId;
                        }).findFirst();
                        if (extUsrInfo.isPresent() && ((ExternalUserInfo)extUsrInfo.get()).getActive().equals("A") && ((ExternalUserInfo)extUsrInfo.get()).getAuthLevel().equals(a.getAuthorizerGroup())) {
                            log.info("There is no change for the user permissions or active status {} ", externalUserInfo);
                        } else if (!extUsrInfo.isPresent() || ((ExternalUserInfo)extUsrInfo.get()).getActive().equals("A") && ((ExternalUserInfo)extUsrInfo.get()).getAuthLevel().equals(a.getAuthorizerGroup())) {
                            log.warn("Failed to find a matching record for the user who has authorized earlier in external system GEB. Authorization details {}", a);
                        } else {
                            log.info("There is a change for the user permissions or active status User details {}. Authorization Details that will be invalidated are {} ", externalUserInfo, a);
                            invalidatedAuthorizations.add(a);
                            this.prepareInvalidAuthorizationData(invalidAuthzDataPositions, a.getAuthorizationRuleId(), a.getAuthorizerGroup(), a.getAuthorizerId());
                        }

                    });
                    int invalidationCount = 0;
                    invalidationCount = this.invalidateEntries(matrixContext, invalidatedAuthorizations, invalidAuthzDataPositions, invalidationCount);
                    return invalidationCount > 0 ? this.transactions.fetchTransactionDetails(matrixContext.getTrxDetails()) : trxDetails;
                }
            }
        }
    }

    private int invalidateEntries(MatrixContext matrixContext, List<TransactionAuthorizations> invalidatedAuthorizations, Map<Long, List<String>> invalidAuthzDataPositions, int invalidationCount) {
        if (!CollectionUtils.isEmpty(invalidatedAuthorizations)) {
            log.info("These authorizations {}  will be invalidated", invalidatedAuthorizations);
            List<Long> invalidAuthIds = (List)invalidatedAuthorizations.stream().map(TransactionAuthorizations::getTransactionAuthorizationId).collect(Collectors.toList());
            invalidationCount = this.transactions.invalidateTransactionAuthorization(invalidAuthIds);
            log.info("No of records affected by the invalidation are {}", invalidationCount);
            if (invalidationCount > 0) {
                invalidationCount = this.transactions.resetTransactionAuthorization(invalidAuthIds);
                log.info("No of records affected by the reset due to invalidations are {}", invalidationCount);
            }

            if (invalidationCount > 0) {
                log.info("Invalidation of authorizations is done");
                matrixContext.getAdditionalContextData().put("INVALIDATED_AUTHZ", invalidAuthzDataPositions);
            }
        }

        return invalidationCount;
    }

    private List<ExternalUserInfo> fetchExternalUserInfo(long companyId, long companyGroupId, String bankEntityId, String externalIdType, List<Long> userIds) {
        log.info("Fetching the current status of the users ");
        List<ExternalUserInfo> externalUserInfo = this.matrixRules.getExternalUserDetailsForRuleValidation(userIds, companyId, companyGroupId, externalIdType, externalIdType, bankEntityId);
        if (CollectionUtils.isEmpty(externalUserInfo)) {
            log.warn("Unable to find the users in the external system (GEB).");
        }

        return externalUserInfo;
    }

    private TransactionDetails preCheckInvalidation(TransactionDetails trxDetails) {
        log.info("Performing checks to invalidate any authorizations if required");
        if (!CollectionUtils.isEmpty(trxDetails.getAuthorizations()) || trxDetails.getTransactionStatus().getAuthorizationStatus() != null && trxDetails.getTransactionStatus().getAuthorizationStatus().equals("PENDING_AUTHORIZATION")) {
            return null;
        } else {
            log.info("Conditions matched to skip authorization invalidation checks");
            return trxDetails;
        }
    }

    private void prepareInvalidAuthorizationData(Map<Long, List<String>> invalidAuthzDataPositions, long ruleId, String invalidatedGroup, long invalidatedId) {
        List<String> invalidatedGroupsAndIds = null;
        if (invalidAuthzDataPositions.containsKey(ruleId)) {
            invalidatedGroupsAndIds = (List)invalidAuthzDataPositions.get(ruleId);
        } else {
            invalidatedGroupsAndIds = new ArrayList();
        }

        ((List)invalidatedGroupsAndIds).add(invalidatedGroup + invalidatedId);
        invalidAuthzDataPositions.put(ruleId, invalidatedGroupsAndIds);
    }

    private void checkDailyLimitForUser(MatrixContext matrixContext) {
        TransactionDetails trxDetails = matrixContext.getTrxDetails();
        if (!"PENDING_SEND".equals(trxDetails.getTransactionStatus().getAuthorizationStatus())) {
            String actionForTrxEvent = (String)matrixContext.getTrxEventActions().get(TransactionEvent.AUTHORIZED);
            String trxResourceId = matrixContext.getTrxDetails().getResourceId();
            List<Role> roles = matrixContext.getUserCompanyPermissions().getRoles();
            boolean hasPermission = roles.stream().anyMatch((r) -> {
                return r.getPermittedResourceActions().stream().anyMatch((resActn) -> {
                    return resActn.getResourceId().equals(trxResourceId) && resActn.getResourceActions().stream().anyMatch((ra) -> {
                        return ra.getActn().equals(actionForTrxEvent);
                    });
                });
            });
            if (hasPermission) {
                DailyLimitService dailyLimitService = this.dailyLimitLoader.loadDailyLimitImplementation();
                CheckLimitRequest limitsRequest = new CheckLimitRequest();
                limitsRequest.setEntityId(matrixContext.getUserCompanyPermissions().getEntity().getAlternateEntityId());
                limitsRequest.setUserId(String.valueOf(matrixContext.getUserCompanyPermissions().getAlternateUserId()));
                limitsRequest.setCountry(matrixContext.getProcessingForCountry());
                if (matrixContext.isBulkProcessing()) {
                    limitsRequest.setTransactionAmount(String.valueOf(matrixContext.getTotalAmount()));
                } else {
                    limitsRequest.setTransactionAmount(String.valueOf(trxDetails.getTrxAmount()));
                }

                limitsRequest.setTransactionCurrency(trxDetails.getTrxCcy());
                limitsRequest.setBankAbbvName(trxDetails.getBankEntityId());
                if (this.authMatrixConfig.getConvertDailyLimitCcys().contains(limitsRequest.getTransactionCurrency())) {
                    FXRateBaseCurrency fxRateBaseCurrency = this.fxRateService.convertToBaseCurrency(matrixContext.getTrxDetails().getResourceId(), matrixContext.getTrxDetails().getBankEntityId(), new BigDecimal(limitsRequest.getTransactionAmount()), limitsRequest.getTransactionCurrency());
                    limitsRequest.setTransactionAmount(String.valueOf(fxRateBaseCurrency.getConvertedBaseCCYAmount()));
                    limitsRequest.setTransactionCurrency(fxRateBaseCurrency.getBaseCurrency());
                }

                CheckLimitResponse dailyLimitResponse = dailyLimitService.checkLimit(limitsRequest);
                log.info("[Transaction Id:{}]  Check Limit Status {}, Check Limit Message {}, Amount {}, Currency {} ", new Object[]{trxDetails.getTrxId(), dailyLimitResponse.getStatus(), dailyLimitResponse.getMessage(), dailyLimitResponse.getAmount(), dailyLimitResponse.getCurrency()});
                if (dailyLimitResponse.getStatus().equals("SR0001")) {
                    log.info("[Transaction Id:{}]  Limit Check is ok", trxDetails.getTrxId());
                    matrixContext.put("LIMIT_AVLBL", "Y");
                } else {
                    log.info("[Transaction Id:{}]  Limit Check is not ok", trxDetails.getTrxId());
                    matrixContext.put("LIMIT_AVLBL", "N");
                }

            }
        }
    }

    public boolean resetAuthMatrixForTransaction(String trxId, String resetProcessingStatusTo, String changeToken) {
        TransactionDetails trxDetails = this.fetchTrxDetailsWithCTValidation(trxId, changeToken);
        this.restTransactionStateAndAuth(trxId);
        trxDetails.getTransactionStatus().setCaptureStatus(resetProcessingStatusTo);
        trxDetails.setNewChangeToken(String.valueOf(System.currentTimeMillis()));
        int count = this.transactions.resetTransactionStatus(trxDetails);
        if (count == 0) {
            throw new AuthMatrixException("Unable to reset the transaction status for the transaction ".concat(trxDetails.getTrxId()));
        } else {
            return true;
        }
    }

    public boolean resetAuthMatrixForTransaction(String trxId, String changeToken) {
        this.restTransactionStateAndAuth(trxId);
        return true;
    }

    private void restTransactionStateAndAuth(String trxId) {
        TransactionAuthState trxAuthState = new TransactionAuthState();
        trxAuthState.setTrxId(trxId);
        this.transactions.deleteTransactionAuthorizationState(trxAuthState);
        TransactionAuthorizations trxAuthorizations = new TransactionAuthorizations();
        trxAuthorizations.setTrxId(trxId);
        this.transactions.deleteTransactionAuthorization(trxAuthorizations);
    }

    public Map<Integer, TrxAuthorizationStates> getTransactionPossiblePaths(String trxId, String trxCCY, BigDecimal trxAmount, String changeToken) {
        TransactionDetails trxDetails = this.fetchTrxDetailsWithCTValidation(trxId, changeToken);
        trxDetails.setTrxCcy(trxCCY);
        trxDetails.setTrxAmount(trxAmount);
        MatrixContext matrixContext = new MatrixContext();
        matrixContext.setTrxDetails(trxDetails);
        matrixContext.setNodeActions(this.nodeToResourceActionMapper.fetchNodeToResourceActionMap());
        String authzStatus = trxDetails.getTransactionStatus().getAuthorizationStatus();
        Map<Integer, TrxAuthorizationStates> possiblePaths = new HashMap();
        Map<String, Integer> statusChangeOrder = new HashMap();
        statusChangeOrder.put("PENDING_VERIFICATION", 1);
        statusChangeOrder.put("PENDING_AUTHORIZATION", 2);
        statusChangeOrder.put("PENDING_SEND", 3);
        int authOrder = authzStatus != null && statusChangeOrder.containsKey(authzStatus) ? (Integer)statusChangeOrder.get(authzStatus) : 0;
        Map<String, Integer> nodeOrder = new HashMap();
        nodeOrder.put("Verifier", 2);
        nodeOrder.put("ApprovalsCompleted", 3);
        nodeOrder.put("Sender", 4);
        List<String> nonAuthLevelNodes = new ArrayList();
        nonAuthLevelNodes.add("Start");
        nonAuthLevelNodes.add("Completed");
        nonAuthLevelNodes.add("Verifier");
        nonAuthLevelNodes.add("ApprovalsCompleted");
        nonAuthLevelNodes.add("Sender");
        Map<Integer, List<List<Node>>> rulesMap = this.getAuthMatrixRules(matrixContext, true).getRulesMap();
        rulesMap.entrySet().stream().forEach((matrix) -> {
            Integer ruleId = (Integer)matrix.getKey();
            List<List<Node>> matrixNodes = (List)matrix.getValue();
            TrxAuthorizationStates trxAuthorizationStates = new TrxAuthorizationStates();
            matrixNodes.stream().forEach((nodes) -> {
                boolean parallelProcessing = nodes.size() > 1;
                if (parallelProcessing) {
                    trxAuthorizationStates.setParallelProcessing(parallelProcessing);
                }

                nodes.stream().forEach((node) -> {
                    if (node.getState().equals(NodeState.COMPLETED) || !node.getName().equals("Verifier") && !node.getName().equals("ApprovalsCompleted") && !node.getName().equals("Sender")) {
                        if (!node.getState().equals(NodeState.COMPLETED) && !nonAuthLevelNodes.contains(node.getName())) {
                            trxAuthorizationStates.putPendingApproval(node.getName());
                            trxAuthorizationStates.appendApprovalFormattedRepresentation(node.getName(), parallelProcessing);
                        } else if (node.getState().equals(NodeState.COMPLETED) && nonAuthLevelNodes.contains(node.getName()) && nodeOrder.containsKey(node.getName())) {
                            trxAuthorizationStates.setDiscardRule((Integer)nodeOrder.get(node.getName()) > authOrder);
                        }
                    } else {
                        trxAuthorizationStates.putPendingState((String)matrixContext.getNodeActions().get(node.getName()));
                    }

                    possiblePaths.put(ruleId, trxAuthorizationStates);
                });
            });
        });
        return possiblePaths;
    }

    private TransactionDetails fetchTrxDetailsWithCTValidation(String trxId, String changeToken) {
        TransactionDetails trxDetails = new TransactionDetails();
        trxDetails.setTrxId(trxId);
        trxDetails = this.transactions.fetchTransactionDetails(trxDetails);
        if (!changeToken.equals(trxDetails.getChangeToken())) {
            throw new AuthMatrixException("Unable to reset as the change token is different. The record may have been updated by another transaction");
        } else {
            return trxDetails;
        }
    }
}

```
