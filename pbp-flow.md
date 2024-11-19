# flow builder
THISE0511202492_Auth_CU10.json
THISE0511202492_Auth_CU10.xml.done


    
2024-11-19 21:17:07.287 DEBUG [main] o.a.c.i.e.InternalRouteStartupManager - Route: CUSTOMER_SUBMITTED_TRANSFORMED >>> Route[file://c:/CEW/tmp/dmp/incoming?antExclude=&antInclude=*_Auth_*.json&charset=utf-8&delay=6000&doneFileName=name.noext.xml.done&maxMessagesPerPoll=1&move=c%3A%2FCEW%2Ftmp%2Fdmp%2Fbackup&moveFailed=c%3A%2FCEW%2Ftmp%2Fdmp%2Ferror&noop=false&readLock=rename&readLockCheckInterval=0&readLockLoggingLevel=WARN&readLockTimeout=60000&recursive=false&sortBy=file%3Amodified -> null]
2024-11-19 21:17:07.287 DEBUG [main] o.a.c.i.e.InternalRouteStartupManager - Starting consumer (order: 1002) on route: CUSTOMER_SUBMITTED_TRANSFORMED
2024-11-19 21:17:07.289 DEBUG [main] o.a.camel.support.DefaultConsumer - Build consumer: Consumer[file://c:/CEW/tmp/dmp/incoming?antExclude=&antInclude=*_Auth_*.json&charset=utf-8&delay=6000&doneFileName=name.noext.xml.done&maxMessagesPerPoll=1&move=c%3A%2FCEW%2Ftmp%2Fdmp%2Fbackup&moveFailed=c%3A%2FCEW%2Ftmp%2Fdmp%2Ferror&noop=false&readLock=rename&readLockCheckInterval=0&readLockLoggingLevel=WARN&readLockTimeout=60000&recursive=false&sortBy=file%3Amodified]
2024-11-19 21:17:07.290 DEBUG [main] o.a.camel.support.DefaultConsumer - Init consumer: Consumer[file://c:/CEW/tmp/dmp/incoming?antExclude=&antInclude=*_Auth_*.json&charset=utf-8&delay=6000&doneFileName=name.noext.xml.done&maxMessagesPerPoll=1&move=c%3A%2FCEW%2Ftmp%2Fdmp%2Fbackup&moveFailed=c%3A%2FCEW%2Ftmp%2Fdmp%2Ferror&noop=false&readLock=rename&readLockCheckInterval=0&readLockLoggingLevel=WARN&readLockTimeout=60000&recursive=false&sortBy=file%3Amodified]
2024-11-19 21:17:07.291 DEBUG [main] o.a.camel.support.DefaultConsumer - Starting consumer: Consumer[file://c:/CEW/tmp/dmp/incoming?antExclude=&antInclude=*_Auth_*.json&charset=utf-8&delay=6000&doneFileName=name.noext.xml.done&maxMessagesPerPoll=1&move=c%3A%2FCEW%2Ftmp%2Fdmp%2Fbackup&moveFailed=c%3A%2FCEW%2Ftmp%2Fdmp%2Ferror&noop=false&readLock=rename&readLockCheckInterval=0&readLockLoggingLevel=WARN&readLockTimeout=60000&recursive=false&sortBy=file%3Amodified]
2024-11-19 21:17:07.294 DEBUG [main] o.a.c.i.e.BaseExecutorServiceManager - Created new ScheduledThreadPool for source: Consumer[file://c:/CEW/tmp/dmp/incoming?antExclude=&antInclude=*_Auth_*.json&charset=utf-8&delay=6000&doneFileName=name.noext.xml.done&maxMessagesPerPoll=1&move=c%3A%2FCEW%2Ftmp%2Fdmp%2Fbackup&moveFailed=c%3A%2FCEW%2Ftmp%2Fdmp%2Ferror&noop=false&readLock=rename&readLockCheckInterval=0&readLockLoggingLevel=WARN&readLockTimeout=60000&recursive=false&sortBy=file%3Amodified] with name: file://c:/CEW/tmp/dmp/incoming?antExclude=&antInclude=*_Auth_*.json&charset=utf-8&delay=6000&doneFileName=name.noext.xml.done&maxMessagesPerPoll=1&move=c%3A%2FCEW%2Ftmp%2Fdmp%2Fbackup&moveFailed=c%3A%2FCEW%2Ftmp%2Fdmp%2Ferror&noop=false&readLock=rename&readLockCheckInterval=0&readLockLoggingLevel=WARN&readLockTimeout=60000&recursive=false&sortBy=file%3Amodified -> org.apache.camel.util.concurrent.SizedScheduledExecutorService@587c276c[file://c:/CEW/tmp/dmp/incoming?antExclude=&antInclude=*_Auth_*.json&charset=utf-8&delay=6000&doneFileName=name.noext.xml.done&maxMessagesPerPoll=1&move=c%3A%2FCEW%2Ftmp%2Fdmp%2Fbackup&moveFailed=c%3A%2FCEW%2Ftmp%2Fdmp%2Ferror&noop=false&readLock=rename&readLockCheckInterval=0&readLockLoggingLevel=WARN&readLockTimeout=60000&recursive=false&sortBy=file%3Amodified]
2024-11-19 21:17:07.295 DEBUG [main] o.a.c.s.DefaultScheduledPollConsumerScheduler - Scheduling 1 consumers poll (fixed delay) with initialDelay: 1000, delay: 6000 (milliseconds) for: file://c:/CEW/tmp/dmp/incoming?antExclude=&antInclude=*_Auth_*.json&charset=utf-8&delay=6000&doneFileName=name.noext.xml.done&maxMessagesPerPoll=1&move=c%3A%2FCEW%2Ftmp%2Fdmp%2Fbackup&moveFailed=c%3A%2FCEW%2Ftmp%2Fdmp%2Ferror&noop=false&readLock=rename&readLockCheckInterval=0&readLockLoggingLevel=WARN&readLockTimeout=60000&recursive=false&sortBy=file%3Amodified
2024-11-19 21:17:07.296 DEBUG [main] o.a.c.i.e.InternalRouteStartupManager - Route: CUSTOMER_SUBMITTED_TRANSFORMED started and consuming from: file://c:/CEW/tmp/dmp/incoming


```yml
bulk-processing:
  bulk-routes:
    - route-name: CUSTOMER_SUBMITTED_TRANSFORMED
      processing-type: INBOUND
      source-type: FILE
      bank-entity: UOBT
      channel: IDB
      request-type: BulkUpload
      enabled: true
      steps:
        - pain001-processing
        - payment-debulk
        - payment-validation
        - payment-enrichment
        - payment-save
      file-source:
        directoryName: c:/CEW/tmp/dmp/incoming
        antInclude: "*_Auth_*.json"
        antExclude:
        charset: utf-8
        doneFileName: "${file:name.noext}.xml.done"
        delay: 6000
        sortBy: file:modified
        maxMessagesPerPoll: 1
        noop: false
        recursive: false
        move: c:/CEW/tmp/dmp/backup
        moveFailed: c:/CEW/tmp/dmp/error
        readLock: rename
        readLockTimeout: 60000
        readLockInterval: 1000
        readLockLoggingLevel: WARN
    - route-name: CUSTOMER_AUTHORIZED
      processing-type: OUTBOUND
      destination-type: API
      bank-entity: UOBT
      channel: IDB
      enabled: false
      steps:
        - payment-load
        - pain001-transform
      api-source:
        http-uri: /path/to/api
      file-destination:
        directoryName: /path/to/outbound/processing
        fileName: "${header.CamelFileName}"
        tempFileName: "${file:name.noext}.tmp"
        doneFileName: "${file:name.noext}.xml.done"
        autoCreate: true
        fileExist: Override
        moveExisting:
        eagerDeleteTargetFile: false
        delete: true
        chmod: rw-r--r--
```
