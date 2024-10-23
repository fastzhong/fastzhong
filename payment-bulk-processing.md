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
 /*
 * Copyright (c) United Overseas Bank Limited Co.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * United Overseas Bank Limited Co. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in accordance
 * with the terms of the license agreement you entered into with
 * United Overseas Bank Limited Co.
 */
package com.uob.gwb.pbp.flow;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uob.gwb.pbp.config.BulkRoutesConfig;
import com.uob.gwb.pbp.config.BulkRoutesConfig.ProcessingType;
import com.uob.gwb.pbp.config.BulkRoutesConfig.RouteConfig;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BulkProcessingFlowBuilder extends RouteBuilder {

    private final BulkRoutesConfig bulkRoutesConfig;
    private final ObjectMapper mapper;
    private final JobRepository jobRepository;
    private final JobLauncher jobLauncher;
    private final PlatformTransactionManager platformTransactionManager;

    @Override
    public void configure() throws Exception {
	// Global error handling - simplified since file movement is handled by Camel
    	onException(BulkProcessingException.class)
        .handled(true)
        .process(exchange -> {
            Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
            String fileName = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
            
            // Just log the error and update status - file movement is handled by moveFailed option
            log.error("Processing failed for file: {}. Error: {}", fileName, cause.getMessage(), cause);
            updateProcessingStatus(exchange, "FAILED");
        });

	    // Configure routes based on config
	    for (RouteConfig routeConfig : bulkRoutesConfig.getRoutes()) {
	        if (routeConfig.isEnabled()) {
	            configureRoute(routeConfig);
	        }
	    }
    }

    private void configureRoute(RouteConfig routeConfig) throws Exception {
        log.info("Creating processing flow: {}", routeConfig.toString());
        
        if (routeConfig.getProcessingType() == ProcessingType.INBOUND) {
            from(buildInboundFromUri(routeConfig))
                .routeId(routeConfig.getRouteName())
                .process(exchange -> {
                // Create and run the batch job
                Job job = createJob(routeConfig);
                JobParameters jobParameters = createJobParameters(exchange, routeConfig);
                JobExecution jobExecution = jobLauncher.run(job, jobParameters);
                
                // If job fails, Camel will automatically move the file using moveFailed
                if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
                    throw new BulkProcessingException(
                        "Job failed: " + jobExecution.getExitStatus().getExitDescription(),
                        new Throwable(jobExecution.getExitStatus().getExitDescription())
                    );
                }
            });
                
        } else if (routeConfig.getProcessingType() == ProcessingType.OUTBOUND) {
            from(buildOutboundFromUri(routeConfig))
                .routeId(routeConfig.getRouteName())
                .process(this::prepareOutboundContext)
                .process(exchange -> {
                    Job job = createJob(routeConfig);
                    JobParameters jobParameters = createJobParameters(exchange, routeConfig);
                    JobExecution jobExecution = jobLauncher.run(job, jobParameters);
                    handleJobExecution(exchange, jobExecution);
                })
                .choice()
                    .when(header("processingStatus").isEqualTo("SUCCESS"))
                        .to(buildOutboundToUri(routeConfig))
                    .otherwise()
                        .to("direct:errorHandler")
                .end();
        }
    }
		
    private String buildInboundFromUri(RouteConfig routeConfig) {
        switch (routeConfig.getSourceType()) {
            case FILE:
                BulkRoutesConfig.FileSource f = routeConfig.getFileSource();
            // Camel will automatically move failed files using moveFailed option
            String fileUri = "file:%s?antInclude=%s"
                    + "&antExclude=%s"
                    + "&charset=%s"
                    + "&doneFileName=%s"
                    + "&delay=%d"
                    + "&sortBy=%s"
                    + "&maxMessagesPerPoll=%d"
                    + "&noop=%b"
                    + "&recursive=%b"
                    + "&move=%s"
                    + "&moveFailed=%s"  // This handles failed file movement automatically
                    + "&readLock=%s"
                    + "&readLockTimeout=%d"
                    + "&readLockLoggingLevel=%s";
            
            return String.format(fileUri, 
                f.getDirectoryName(), 
                f.getAntInclude(),
                f.getAntExclude(),
                f.getCharset(),
                f.getDoneFileName(),
                f.getDelay(),
                f.getSortBy(),
                f.getMaxMessagesPerPoll(),
                f.isNoop(),
                f.isRecursive(),
                f.getMove(),
                f.getMoveFailed(),  // Failed files will be moved here automatically
                f.getReadLock(),
                f.getReadLockTimeout(),
                f.getReadLockInterval());
                
            case JDBC:
                return buildJdbcUri(routeConfig);
                
            case MESSAGE:
                return buildMessageUri(routeConfig);
                
            case API:
                return buildApiUri(routeConfig);
                
            default:
                log.error("Unsupported source type: {}", routeConfig.getSourceType());
                throw new BulkProcessingException("Unsupported source type", 
                    new Throwable("Unsupported source type: " + routeConfig.getSourceType()));
        }
    }

    private String buildOutboundFromUri(RouteConfig routeConfig) {
        // ToDo
        throw new BulkProcessingException("outbound flow yet to implement",
                new Throwable("outbound flow yet to implement"));
    }

    private String buildOutboundToUri(RouteConfig routeConfig) {
        switch (routeConfig.getDestinationType()) {
            case FILE :
                BulkRoutesConfig.FileDestination f = routeConfig.getFileDestination();
                String fileUri = "file:%s?fileName=%s" + "&tempFileName=%s" + "&doneFileName=%s" + "&autoCreate=%b"
                        + "&fileExist=%s" + "&moveExisting=%s" + "&eagerDeleteTargetFile=%b" + "&delete=%b"
                        + "&chmod=%s";
                return String.format(fileUri, f.getFileName(), f.getTempFileName(), f.getDoneFileName(),
                        f.isAutoCreate(), f.getFileExist(), f.getMoveExisting(), f.isEagerDeleteTargetFile(),
                        f.isDelete(), f.getChmod());
            case JDBC, MESSAGE, API :
                break;
        }
        log.error("destination type {} not supported", routeConfig.getDestinationType());
        throw new BulkProcessingException("destination not supported", new Throwable("destination not supported"));
    }

    
    private void validateInputFile(Exchange exchange) {
        try {
            // Get file content
            String content = exchange.getIn().getBody(String.class);
            
            // Basic file validation
            if (content == null || content.trim().isEmpty()) {
                throw new BulkProcessingException("Empty file received", new Throwable("Empty file"));
            }
            
            // Validate file format
            Pain001 pain001 = mapper.readValue(content, Pain001.class);
            validatePain001Structure(pain001);
            
            // Set validated content in exchange
            exchange.setProperty("fileContent", content);
            exchange.setProperty("pain001", pain001);
            
        } catch (Exception e) {
            throw new BulkProcessingException("File validation failed", e);
        }
    }

    private JobParameters createJobParameters(Exchange exchange, RouteConfig routeConfig) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> jobContext = exchange.getProperty("jobContext", Map.class);
            
            return new JobParametersBuilder()
                .addString("routeName", routeConfig.getRouteName())
                .addString("routeConfig", mapper.writeValueAsString(routeConfig))
                .addString("fileName", (String) jobContext.get("fileName"))
                .addLong("timestamp", System.currentTimeMillis())
                .addString("content", (String) jobContext.get("fileContent"))
                .toJobParameters();
                
        } catch (JsonProcessingException e) {
            throw new BulkProcessingException("Error creating job parameters", e);
        }
    }

    private void handleJobExecution(Exchange exchange, JobExecution jobExecution) {
        // Set processing status based on job execution
        String status = jobExecution.getStatus().equals(BatchStatus.COMPLETED) ? "SUCCESS" : "FAILED";
        exchange.getIn().setHeader("processingStatus", status);
        
        // Handle job execution results
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("Job completed successfully: {}", jobExecution.getJobInstance().getJobName());
            processSuccessfulExecution(exchange, jobExecution);
        } else {
            log.error("Job failed: {}", jobExecution.getJobInstance().getJobName());
            ExitStatus exitStatus = jobExecution.getExitStatus();
            String errorMessage = exitStatus.getExitDescription();
            exchange.setProperty("errorMessage", errorMessage);
            processFailedExecution(exchange, jobExecution);
        }
    }

    private void processSuccessfulExecution(Exchange exchange, JobExecution jobExecution) {
        ExecutionContext executionContext = jobExecution.getExecutionContext();
        // Process any results from the job
        if (executionContext.containsKey("processedPayments")) {
            @SuppressWarnings("unchecked")
            List<PaymentInformation> processedPayments = 
                (List<PaymentInformation>) executionContext.get("processedPayments");
            exchange.setProperty("processedPayments", processedPayments);
        }
        // Update processing status or send notifications
        updateProcessingStatus(exchange, "SUCCESS");
    }

    private void processFailedExecution(Exchange exchange, JobExecution jobExecution) {
        String errorMessage = exchange.getProperty("errorMessage", String.class);
        log.error("Job failed with error: {}", errorMessage);
        // Update processing status or send notifications
        updateProcessingStatus(exchange, "FAILED");
        // Handle cleanup if needed
        handleFailureCleanup(exchange);
    }

    private Job createJob(RouteConfig routeConfig) {
        JobBuilder jobBuilder = new JobBuilder(routeConfig.getRouteName() + "Job", jobRepository);
        jobBuilder.listener(new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                ExecutionContext executionContext = jobExecution.getExecutionContext();
                executionContext.put("routeConfig", routeConfig);
                // ToDo: add any other common metadata here
                // ToDo: how to pass file content to job
                log.info("Job started successfully");
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
            throw new BulkProcessingException("No steps defined for route: " + routeConfig.getRouteName(),
                    new Throwable("no steps defined"));
        }

        Step firstStep = createStepForName(stepNames.get(0), routeConfig);
        SimpleJobBuilder simpleJobBuilder = jobBuilder.start(firstStep);

        for (int i = 1; i < stepNames.size(); i++) {
            Step step = createStepForName(stepNames.get(i), routeConfig);
            simpleJobBuilder.next(step);
        }

        return simpleJobBuilder.build();
    }

    private Step createStepForName(String stepName, RouteConfig routeConfig) {

        log.info("Creating step: {} for route: {}", stepName, routeConfig.getRouteName());

        StepBuilder stepBuilder = new StepBuilder(stepName, jobRepository);
        switch (stepName) {
            case "pain001-processing" :
                return createPain001ProcessingStep(stepBuilder, routeConfig);
            // ... other cases ...
            default :
                throw new BulkProcessingException("Unknown step: " + stepName, new Throwable("Unknown step"));
        }
    }

    // implement steps

	private Step createPain001ProcessingStep(StepBuilder stepBuilder, RouteConfig routeConfig) {
    return stepBuilder.tasklet((contribution, chunkContext) -> {
        ExecutionContext jobContext = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext();
        
        String fileContent = jobContext.getString("fileContent");
        ObjectMapper objectMapper = new ObjectMapper();
        Pain001 pain001 = objectMapper.readValue(fileContent, Pain001.class);
        
        // Validate file-level data
        validatePain001FileLevel(pain001);
        
        // Process group header
        processGroupHeader(pain001.getBusinessDocument()
            .getCustomerCreditTransferInitiation()
            .getGroupHeader());
            
        // Convert to business objects for further processing
        List<PaymentInformation> paymentInformations = convertToPaymentInformation(pain001);
        jobContext.put("paymentInformations", paymentInformations);
        
        return RepeatStatus.FINISHED;
    }, platformTransactionManager).build();
}

