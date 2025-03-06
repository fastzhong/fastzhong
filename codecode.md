```java
public ReportHandler createReportHandler(ServiceModel serviceModel) {
        if (serviceModel instanceof OnlineReportReq) {
            OnlineReportReq onlineReportReq = (OnlineReportReq) serviceModel;

            // Advice
            if (IS_CREDIT_ADVICE_ONLINE.test(onlineReportReq) || IS_DEBIT_ADVICE_ONLINE.test(onlineReportReq)) {
                return adviceOnlineHandler;
            }

            // Auth Setup
            if (IS_AUTH_SETUP_ONLINE.test(onlineReportReq)) {
                return authSetupOnlineHandler;
            }

            // Manage PayerPayee
            if (IS_MANAGE_PAYERPAYEE_ONLINE.test(onlineReportReq)) {
                return managePayerPayeeOnlineHandler;
            }

            // txn
            Boolean isSingleChild = IS_WITH_SINGLE_CHILD.test(onlineReportReq);
            Boolean isBulkOnline = ONLINE_IS_BULK.test(onlineReportReq);
            if (!isSingleChild) {
                return singleOnlineReportHandler;
            } else if (isBulkOnline) {
                return bulkOnlineReportHandler;
            } else {
                // generate one child of bulk online report
                return bulkChildOnlineReportHandler;
            }

        } else if (serviceModel instanceof OfflineReportReq) {
            OfflineReportReq offlineReportReq = (OfflineReportReq) serviceModel;

            // Advice
            if (IS_CREDIT_ADVICE_OFFLINE.test(offlineReportReq) || IS_DEBIT_ADVICE_OFFLINE.test(offlineReportReq)) {
                return adviceOfflineHandler;
            }

            // Auth Setup
            if (IS_AUTH_SETUP_OFFLINE.test(offlineReportReq)) {
                return authSetupOfflineHandler;
            }

            // Manage PayerPayee
            if (IS_MANAGE_PAYERPAYEE_OFFLINE.test(offlineReportReq)) {
                return managePayerPayeeOfflineHandler;
            }

            // txn
            if (OFFLINE_BULK.test(offlineReportReq)) {
                return bulkOfflineReportHandler;
            } else {
                return singleOfflineReportHandler;
            }
        } else {
            return null;
        }
    }
```
