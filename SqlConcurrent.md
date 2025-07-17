I am using Java17, Spring Boot 3, MyBatis and Oracle 19. A method below to update pws_transactions table: 

private void updateFinalBulkParentStatus(PwsTransactions pwsTransactions) {
        List<String> childStatusList = getTransactionStatusDao.getChildStatus(pwsTransactions.getTransactionId());
        String finalStatusForParent = getFinalStatusForParent(childStatusList);
        log.info("updateFinalBulkParentStatus, Final Status of parent {}", finalStatusForParent);
        if (Objects.nonNull(finalStatusForParent)) {
            if (finalStatusForParent.equals(REJECTED_STATUS)) {
                pwsTransactions.setCustomerTransactionStatus(REJECTED_STATUS);
                pwsTransactions.setProcessingStatus(REJECTED_STATUS);
                pwsTransactions.setCaptureStatus(REJECTED_STATUS);
                getTransactionStatusDao.updatePWSTransactions(pwsTransactions);
            } else if (finalStatusForParent.equals(SUCCESSFUL_STATUS)) {
                pwsTransactions.setCustomerTransactionStatus(SUCCESSFUL_STATUS);
                pwsTransactions.setProcessingStatus(SUCCESSFUL_STATUS);
                pwsTransactions.setCaptureStatus(SUBMITTED_STATUS);
                getTransactionStatusDao.updatePWSTransactions(pwsTransactions);
            } else if (finalStatusForParent.equals(PARTIAL_REJECTED_STATUS)) {
                pwsTransactions.setCustomerTransactionStatus(PARTIAL_REJECTED_STATUS);
                pwsTransactions.setProcessingStatus(PARTIAL_REJECTED_STATUS);
                pwsTransactions.setCaptureStatus(SUBMITTED_STATUS);
                getTransactionStatusDao.updatePWSTransactions(pwsTransactions);
            }

            NotificationMessage notificationMessage = getNotificationMessageRequestForBulk(pwsTransactions);
            service.publishMessage(notificationMessage);
        } else {
            PwsTransactions pwsTransactionsForIndicatior = new PwsTransactions();
            pwsTransactionsForIndicatior.setReportIndicator(pwsTransactions.getReportIndicator());
            pwsTransactionsForIndicatior.setTransactionId(pwsTransactions.getTransactionId());
            getTransactionStatusDao.updatePWSTransactions(pwsTransactionsForIndicatior);
            log.error("updateFinalBulkParentStatus, no suitable combinations match for parent {} Status {}",
                    pwsTransactions.getTransactionId(), finalStatusForParent);
        }

    }

 Mybatis mapper sql: 
    <select id="updatePWSTransactions"
            parameterType="com.uob.gwb.pis.domain.PwsTransactions" statementType="PREPARED">
        <![CDATA[update pws_transactions pt
                        set
                        <#if PwsTransactions.captureStatus?has_content>
				  			pt.capture_status = <@p name='PwsTransactions.captureStatus'/>,
				  		</#if>
 						<#if PwsTransactions.processingStatus?has_content>
				  			pt.processing_status = <@p name='PwsTransactions.processingStatus'/>,
				  		</#if>
					   <#if PwsTransactions.customerTransactionStatus?has_content>
				  			pt.customer_transaction_status = <@p name='PwsTransactions.customerTransactionStatus'/>,
				  		</#if>
					    <#if PwsTransactions.rejectReason?has_content>
				  			pt.reject_reason = <@p name='PwsTransactions.rejectReason'/>,
				  		</#if>
				  		<#if PwsTransactions.reportIndicator?has_content>
				  			pt.EXECUTION_REPORT_INDICATOR = <@p name='PwsTransactions.reportIndicator'/>,
				  		</#if>
					    pt.processing_status_updated_date = LOCALTIMESTAMP

				  where pt.transaction_id = <@p name='PwsTransactions.transactionId'/>
			]]>

    </select>

    During the performance testing (high concurrency), deadlock detect:
### Error querying database.  Cause: java.sql.SQLException: ORA-00060: deadlock detected while waiting for resource
### The error may exist in URL [jar:nested:/prodlib/CEWMSTH/services/payment-integration-ccti-service-1.0.0.jar/!BOOT-INF/classes/!/templates/db/core/getTransactionStatusDetails.xml]
### The error may involve com.uob.gwb.pis.dao.GetTransactionStatusDao.updatePWSTransactions-Inline
### The error occurred while setting parameters
### Cause: java.sql.SQLException: ORA-00060: deadlock detected while waiting for resource
; ORA-00060: deadlock detected while waiting for resource
	at org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator.doTranslate(SQLErrorCodeSQLExceptionTranslator.java:278)
	at org.springframework.jdbc.support.AbstractFallbackSQLExceptionTranslator.translate(AbstractFallbackSQLExceptionTranslator.java:107)
	at org.mybatis.spring.MyBatisExceptionTranslator.translateExceptionIfPossible(MyBatisExceptionTranslator.java:92)
	at org.mybatis.spring.SqlSessionTemplate$SqlSessionInterceptor.invoke(SqlSessionTemplate.java:439)
	at jdk.proxy2/jdk.proxy2.$Proxy139.selectOne(Unknown Source)
	at org.mybatis.spring.SqlSessionTemplate.selectOne(SqlSessionTemplate.java:160)
	at org.apache.ibatis.binding.MapperMethod.execute(MapperMethod.java:87)
	at org.apache.ibatis.binding.MapperProxy$PlainMethodInvoker.invoke(MapperProxy.java:141)
	at org.apache.ibatis.binding.MapperProxy.invoke(MapperProxy.java:86)
	at jdk.proxy2/jdk.proxy2.$Proxy146.updatePWSTransactions(Unknown Source)
	at jdk.internal.reflect.GeneratedMethodAccessor726.invoke(Unknown Source)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:568)
	at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:354)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:196)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163)
	at org.springframework.dao.support.PersistenceExceptionTranslationInterceptor.invoke(PersistenceExceptionTranslationInterceptor.java:138)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.aop.framework.JdkDynamicAopProxy.invoke(JdkDynamicAopProxy.java:223)
	at jdk.proxy2/jdk.proxy2.$Proxy147.updatePWSTransactions(Unknown Source)
	at com.uob.gwb.pis.service.impl.PaymentStatusServiceImpl.updateFinalBulkParentStatus(PaymentStatusServiceImpl.java:712)
	at com.uob.gwb.pis.service.impl.PaymentStatusServiceImpl.updateBulkTransactionDetails(PaymentStatusServiceImpl.java:626)
	at com.uob.gwb.pis.service.impl.PaymentStatusServiceImpl.updateTransactionStatus(PaymentStatusServiceImpl.java:605)
	at com.uob.gwb.pis.service.impl.PaymentStatusServiceImpl.updateTransaction(PaymentStatusServiceImpl.java:317)
	at com.uob.gwb.pis.service.impl.PaymentStatusServiceImpl.updateSingleAndBulkChildTransactionStatus(PaymentStatusServiceImpl.java:296)
	at com.uob.gwb.pis.service.impl.PaymentStatusServiceImpl.updatePaymentStatus(PaymentStatusServiceImpl.java:137)

Why waiting for resource and why the deadcok occurred while setting sql parameters,  


    
