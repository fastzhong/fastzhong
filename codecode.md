```java
public ReportHandler createReportHandler(ServiceModel serviceModel) {
    if (serviceModel instanceof OnlineReportReq) {
        return handleOnlineReport((OnlineReportReq) serviceModel);
    } else if (serviceModel instanceof OfflineReportReq) {
        return handleOfflineReport((OfflineReportReq) serviceModel);
    }
    return null;
}

private ReportHandler handleOnlineReport(OnlineReportReq request) {
    if (IS_CREDIT_ADVICE_ONLINE.test(request) || IS_DEBIT_ADVICE_ONLINE.test(request)) {
        return adviceOnlineHandler;
    }
    if (IS_AUTH_SETUP_ONLINE.test(request)) {
        return authSetupOnlineHandler;
    }
    if (IS_MANAGE_PAYERPAYEE_ONLINE.test(request)) {
        return managePayerPayeeOnlineHandler;
    }
    return getOnlineTransactionHandler(request);
}

private ReportHandler getOnlineTransactionHandler(OnlineReportReq request) {
    if (!IS_WITH_SINGLE_CHILD.test(request)) {
        return singleOnlineReportHandler;
    }
    return ONLINE_IS_BULK.test(request) ? bulkOnlineReportHandler : bulkChildOnlineReportHandler;
}

private ReportHandler handleOfflineReport(OfflineReportReq request) {
    if (IS_CREDIT_ADVICE_OFFLINE.test(request) || IS_DEBIT_ADVICE_OFFLINE.test(request)) {
        return adviceOfflineHandler;
    }
    if (IS_AUTH_SETUP_OFFLINE.test(request)) {
        return authSetupOfflineHandler;
    }
    if (IS_MANAGE_PAYERPAYEE_OFFLINE.test(request)) {
        return managePayerPayeeOfflineHandler;
    }
    return OFFLINE_BULK.test(request) ? bulkOfflineReportHandler : singleOfflineReportHandler;
}

```