private Step createPaymentSplittingStep(StepBuilder stepBuilder, RouteConfig routeConfig) {
    return stepBuilder.tasklet((contribution, chunkContext) -> {
        ExecutionContext jobContext = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext();
        
        @SuppressWarnings("unchecked")
        List<PaymentInformation> paymentInformations = 
            (List<PaymentInformation>) jobContext.get("paymentInformations");
        
        List<PaymentInformation> splitPayments = new ArrayList<>();
        
        for (PaymentInformation payment : paymentInformations) {
            // Split based on amount threshold and bank code
            List<PaymentInformation> split = splitPaymentInformation(payment, 
                routeConfig.getMaxTransactionAmount(),
                routeConfig.getMaxTransactionsPerBatch());
            splitPayments.addAll(split);
        }
        
        jobContext.put("splitPaymentInformations", splitPayments);
        return RepeatStatus.FINISHED;
    }, platformTransactionManager).build();
}

private Step createPaymentProcessingStep(StepBuilder stepBuilder, RouteConfig routeConfig) {
    return stepBuilder.<PaymentInformation, PaymentInformation>chunk(100, platformTransactionManager)
        .reader(new ListItemReader<>((List<PaymentInformation>) 
            stepBuilder.getJobRepository()
                      .getLastJobExecution(routeConfig.getRouteName(), new JobParameters())
                      .getExecutionContext()
                      .get("splitPaymentInformations")))
        .processor(payment -> {
            // Validate payment information
            validatePaymentInformation(payment);
            
            // Process credit transfer transactions
            processCreditTransferTransactions(payment);
            
            // Generate bank references
            generateBankReferences(payment);
            
            // Update aggregated information
            updateAggregatedInformation(payment);
            
            return payment;
        })
        .writer(items -> {
            ExecutionContext jobContext = stepBuilder.getJobRepository()
                .getLastJobExecution(routeConfig.getRouteName(), new JobParameters())
                .getExecutionContext();
                
            jobContext.put("processedPayments", items);
        })
        .build();
}

