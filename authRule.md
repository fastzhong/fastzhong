```sq
SELECT * FROM (
		    SELECT DISTINCT
    ar.authorization_rule_id  authorizationruleid,
    ar.relationship_type,
    c.company_id               companyid,
    c.group_id                 groupid,
    gcm.entity_name || ' ' || gcm.second_entity_name companyname,
    r.resource_id resourceid,
    rf.feature_id featureId,
    a.account_number           accountno,
    a.account_name,
    a.account_id,
    a.account_currency,
    verifier_required          verify,
    (
        SELECT
            LISTAGG(rg.authorization_group, ',') WITHIN GROUP(
                ORDER BY
                    rg.authorization_group
            ) AS levels
        FROM
            aes_company_resource_authorization_groups rg
        WHERE
            rg.authorization_rule_id = ar.authorization_rule_id
    ) levels,
    sender_required            send,
    authorization_currency     ccy,
    authorization_order        seq,
    minimum_amount             minimumtransactionamount,
    maximum_amount             maximumtransactionamount,
    ar.approved_date_time      lastacteddate,
    ar.approved_by             lastactedby,
    ar.change_token            changetoken,
    'APPROVED' status,
    nvl2(ar.updated_date_time, 'UPDATE', 'ADD') useraction,
     (
        SELECT
            LISTAGG(feature_id, '_') WITHIN GROUP(
                ORDER BY
                    feature_id
            )
        FROM
            (
                SELECT DISTINCT
                    rf.feature_id
                FROM
                    aes_company_resource_authorization_features_rules rf
                WHERE
                    rf.authorization_rule_id = ar.authorization_rule_id
            )
    ) featuregroup
FROM
    aes_company_resource_authorization_rules   ar
    JOIN aes_resources                              r ON ar.resource_id = r.resource_id
    LEFT JOIN aes_account_master                         a ON ( ar.relationship_type = 'ACCOUNT'
                                        AND TO_CHAR(ar.relationship_id) = a.account_number )
    LEFT JOIN aes_company_master                         c ON ( ar.relationship_type = 'COMPANY'
                                        AND ar.relationship_id = c.company_id )
                                      OR ( ar.relationship_type = 'ACCOUNT'
                                           AND a.company_id = c.company_id )
                                      OR ( ar.relationship_type = 'COMPANY_GROUP'
                                           AND ar.relationship_id = c.group_id )
    LEFT JOIN aes_company_group_master                   cg ON ( ar.relationship_type = 'COMPANY_GROUP'
                                               AND ar.relationship_id = cg.group_id )
                                             OR ( ar.relationship_type = 'COMPANY'
                                                  AND cg.group_id = c.group_id )
                                             OR ( ar.relationship_type = 'ACCOUNT'
                                                  AND cg.group_id = a.group_id )
    LEFT JOIN aes_company_resource_authorization_features_rules rf ON rf.authorization_rule_id = ar.authorization_rule_id
    LEFT JOIN geb_company_attribute_mvr gcm on  c.company_id = gcm.company_id
WHERE
    ar.authorization_rule_id NOT IN (
        SELECT
            p.authorization_rule_id
        FROM
            aes_company_resource_authorization_rules_pend p
        WHERE
            p.status != 'CANCELLED'
    )
    AND ar.bank_entity_id = <@p name='listAuthMatrixReqDetails.bankEntityId'/>
			and c.group_id = <@p name='listAuthMatrixReqDetails.groupId'/>
			<#if listAuthMatrixReqDetails.companyName ?? && (listAuthMatrixReqDetails.companyName) ? size gt 0>
			and ( 
			 gcm.entity_name || ' ' || gcm.second_entity_name in(<#list listAuthMatrixReqDetails.companyName as companyName> <@p value=companyName/> <#if companyName_has_next>,</#if> </#list> )
			or gcm.entity_name  in(<#list listAuthMatrixReqDetails.companyName as companyName> <@p value=companyName/> <#if companyName_has_next>,</#if> </#list> )
			)
			</#if>
			<#if listAuthMatrixReqDetails.verifierRequired ?? && (listAuthMatrixReqDetails.verifierRequired) ? size gt 0>
			and ar.VERIFIER_REQUIRED in (<#list listAuthMatrixReqDetails.verifierRequired as verifierRequired> <@p value=verifierRequired/> <#if verifierRequired_has_next>,</#if> </#list> )
			</#if>
			<#if listAuthMatrixReqDetails.limitAmount ??>
				<#if listAuthMatrixReqDetails.limitAmount.minTransactionAmount ??>
					and ar.minimum_amount >= <@p name='listAuthMatrixReqDetails.limitAmount.minTransactionAmount'/>
				</#if>
				<#if listAuthMatrixReqDetails.limitAmount.maxTransactionAmount ??>
					and ar.maximum_amount <=  <@p name='listAuthMatrixReqDetails.limitAmount.maxTransactionAmount'/>
				</#if>
			</#if>
			<#if listAuthMatrixReqDetails.senderRequired ?? && (listAuthMatrixReqDetails.senderRequired) ? size gt 0>
			and ar.sender_required in (<#list listAuthMatrixReqDetails.senderRequired as senderRequired> <@p value=senderRequired/> <#if senderRequired_has_next>,</#if> </#list> )
		 	</#if>
		   <#if listAuthMatrixReqDetails.accountNumber ?? && (listAuthMatrixReqDetails.accountNumber) ? size gt 0>
		    and a.account_number in (<#list listAuthMatrixReqDetails.accountNumber as accountNumber> <@p value=accountNumber/> <#if accountNumber_has_next>,</#if> </#list> )
		    </#if>
		    ORDER BY ${sortFieldWithDirection})
		    <#if resourceFeatureList?size gt 0>
			WHERE (resourceid || '_' || featuregroup) in(<#list resourceFeatureList as resourceFeatureList> <@p value=resourceFeatureList/> <#if resourceFeatureList_has_next>,</#if> </#list> )
			</#if>
		 
```
