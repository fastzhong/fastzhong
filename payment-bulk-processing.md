# pom.xml 

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.uob.gwb.pws.pbp</groupId>
    <artifactId>payment-bulk-processing</artifactId>
    <version>1.0-SNAPSHOT</version>
    <name>payments-api</name>
    <packaging>jar</packaging> <!-- Change the packing to  war to run in JBOSS -->
    <description>Payment Bulk Processing</description>
    <parent>
        <groupId>com.uob.ufw</groupId>
        <artifactId>uob-ms-starter</artifactId>
        <version>3.4.0.RELEASE</version>
    </parent>
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compile.source>17</maven.compile.source>
        <maven.compiler.target>17</maven.compiler.target>
        <maven.compiler.plugin.version>3.10.1</maven.compiler.plugin.version>
        <bean-matchers.version>0.14</bean-matchers.version>
        <common-utils.version>2.0.2</common-utils.version>
        <eclipse-java-format.version>4.16.0</eclipse-java-format.version>
        <git-commit-id-maven-plugin.version>6.0.0</git-commit-id-maven-plugin.version>
        <google-java-format.version>1.11.0</google-java-format.version>
        <ibm.mq.client.version>9.3.3.1</ibm.mq.client.version>
        <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
        <ojdbc.version>12.2.0.1</ojdbc.version>
        <sonar.coverage.exclusions>
            **/*Test.java, **/mapper/*.java, **/model/*.java, **/dao/*.java, **/domain/*.java, **/api/*.java,
            **/config/*.java, **/processing/*.java, **/processing/*.java, **/dto/*.java, **/domestic/*.java,
            **/domestic/th/*.java,
            **/bo/*Bo.java, **/model/fx/*.java, **/mapper/core/*.java, **/constant/*.java, **/common/validation/**,
            **/common/po/**
        </sonar.coverage.exclusions>
        <sonar.cpd.exclusions>
            **/config/cbp/*.java, **/config/domestic/*.java, **/dto/*.java, **/dto/domestic/*.java, **/pws/bo/*.java,
            **/aggregation/cbp/*.java, **/domestic/th/*.java, **/aggregation/domestic/*.java
        </sonar.cpd.exclusions>
        <sonar.exclusions>**/api/*.java</sonar.exclusions>
        <spotless.plugin.version>2.26.0</spotless.plugin.version>
        <common-utils.version>2.0.2</common-utils.version>
        <transaction-common.version>6.0.106</transaction-common.version>
    </properties>
    <dependencies>
        <!-- mq -->
        <dependency>
            <groupId>com.ibm.mq</groupId>
            <artifactId>com.ibm.mq.jakarta.client</artifactId>
            <version>${ibm.mq.client.version}</version>
        </dependency>
        <!-- oracle -->
        <dependency>
            <groupId>com.oracle.ojdbc</groupId>
            <artifactId>oraclepki</artifactId>
            <version>19.3.0.0</version>
        </dependency>
        <dependency>
            <groupId>com.oracle.ojdbc</groupId>
            <artifactId>osdt_cert</artifactId>
            <version>19.3.0.0</version>
        </dependency>
        <dependency>
            <groupId>com.oracle.ojdbc</groupId>
            <artifactId>osdt_core</artifactId>
            <version>19.3.0.0</version>
        </dependency>
        <dependency>
            <groupId>com.oracle.jdbc</groupId>
            <artifactId>ojdbc8</artifactId>
            <version>${ojdbc.version}</version>
        </dependency>
        <!-- camel -->
        <dependency>
            <groupId>org.apache.camel</groupId>
            <artifactId>camel-jackson</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.camel.springboot</groupId>
            <artifactId>camel-jackson-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.camel.springboot</groupId>
            <artifactId>camel-jacksonxml-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.camel.springboot</groupId>
            <artifactId>camel-bean-starter</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.apache.camel</groupId>
                    <artifactId>camel-caffeine-lrucache</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.apache.camel</groupId>
                    <artifactId>camel-xml-jaxp</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.apache.camel</groupId>
                    <artifactId>camel-xpath</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.apache.camel</groupId>
                    <artifactId>camel-vm</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.apache.camel</groupId>
                    <artifactId>camel-xml-jaxb</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.apache.camel</groupId>
                    <artifactId>camel-xslt</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.apache.camel</groupId>
                    <artifactId>camel-cluster</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.apache.camel</groupId>
                    <artifactId>camel-file</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <!-- spring batch -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-batch</artifactId>
            <version>3.3.4</version>
        </dependency>
        <!-- drools -->
        <!-- CEW -->
        <dependency>
            <groupId>com.uob.gwb</groupId>
            <artifactId>common-utils</artifactId>
            <version>${common-utils.version}</version>
        </dependency>
        <dependency>
            <groupId>com.uob.gwb</groupId>
            <artifactId>transaction-common</artifactId>
            <version>${transaction-common.version}</version>
            <exclusions>
                <exclusion>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <version>${mapstruct.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.codehaus.janino</groupId>
            <artifactId>janino</artifactId>
        </dependency>
        <!-- compile -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>compile</scope>
        </dependency>
        <!-- test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.batch</groupId>
            <artifactId>spring-batch-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-inline</artifactId>
            <version>4.11.0</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.batch</groupId>
            <artifactId>spring-batch-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven.compiler.plugin.version}</version>
                <configuration>
                    <forceJavacCompilerUse>true</forceJavacCompilerUse>
                    <compilerArgs>
                        <arg>-parameters</arg>
                    </compilerArgs>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>${lombok-mapstruct-binding.version}</version>
                        </path>
                    </annotationProcessorPaths>
                    <release>${java.version}</release>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <version>${maven.failsafe.plugin.version}</version>
                <configuration>
                    <includes>
                        <include>**/*IT.java</include>
                    </includes>
                    <additionalClasspathElements>
                        <additionalClasspathElement>${basedir}/target/classes
                        </additionalClasspathElement>
                    </additionalClasspathElements>
                    <parallel>none</parallel>
                    <argLine>${failsafe.jacoco.args}</argLine>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>io.github.git-commit-id</groupId>
                <artifactId>git-commit-id-maven-plugin</artifactId>
                <version>${git-commit-id-maven-plugin.version}</version>
                <executions>
                    <execution>
                        <id>get-the-git-infos</id>
                        <goals>
                            <goal>revision</goal>
                        </goals>
                        <phase>initialize</phase>
                    </execution>
                </executions>
                <configuration>
                    <verbose>true</verbose>
                    <generateGitPropertiesFile>true</generateGitPropertiesFile>
                    <generateGitPropertiesFilename>${project.build.outputDirectory}/git.properties
                    </generateGitPropertiesFilename>
                    <gitDescribe>
                        <skip>false</skip>
                        <always>false</always>
                        <dirty>-dirty</dirty>
                    </gitDescribe>
                    <commitIdGenerationMode>full</commitIdGenerationMode>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring.boot.version}</version>
                <configuration>
                    <mainClass>com.uob.ufw.batch.sample.SampleBatchApplication
                    </mainClass>
                    <layout>ZIP</layout>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>build-info</id>
                        <goals>
                            <goal>build-info</goal>
                        </goals>
                        <configuration>
                            <additionalProperties>
                                <number>${env.BUILD_NUMBER}</number>
                                <url>${env.BUILD_URL}</url>
                            </additionalProperties>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.openrewrite.maven</groupId>
                <artifactId>rewrite-maven-plugin</artifactId>
                <version>5.7.1</version>
                <configuration>
                    <activeRecipes>
                        <recipe>org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_1</recipe>
                        <recipe>org.openrewrite.java.migrate.UpgradeToJava17</recipe>
                    </activeRecipes>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>org.openrewrite.recipe</groupId>
                        <artifactId>rewrite-spring</artifactId>
                        <version>5.0.10</version>
                    </dependency>
                    <dependency>
                        <groupId>org.openrewrite.recipe</groupId>
                        <artifactId>rewrite-migrate-java</artifactId>
                        <version>2.1.0</version>
                    </dependency>
                </dependencies>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>**/*IT.java</exclude>
                    </excludes>
                    <argLine>${surefire.jacoco.args}
                        --add-opens java.base/java.time=ALL-UNNAMED
                    </argLine>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>${jacoco.maven.plugin.version}</version>
                <configuration>
                    <excludes>
                        <exclude>**org.drools*</exclude>
                        <exclude>**/*Test.java</exclude>
                        <exclude>**/mapper/*.java</exclude>
                        <exclude>**/model/*.java</exclude>
                        <exclude>**/dao/*.java</exclude>
                        <exclude>**/domain/*.java</exclude>
                        <exclude>**/api/*.java</exclude>
                        <exclude>**/config/*.java</exclude>
                        <exclude>**/processing/*.java</exclude>
                        <exclude>**/builder/*.java</exclude>
                    </excludes>
                </configuration>
                <executions>
                    <execution>
                        <id>before-unit-test-execution</id>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                        <configuration>
                            <destFile>${project.build.directory}/jacoco-output/jacoco-unit-tests.exec
                            </destFile>
                            <propertyName>surefire.jacoco.args</propertyName>
                        </configuration>
                    </execution>
                    <execution>
                        <id>after-unit-test-execution</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                        <configuration>
                            <dataFile>${project.build.directory}/jacoco-output/jacoco-unit-tests.exec
                            </dataFile>
                            <outputDirectory>${project.reporting.outputDirectory}/jacoco-unit-test-coverage-report
                            </outputDirectory>
                        </configuration>
                    </execution>
                    <execution>
                        <id>before-integration-test-execution</id>
                        <phase>pre-integration-test</phase>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                        <configuration>
                            <destFile>${project.build.directory}/jacoco-output/jacoco-integration-tests.exec
                            </destFile>
                            <propertyName>failsafe.jacoco.args</propertyName>
                        </configuration>
                    </execution>
                    <execution>
                        <id>after-integration-test-execution</id>
                        <phase>post-integration-test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                        <configuration>
                            <dataFile>${project.build.directory}/jacoco-output/jacoco-integration-tests.exec
                            </dataFile>
                            <outputDirectory>
                                ${project.reporting.outputDirectory}/jacoco-integration-test-coverage-report
                            </outputDirectory>
                        </configuration>
                    </execution>
                    <execution>
                        <id>merge-unit-and-integration</id>
                        <phase>post-integration-test</phase>
                        <goals>
                            <goal>merge</goal>
                        </goals>
                        <configuration>
                            <fileSets>
                                <fileSet>
                                    <directory>${project.build.directory}/jacoco-output/
                                    </directory>
                                    <includes>
                                        <include>*.exec</include>
                                    </includes>
                                </fileSet>
                            </fileSets>
                            <destFile>${project.build.directory}/jacoco.exec
                            </destFile>
                        </configuration>
                    </execution>
                    <execution>
                        <id>create-merged-report</id>
                        <phase>post-integration-test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                        <configuration>
                            <dataFile>${project.build.directory}/jacoco.exec
                            </dataFile>
                            <outputDirectory>${project.reporting.outputDirectory}/jacoco
                            </outputDirectory>
                        </configuration>
                    </execution>
                    <execution>
                        <id>check</id>
                        <goals>
                            <goal>check</goal>
                        </goals>
                        <configuration>
                            <excludes>
                                <exclude>**/cbp/data/mapper/*</exclude>
                            </excludes>
                            <rules>
                                <rule>
                                    <element>BUNDLE</element>
                                    <limits>
                                        <limit>
                                            <counter>LINE</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>0.0
                                            </minimum> <!-- Adjust the code coverage percent as needed. Recommended entry to achieve 0.70 -->
                                        </limit>
                                    </limits>
                                </rule>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>com.diffplug.spotless</groupId>
                <artifactId>spotless-maven-plugin</artifactId>
                <version>${spotless.plugin.version}</version>
                <configuration>
                    <java>
                        <eclipse>
                            <version>${eclipse-java-format.version}</version>
                            <file>${project.basedir}/eclipse-formatter.xml</file>
                        </eclipse>
                        <importOrder/>
                        <removeUnusedImports/>
                        <trimTrailingWhitespace/>
                        <endWithNewline/>
                        <indent> <!-- specify whether to use tabs or spaces for indentation -->
                            <spaces>true</spaces> <!-- or <tabs>true</tabs> -->
                            <spacesPerTab>4</spacesPerTab> <!-- optional, default is 4 -->
                        </indent>
                        <licenseHeader>
                            <file>${project.basedir}/license-header</file>
                        </licenseHeader>
                    </java>
                </configuration>
                <executions>
                    <execution>
                        <phase>verify</phase>
                        <goals>
                            <goal>apply</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
    <distributionManagement>
        <repository>
            <id>central</id>
            <name>Artifactory-releases</name>
            <url>https://artifactoryp.sg.uobnet.com/artifactory/digibank-local2</url>
        </repository>
        <snapshotRepository>
            <id>snapshots</id>
            <name>Artifactory-snapshots</name>
            <url>https://artifactoryp.sg.uobnet.com/artifactory/digibank-local2</url>
        </snapshotRepository>
    </distributionManagement>
    <profiles>
        <profile>
            <id>BUILD_URL default value</id>
            <activation>
                <property>
                    <name>!env.BUILD_URL</name>
                </property>
            </activation>
            <properties>
                <env.BUILD_URL>https://jenkinsp.sg.uobnet.com/jenkins/view/GBT_CNP_CEW</env.BUILD_URL>
                <env.BUILD_NUMBER>1</env.BUILD_NUMBER>
            </properties>
        </profile>
    </profiles>
</project>
```
# application.yml

```yml
routeConfigserver:
  port: ${pbp_port:8015}
  servlet:
    context-path: /paymentbulkprocessing
  shutdown: graceful
  tomcat:
    threads:
      min-spare: 100
      max: 200
    connection-timeout: 10s
    accept-count: 200
    max-connections: 600
    accesslog:
      enabled: true
      buffered: false
      rotate: true
      max-days: 90
      encoding: UTF-8
      request-attributes-enabled: true
      pattern: "%{yyyy-MM-dd HH:mm:ss.SSS}t %h %r %s %b [%D ms]"
      directory: ${log_dir}
      prefix: access_log_paymentbulkprocessing
      suffix: .log
  ssl:
    enabled: false
    client-auth:
    key-store:
    key-alias:
    key-store-type:
    key-store-password:
    key-password:
    trust-store:
    trust-store-password:
    trust-store-type:

spring:
  config:
    import: bootstrap.yml,classpath:trancommon.yml,classpath:tran-common-caches.yml,classpath:common-utils.yml
  lifecycle:
    timeout-per-shutdown-phase: 20s
  jackson:
    serialization:
      WRITE_DATES_AS_TIMESTAMPS: false
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://127.0.0.1:8080/auth/realms/oauth2-sample/protocol/openid-connect/certs
  datasource:
    common:
      type: com.zaxxer.hikari.HikariDataSource
      driver-class-name: oracle.jdbc.OracleDriver
      hikari:
        maximum-pool-size: 20
        minimum-idle: 10
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
        data-source-properties:
          rewriteBatchedStatement: true
          cachePrepStmts: true
          preStmtCacheSize: 250
          preStmtCacheSqlLimit: 2048
      vault:
        enabled: false
        key-store: /path/to/keystore.jks
        key-store-password: keystorePassword
        key-store-type: JKS
        trust-store: /path/to/truststore.jks
        trust-store-password: truststorePassword
        wallet-location: /path/to/wallet

    # Default datasource for Spring Batch
    default:
      jdbc-url: jdbc:oracle:thin:@localhost:1521/XEPDB1
      username: batch_user
      password: batch_password

    # PWS Insertion datasource
    pws-insertion:
      jdbc-url: jdbc:oracle:thin:@localhost:1521/PWSDB1
      username: pws_insert_user
      password: pws_insert_password

    # PWS Loading datasource
    pws-loading:
      jdbc-url: jdbc:oracle:thin:@localhost:1521/PWSDB2
      username: pws_load_user
      password: pws_load_password

    batch:
      job:
        enabled: false  # Disable auto-start of jobs
      job-repository:
        initialize: false  # Set to true to use database-backed job repository

mybatis:
  mapper-locations:
    - classpath:mappers/**/*.xml
  configuration:
    jdbc-type-for-null: VARCHAR
    default-statement-timeout: 5          #public-key-location: file:${ssl_dir}/geb-sg-sso-jwt.pub

