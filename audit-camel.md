```xml
    <select id="getAuditByMQ" statementType="PREPARED" 
        parameterType="com.uob.gwb.aqs.domain.AuditMessage" resultMap="AuditEventsInfoMap">
    <![CDATA[select
         ac.audit_event_code,
         ac.audit_category_code,
         ac.audit_tab_code,
         arc.audit_result_code
         from aqs_audit_codes ac
         left join aqs_audit_result_code arc on ac.audit_type_id=arc.audit_type_id
         where
            arc.audit_result_code = <@p name='resultCode'/>
         <#if auditMessage.auditKey??>
         and ac.audit_key = <@p name='auditMessage.auditKey'/>
         <#else>
         and ac.RESOURCE_ID = <@p name='auditMessage.resourceId'/>
         and ac.FEATURE_ID = <@p name='auditMessage.featureId'/>
         </#if>
         <#if auditMessage.endpoint??>
         and ac.API_ENDPOINT = <@p name='auditMessage.endpoint'/>
         </#if>
         <#if auditMessage.microService??>
         and ac.MICROSERVICE_NAME = <@p name='auditMessage.microService'/>
         </#if>
    ]]>
</select>

    <resultMap id="AuditEventsInfoMap"
               type="com.uob.gwb.aqs.domain.AuditCodes">
        <result column="audit_event_code" property="eventCode" />
        <result column="audit_category_code" property="categoryCode" />
        <result column="audit_tab_code" property="tabCode" />
        <result column="audit_result_code" property="resultCode" />
        <result column="AUDIT_KEY" property="auditActionKey" />
    </resultMap>
```

```xml

<routes>
    <route id="initiation-audit-query-route" autoStartup="{{ufw.app.qfeature.audit-query-async-event.start-audit-queue}}"><description>Route to send message from Audit Query</description>
        <from uri="ufwjms{{ufw.app.qfeature.audit-query-async-event.receive-host-id}}:{{ufw.app.qfeature.audit-query-async-event.receive-queue}}?testConnectionOnStartup=true"/>
        <log loggingLevel="INFO" message="AuditService In flight Message Received ==>>>\n ${headers} \n ${body}"/>
        <to uri="bean:AuditServiceAsyncProcessor?method=processJson"/>
        <log loggingLevel="INFO" message="After unmarshalling, Mandate Initiation Message Object ==>>>\n ${body}"/>
    </route>
<routes>
```

```yml
camel:
  springboot:
    use-mdc-logging: true
    name: camel-auditqueryservice
    routes-include-pattern: classpath:/spring/core/routes/ueqs-routes.xml,/spring/core/routes/common-routes.xml,/spring/core/routes/rds-routes.xml,/spring/core/routes/bms-routes.xml,/spring/core/routes/audit-query-routes.xml
```

```java
@RequiredArgsConstructor
@Slf4j
@Component("AuditServiceAsyncProcessor")
public class AuditServiceAsyncProcessor {

    public void processJson(String message) {
        log.info("AuditServiceAsyncProcessor  - START {}", message);
        try {
            JsonNode inAuditJsonNode = JsonUtil.fromJsonString(JsonNode.class, message);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            AuditMessage auditMessage = objectMapper.treeToValue(inAuditJsonNode, AuditMessage.class);
            log.info("", auditMessage);
        } catch (Exception e) {
            log.error("Transactions for audit Async Failed ", e.getMessage(), e);
        }
        log.info("AuditServiceAsyncProcessor audit - END");
    }
}
```

Caused by: org.apache.camel.NoSuchEndpointException: No endpoint could be found for: ufwjmsMLTSG82B.sg.uobnet.com://UOB.CEW.DEV.THAUDITINTERNALQ?testConnectionOnStartup=true, please check your classpath contains the needed Camel component jar.

