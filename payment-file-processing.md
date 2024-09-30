```yml
# Assumptions
# Core Payment Model is common for all flows, country
# Customer File Upload Table and NAS design is common for all countries

bulk-file-processors:
  bulk-routes:
    -   route-name: CUSTOMER_SUBMITTED
        processing-type: OUTBOUND
        input-source: DATABASE #NAS can be other alternative
        trigger-type: JMS # File Watcher can be other option
        source-trigger-endpoint: QUEUENAME # QUEUE or Folder
        transformation-required: FALSE
        meta-data-file-required: TRUE #For DMP to use while transforming file
        meta-data-config: MetaData.json #For DMP to use while transforming file
        enabled: false #enable disable job
    -   route-name: CUSTOMER_AUTHORIZED # Post release to bank
        processing-type: OUTBOUND
        input-source: DATABASE
        trigger-type: JMS
        source-trigger-endpoint: QUEUENAME #Message in this queue which triggers Batch flow
        transformation-required: TRUE
        transformation-format-config: UTRI.JSON #Should handle reading from database and writing data to file
        destination-system: ROS #NPP OR REM is another option
        destination-delivery: FILE
        enabled: false #enable disable job
    -   route-name: CUSTOMER_SUBMITTED_TRANSFORMED # Post DMP transformation
        processing-type: INBOUND
        input-source: FILE
        trigger-type: FILE
        source-trigger-endpoint: ./payment_transaction.avro #File to load
        transformation-required: FALSE
        load-to-database: TRUE
        enabled: false #enable disable job
    -   route-name: CUSTOMER_AUTHORIZED_TECH_ACK #L4.1
        processing-type: INBOUND
        input-source: FILE #API is other option
        trigger-type: API
        source-trigger-endpoint: QUEUENAME
        transformation-required: TRUE
        transformation-format-config: UTRO.JSON #Should handle reading from database and writing data to file
        receive: FILE # API is alternative way
        enabled: false #enable disable job
    -   route-name: TT_CUSTOMER_AUTHORIZED_FINAL_ACK #L4
        processing-type: INBOUND
        input-source: FILE #API is other option
        inputFilePath: ./ROS_UTRI.TXT
        trigger-type: FILE # For ROS it's file and for NPP API
        source-trigger-endpoint: QUEUENAME
        transformation-required: TRUE
        transformation-format-config: UTRO.JSON #Should handle reading from database and writing data to file
        receive: FILE # API is alternative way
        enabled: true #enable disable job
    -   route-name: CBP_CUSTOMER_AUTHORIZED_FINAL_ACK #L4
        processing-type: INBOUND
        input-source: FILE #API is other option
        trigger-type: API # For ROS it's file and for NPP API
        source-trigger-endpoint: QUEUENAME
        transformation-required: TRUE
        transformation-format-config: UTRO.JSON #Should handle reading from database and writing data to file
        receive: FILE # API is alternative way
        enabled: false #enable disable job
```