management:
  endpoint:
    prometheus:
      enabled: true
    health:
      probes:
        enabled: true
      show-details: ALWAYS
    caches:
      enabled: true
    metrics:
      enabled: true
  endpoints:
    web:
      exposure:
        include:
          - health
          - info
          - prometheus
          - metrics
          - caches

country: th
country_code: TH

ufw:
  app:
    enable-db-performance-logging: true
    enable-camel-performance-logging: true
    jwt-require-claims: false
    jwt-subject-additional-fields:
      - userSegmentUUID
    jwt-claim-keys:
      - sub
      - iss
      - userSegmentUUID
    enable-entitlements: true
    enable-simple-policy-enforcement: true
    symmetric-crypto:
      secret: password
      salt: 5c0744940b5c369b
    jwt-public-keys:
      - client-id: IDBv2
        client-public-key-path: ${ssl_dir}/geb-${country}-sso-jwt.pub
        key-algorithm: rs256
    async-protocol: cew-mq
    mq-properties:

tranCommon:
  connection-per-route@: 40
  sort-per-route@: asc
  max-total-connections@: 400
  socket-timeout@: 5000
  connect-timeout@: 3000
  request-timeout@: 1000
  initiateService:
    enable@: false
  ueqs:
    defaultEntitlementRole@:
    enable@: true
    url:
      routes@: ${scheme}://${hostname}:${ueqs_port},${scheme}://${hostname}:${ueqs_routes_1}
      maximumFailoverAttempts@: 2
      base-path@: /userentitlementqueryapi
      companyAndAccount@: /api/entitlements/v1/companyAndAccounts/
      resourcesAndFeatures@: /api/entitlements/v1/resourceAndFeaturesFromJWT
      companyAndAccountsForUserResourceFeatures@: /api/entitlements/v3/companyAndAccountsForUserResourceFeatures
      resourceAndFileType@: /api/entitlements/v1/getResourceAndFileType
  rds:
    enable@: true
    url:
      routes@: ${scheme}://${hostname}:${rds_port}
      maximumFailoverAttempts@: 2
      base-path@: /banklookupapi
      v2-country-list@: /api/refData/v2/countries
      payment-codes@: /api/refData/v1/paymentCodes
      v2-banks-list@: /api/refData/v2/banks
  fx:
    enable@: true
    url:
      routes@: ${scheme}://${hostname}:${fx_port}
      maximumFailoverAttempts@: 2
      base-path@: /fxservice
      prebook-contract@: /api/foreignExchange/v1/prebookedContracts
      fxRate@: /api/foreignExchange/v1/fxRate
      computeEquivalentAmount@: /api/foreignExchange/v1/computeEquivalentAmount