private Step createPwsTransactionInsertStep(StepBuilder stepBuilder, RouteConfig routeConfig) {
    return stepBuilder.<PaymentInformation, PaymentInformation>chunk(100, platformTransactionManager)
        .reader(new ListItemReader<>((List<PaymentInformation>) 
            stepBuilder.getJobRepository()
                      .getLastJobExecution(routeConfig.getRouteName(), new JobParameters())
                      .getExecutionContext()
                      .get("processedPayments")))
        .processor(payment -> {
            // Create and populate PwsTransactions
            PwsTransactions pwsTransaction = createPwsTransaction(payment);
            
            // Create and populate PwsBulkTransactions
            PwsBulkTransactions pwsBulkTransaction = createPwsBulkTransaction(payment);
            
            // Set the relationships
            payment.setPwsTransactions(pwsTransaction);
            payment.setPwsBulkTransactions(pwsBulkTransaction);
            
            return payment;
        })
        .writer(items -> {
            // Persist PwsTransactions and PwsBulkTransactions
            persistTransactions(items);
        })
        .build();
}

private Step createPwsInstructionInsertStep(StepBuilder stepBuilder, RouteConfig routeConfig) {
    return stepBuilder.<CreditTransferTransaction, CreditTransferTransaction>chunk(1000, platformTransactionManager)
        .reader(new CreditTransferTransactionReader(stepBuilder.getJobRepository()
                .getLastJobExecution(routeConfig.getRouteName(), new JobParameters())))
        .processor(transaction -> {
            // Create and populate PwsBulkTransactionInstructions
            PwsBulkTransactionInstructions instructions = createInstructions(transaction);
            
            // Create and populate PwsParties
            PwsParties parties = createParties(transaction);
            
            // Create and populate PwsTransactionAdvices
            PwsTransactionAdvices advices = createAdvices(transaction);
            
            // Create and populate PwsTransactionCharges
            PwsTransactionCharges charges = createCharges(transaction);
            
            // Set all created objects to transaction
            transaction.setPwsBulkTransactionInstructions(instructions);
            transaction.setPwsParties(parties);
            transaction.setPwsTransactionAdvices(advices);
            transaction.setPwsTransactionCharges(charges);
            
            return transaction;
        })
        .writer(items -> {
            // Persist all instruction related objects
            persistInstructions(items);
        })
        .build();
}