bulk-routes:
  - route-name: CUSTOMER_SUBMITTED_TRANSFORMED
    processing-type: INBOUND
    source-type: FILE
    enabled: true
    steps:
      - pain001-validation
      - group-validation
      - paymentInfo-splitting
      - paymentInfo-validation
      - paymentInfo-enrichment
      - transaction-validation
      - transaction-enrichment
      - pws-computation
      - pws-insertion
      - notification
    file-source:
      directoryName: /path/to/inbound/processing
      antInclude: "*_Auth.json"
      antExclude:
      charset: utf-8
      doneFileName: "${file:name:noext}.xml.done"
      delay: 6000
      sortBy: file:modified
      maxMessagesPerPoll: 1
      noop: false
      recursive: false
      move: /path/to/inbound/backup
      moveFailed: /path/to/inbound/error
      readLock: rename
      readLockTimeout: 60000
      readLockInterval: 1000
      readLockLoggingLevel: WARN
    pws-insertion:
      datasource: datasource-pws-insertion
    notification:
      email:
        host: smtp.example.com
        port: 587
        username: notification@example.com
        password: notification_password
    file-archive:
      path: /path/to/archive/folder

  - route-name: CUSTOMER_AUTHORIZED
    processing-type: OUTBOUND
    destination-type: API
    enabled: false
    steps:
      - pws-loading
      - pain001-transformation
    api-source:
      http-uri: /path/to/api
    pws-loading:
      datasource: datasource-pws-loading
    pain001-transformation:
      template-path: /path/to/pain001/template.xml
    file-destination:
      directoryName: /path/to/outbound/processing
      fileName: "${header.CamelFileName}"
      tempFileName: "${file:name:noext}.tmp"
      doneFileName: "${file:name:noext}.xml.done"
      autoCreate: true
      fileExist: Override
      moveExisting:
      eagerDeleteTargetFile: false
      delete: true
      chmod: rw-r--r--
```

# logback.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

	<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
		<encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5level) [%thread] %cyan(%logger{36}) - %msg%n</pattern>
		</encoder>
	</appender>

	<!-- LOG everything at INFO level -->
	<root level="info">
		<appender-ref ref="STDOUT" />
	</root>


	<logger name="com.zaxxer.hikari" level="info"/>
	<logger name="org.apache.camel" level="debug"/>
	<logger name="org.apache.kafka" level="debug"/>
	<logger name="org.mybatis" level="debug"/>
	<logger name="org.springframework.security" level="debug"/>
	<logger name="org.springframework" level="debug"/>


	<logger name="com.uob.gwb.pre.processing" level="debug"/>
	<logger name="com.uob.gwb.transaction" level="debug"/>
	<logger name="com.uob.gwb.common" level="debug"/>
	<logger name="com.uob.gwb.paymentv3" level="debug"/>
	<logger name="com.uob.gwb.pws" level="debug"/>
	<logger name="com.uob.gwb.ufw" level="debug"/>
	<logger name="com.uob.gwb.utils" level="info"/>
	<logger name="com.uob.trx.auth.matrix" level="debug"/>
	<logger name="com.uob.ufw" level="debug"/>
	<logger name="com.uob.ufw.security" level="debug"/>


	<logger name="com.uob.gwb.ans" level="debug"/>
	<logger name="com.uob.gwb.aes" level="debug"/>
	<logger name="com.uob.gwb.bms" level="debug"/>
	<logger name="com.uob.gwb.pis" level="debug"/>
	<logger name="com.uob.gwb.pws" level="debug"/>
	<logger name="com.uob.ref" level="debug"/>
	<logger name="com.uob.gwb.tws" level="debug"/>

</configuration>
```