// Helper methods for processing steps
private void validatePain001FileLevel(Pain001 pain001) {
    // Validate message identification
    // Validate creation date time
    // Validate number of transactions
    // Validate control sum
    // Validate initiating party
    if (!isValid(pain001)) {
        throw new BulkProcessingException("Invalid PAIN.001 file", new Throwable("Validation failed"));
    }
}

private void validatePaymentInformation(PaymentInformation payment) {
    // Validate payment method
    // Validate execution date
    // Validate debtor information
    // Validate debtor account
    // Validate debtor agent
    if (!isValid(payment)) {
        throw new BulkProcessingException("Invalid payment information", new Throwable("Validation failed"));
    }
}

private List<PaymentInformation> splitPaymentInformation(
    PaymentInformation payment, 
    BigDecimal maxAmount, 
    int maxTransactions
) {
    List<PaymentInformation> splitPayments = new ArrayList<>();
    List<CreditTransferTransaction> transactions = new ArrayList<>(payment.getCreditTransferTransactions());
    
    PaymentInformation currentBatch = copyPaymentInformationWithoutTransactions(payment);
    BigDecimal currentBatchAmount = BigDecimal.ZERO;
    int currentTransactionCount = 0;
    
    for (CreditTransferTransaction transaction : transactions) {
        if (currentBatchAmount.add(transaction.getAmount()).compareTo(maxAmount) > 0 
            || currentTransactionCount >= maxTransactions) {
            splitPayments.add(currentBatch);
            currentBatch = copyPaymentInformationWithoutTransactions(payment);
            currentBatchAmount = BigDecimal.ZERO;
            currentTransactionCount = 0;
        }
        
        currentBatch.getCreditTransferTransactions().add(transaction);
        currentBatchAmount = currentBatchAmount.add(transaction.getAmount());
        currentTransactionCount++;
    }
    
    if (!currentBatch.getCreditTransferTransactions().isEmpty()) {
        splitPayments.add(currentBatch);
    }
    
    return splitPayments;
}
	
    

}
```

# pain001
```java
@NoArgsConstructor
@Data
public class Pain001 {
    @JsonProperty("businessDocument")
    private BusinessDocumentDTO businessDocument;
}