# BatchConfig

```java
package com.uob.gwb.pbp.config;


import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Value("${spring.batch.jdbc.initialize-schema:false}")
    private boolean initializeSchema;

    @Bean
    public PlatformTransactionManager transactionManager(@Qualifier("defaultDataSource") DataSource dataSource) {
        return initializeSchema ? new DataSourceTransactionManager(dataSource) : new ResourcelessTransactionManager();
    }

    @Bean
    public JobRepository jobRepository(PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setTransactionManager(transactionManager);
        factory.setIsolationLevelForCreate("ISOLATION_SERIALIZABLE");
        factory.setTablePrefix("batch_");
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Bean
    public JobExplorer jobExplorer(PlatformTransactionManager transactionManager) throws Exception {
        JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
        factory.setTransactionManager(transactionManager);
        factory.afterPropertiesSet();
        return factory.getObject();

    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}
```

# flow builder 

```java
package com.uob.gwb.pbp.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uob.gwb.pbp.config.BulkRoutesConfig;
import com.uob.gwb.pbp.config.BulkRoutesConfig.RouteConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BulkProcessingFlowBuilder extends RouteBuilder {

    private final BulkRoutesConfig bulkRoutesConfig;
    private final ObjectMapper mapper;
    private final JobRepository jobRepository;
    private final JobLauncher jobLauncher;
    private final PlatformTransactionManager transactionManager;

    // ... existing methods (configure, configureRoute, buildInboundFromUri, etc.) ...

    private Job createJob(RouteConfig routeConfig) {
        JobBuilder jobBuilder = new JobBuilder(routeConfig.getRouteName() + "Job", jobRepository)
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        ExecutionContext executionContext = jobExecution.getExecutionContext();
                        executionContext.put("routeConfig", routeConfig);
                        // Add any other common metadata here
                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                            log.info("Job completed successfully");
                        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
                            log.error("Job failed with status: {}", jobExecution.getStatus());
                        }
                    }
                });

        List<String> stepNames = routeConfig.getSteps();
        if (stepNames == null || stepNames.isEmpty()) {
            throw new BulkProcessingException("No steps defined for route: " + routeConfig.getRouteName());
        }

        Step firstStep = createStepForName(stepNames.get(0), routeConfig);
        JobBuilder flowJobBuilder = jobBuilder.start(firstStep);

        for (int i = 1; i < stepNames.size(); i++) {
            Step step = createStepForName(stepNames.get(i), routeConfig);
            flowJobBuilder = flowJobBuilder.next(step);
        }

        return flowJobBuilder.build();
    }

    private Step createStepForName(String stepName, RouteConfig routeConfig) {
        log.info("Creating step: {} for route: {}", stepName, routeConfig.getRouteName());
        StepBuilder stepBuilder = new StepBuilder(stepName, jobRepository);

        switch (stepName) {
            case "pain001-validation":
                return createPain001ValidationStep(stepBuilder, routeConfig);
            case "group-validation":
                return createGroupValidationStep(stepBuilder, routeConfig);
            // ... other cases ...
            default:
                throw new BulkProcessingException("Unknown step: " + stepName);
        }
    }

private Step fileLevelValidationStep(RouteConfig routeConfig) {
        return new StepBuilder("fileLevelValidationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    ExecutionContext jobContext = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
                    String filePath = (String) jobContext.get("filePath");
                    boolean isValid = pain001FileValidator.validateFile(filePath);
                    if (!isValid) {
                        throw new ValidationException("File-level validation failed for " + filePath);
                    }
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }	

    private Step createPain001ValidationStep(StepBuilder stepBuilder, RouteConfig routeConfig) {
        return stepBuilder
            .tasklet((contribution, chunkContext) -> {
                ExecutionContext jobContext = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
                // Access routeConfig or other metadata from jobContext
                
                // Implement Pain001 validation logic here
                return RepeatStatus.FINISHED;
            }, transactionManager)
            .listener(new StepExecutionListener() {
                @Override
                public void beforeStep(StepExecution stepExecution) {
                    // Preparation logic
                }

                @Override
                public ExitStatus afterStep(StepExecution stepExecution) {
                    if (stepExecution.getStatus() == BatchStatus.FAILED) {
                        // If errorOnFile is set, we return a special exit status
                        return new ExitStatus("FAILED_STOP_JOB");
                    }
                    return stepExecution.getExitStatus();
                }
            })
            .build();
    }

    private Step createGroupValidationStep(StepBuilder stepBuilder, RouteConfig routeConfig) {
        return stepBuilder
            .<Pain001Group, Pain001Group>chunk(10, transactionManager)
            .reader(pain001GroupReader(routeConfig))
            .processor(groupValidator())
            .writer(validatedGroupWriter())
            .faultTolerant()
            .retry(TransientDataAccessException.class)
            .retryLimit(3)
            .skip(ValidationException.class)
            .skipLimit(10)
            .listener(new StepExecutionListener() {
                @Override
                public ExitStatus afterStep(StepExecution stepExecution) {
                    if (stepExecution.getStatus() == BatchStatus.FAILED) {
                        return new ExitStatus("FAILED_STOP_JOB");
                    }
                    return stepExecution.getExitStatus();
                }
            })
            .build();
    }

    // ... implement other step creation methods ...

    @Override
    public void configure() throws Exception {
        for (RouteConfig routeConfig : bulkRoutesConfig.getRoutes()) {
            if (routeConfig.isEnabled()) {
                from(buildInboundFromUri(routeConfig))
                    .routeId(routeConfig.getRouteName())
                    .process(exchange -> {
                        Job job = createJob(routeConfig);
                        JobParameters jobParameters = createJobParameters(exchange, routeConfig);
                        JobExecution jobExecution = jobLauncher.run(job, jobParameters);
                        
                        // Check for the special exit status
                        if (jobExecution.getExitStatus().getExitCode().equals("FAILED_STOP_JOB")) {
                            exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
                            // Optionally, set an error message or perform cleanup
                        }
                    });
            }
        }
    }

    // ... other methods ...
}

```

```java
public class BulkProcessingFlowBuilder extends RouteBuilder {

    private final BulkRoutesConfig bulkRoutesConfig;
    private final ObjectMapper mapper;
    private final JobRepository jobRepository;
    private final JobLauncher jobLauncher;
    private final PlatformTransactionManager transactionManager;

    @Autowired
    private Pain001FileValidator pain001FileValidator;
    @Autowired
    private Pain001GroupProcessor pain001GroupProcessor;
    @Autowired
    private Pain001ItemReader pain001ItemReader;
    @Autowired
    private Pain001ItemWriter pain001ItemWriter;

    // ... other existing fields ...

    private Job createJob(RouteConfig routeConfig) {
        JobBuilder jobBuilder = new JobBuilder(routeConfig.getRouteName() + "Job", jobRepository)
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        ExecutionContext executionContext = jobExecution.getExecutionContext();
                        executionContext.put("routeConfig", routeConfig);
                        executionContext.put("filePath", routeConfig.getInputFilePath());
                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                            log.info("Job completed successfully");
                        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
                            log.error("Job failed with status: {}", jobExecution.getStatus());
                        }
                    }
                });

        return jobBuilder
                .start(fileLevelValidationStep(routeConfig))
                .next(groupLevelProcessingStep(routeConfig))
                .next(transactionProcessingStep(routeConfig))
                .build();
    }

    private Step fileLevelValidationStep(RouteConfig routeConfig) {
        return new StepBuilder("fileLevelValidationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    ExecutionContext jobContext = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
                    String filePath = (String) jobContext.get("filePath");
                    boolean isValid = pain001FileValidator.validateFile(filePath);
                    if (!isValid) {
                        throw new ValidationException("File-level validation failed for " + filePath);
                    }
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    private Step groupLevelProcessingStep(RouteConfig routeConfig) {
        return new StepBuilder("groupLevelProcessingStep", jobRepository)
                .<Pain001Group, Pain001Group>chunk(10, transactionManager)
                .reader(pain001GroupReader(routeConfig))
                .processor(pain001GroupProcessor)
                .writer(groupWriter())
                .build();
    }

    private ItemReader<Pain001Group> pain001GroupReader(RouteConfig routeConfig) {
        return new ItemReader<Pain001Group>() {
            private Iterator<Pain001Group> groupIterator;

            @Override
            public Pain001Group read() throws Exception {
                if (groupIterator == null) {
                    ExecutionContext jobContext = StepSynchronizationManager.getContext().getStepExecution().getJobExecution().getExecutionContext();
                    String filePath = (String) jobContext.get("filePath");
                    List<Pain001Group> groups = pain001FileValidator.extractGroups(filePath);
                    groupIterator = groups.iterator();
                }
                return groupIterator.hasNext() ? groupIterator.next() : null;
            }
        };
    }

    private ItemWriter<Pain001Group> groupWriter() {
        return new ItemWriter<Pain001Group>() {
            @Override
            public void write(List<? extends Pain001Group> items) throws Exception {
                // Implement group-level writing logic if needed
                // This could involve updating a database with group-level information
                // or preparing data for the next step
            }
        };
    }

    private Step transactionProcessingStep(RouteConfig routeConfig) {
        return new StepBuilder("transactionProcessingStep", jobRepository)
                .<Pain001Bulk, Pain001Bulk>chunk(10, transactionManager)
                .reader(pain001ItemReader)
                .processor(pain001Validator())
                .writer(pain001ItemWriter)
                .listener(new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();
                        String filePath = (String) jobContext.get("filePath");
                        pain001ItemReader.setFilePath(filePath);
                    }

                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        if (stepExecution.getStatus() == BatchStatus.FAILED) {
                            return new ExitStatus("FAILED_STOP_JOB");
                        }
                        return stepExecution.getExitStatus();
                    }
                })
                .faultTolerant()
                .skip(ValidationException.class)
                .skipLimit(10)
                .build();
    }

    private ItemProcessor<Pain001Bulk, Pain001Bulk> pain001Validator() {
        return new ItemProcessor<Pain001Bulk, Pain001Bulk>() {
            @Override
            public Pain001Bulk process(Pain001Bulk item) throws Exception {
                // Implement transaction-level validation logic here
                // You can use your existing RuleEngine here if needed
                return item; // Return null to filter out the item
            }
        };
    }

    @Override
    public void configure() throws Exception {
        for (RouteConfig routeConfig : bulkRoutesConfig.getRoutes()) {
            if (routeConfig.isEnabled()) {
                from(buildInboundFromUri(routeConfig))
                    .routeId(routeConfig.getRouteName())
                    .process(exchange -> {
                        Job job = createJob(routeConfig);
                        JobParameters jobParameters = createJobParameters(exchange, routeConfig);
                        JobExecution jobExecution = jobLauncher.run(job, jobParameters);
                        
                        if (jobExecution.getExitStatus().getExitCode().equals("FAILED_STOP_JOB")) {
                            exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
                            // Optionally, set an error message or perform cleanup
                        }
                    });
            }
        }
    }

    // ... other existing methods ...
}

// You'll need to implement these:
public interface Pain001FileValidator {
    boolean validateFile(String filePath) throws Exception;
    List<Pain001Group> extractGroups(String filePath) throws Exception;
}

@Component
public class Pain001GroupProcessor implements ItemProcessor<Pain001Group, Pain001Group> {
    @Override
    public Pain001Group process(Pain001Group item) throws Exception {
        // Implement group-level processing logic
        return item;
    }
}

public class Pain001Group {
    // Define fields and methods for group-level data
}
```

```java
// Pain001ItemWriter using MyBatis
@Component
public class Pain001ItemWriter implements ItemWriter<Pain001Bulk> {
    @Autowired
    private PwsTransactionsMapper bulkTransactionMapper;
    @Autowired
    private PwsBulkTransactionsMapper bulkTransactionDetailsMapper;
    @Autowired
    private PwsBulkTransactionInstructionsMapper instructionsMapper;

    @Override
    public void write(List<? extends Pain001Bulk> items) throws Exception {
        for (Pain001Bulk bulk : items) {
            bulkTransactionMapper.insertPwsTransactions(bulk.getBulkTransaction());
            bulkTransactionDetailsMapper.insertPwsBulkTransactions(bulk.getBulkTransactionDetails());
            
            for (Pain001Transaction transaction : bulk.getTransactions()) {
                instructionsMapper.insertPwsBulkTransactionInstructions(transaction.getInstruction());
                // Insert other related information (parties, charges, etc.)
            }
        }
    }
}
```