@NoArgsConstructor
@Data
public class BusinessDocumentDTO {
    @JsonProperty("customerCreditTransferInitiation")
    private CustomerCreditTransferInitiationDTO customerCreditTransferInitiation;
}

@NoArgsConstructor
 @Data
 public class CustomerCreditTransferInitiationDTO {
     @JsonProperty("groupHeader")
     private GroupHeaderDTO groupHeader;
     @JsonProperty("paymentInformation")
     private List<PaymentInformationDTO> paymentInformation;
 }
```

```java
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.annotation.AfterStep;

public abstract class StepAwareService {

    protected StepExecution stepExecution;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @AfterStep
    public void afterStep(StepExecution stepExecution) {
        // Subclasses can optionally override this method
    }

    // Helper method to access JobExecutionContext
    protected JobExecution getJobExecution() {
        return stepExecution.getJobExecution();
    }

    // Helper method to access StepExecutionContext
    protected StepExecution getStepExecution() {
        return this.stepExecution;
    }

    protected String getFromStepContext(String key, String defaultValue) {
        return stepExecution.getExecutionContext().getString(key, defaultValue);
    }

    protected void putToStepContext(String key, String value) {
        stepExecution.getExecutionContext().putString(key, value);
    }
    
    protected String getFromJobContext(String key, String defaultValue) {
        return getJobExecution().getExecutionContext().getString(key, defaultValue);
    }

    protected void putToJobContext(String key, String value) {
        getJobExecution().getExecutionContext().putString(key, value);
    }

    // Abstract method to be implemented by subclasses for specific service logic
    public abstract void performService();
}

```


# payment info
```java

@Data
public class PaymentInformantion {

    PwsTransactions pwsTransactions;
    PwsBulkTransactions pwsBulkTransactions;

    List<CreditTransferTransaction> creditTransferTransactions;

}
```

# credit transfer txn info
```java
 @Data
 public class CreditTransferTransaction {

     PwsBulkTransactionInstructions pwsBulkTransactionInstructions;
     PwsParties pwsParties;
     PwsTransactionAdvices pwsTransactionAdvices;
     PwsTransactionCharges pwsTransactionCharges;
     
}
```
