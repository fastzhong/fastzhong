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

```yml
server:
  port: ${pbp_port}
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
        maximum-pool-size: 5
        minimum-idle: 2
        idle-timeout: 300000
        connection-timeout: 20000
        max-lifetime: 1800000
      vault:
        enabled: false
        key-store: /path/to/keystore.jks
        key-store-password: keystorePassword
        key-store-type: JKS
        trust-store: /path/to/truststore.jks
        trust-store-password: truststorePassword
        wallet-location: /path/to/wallet

    # Default datasource for Spring Batch and pws
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
      - file-validation
      - group-validation
      - paymentInfo-splitting
      - paymentInfo-validation
      - paymentInfo-enrichment
      - transaction-validation
      - transaction-enrichment
      - pws-computation
      - pws-insertion
      - notification
      - file-archive
    file-source:
      path: /path/to/inbound/files
      file-patterns: *.json
      interval: 60000  # 60 seconds
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
    destination-type: FILE
    enabled: true
    steps:
      - pws-loading
      - pain001-transformation
    pws-loading:
      datasource: datasource-pws-loading
      cron: 0 0/15 * * * ?  # Every 15 minutes
    pain001-transformation:
      template-path: /path/to/pain001/template.xml
    file-destination:
      path: /path/to/outbound/files
```

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

```java
package com.example.paymentprocessing.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class DataSourceConfig {

    private final Environment env;

    public DataSourceConfig(Environment env) {
        this.env = env;
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.default")
    public DataSource defaultDataSource() {
        return createDataSource("spring.datasource.default");
    }

    @Bean
    @ConfigurationProperties("spring.datasource.pws-insertion")
    public DataSource pwsInsertionDataSource() {
        return createDataSource("spring.datasource.pws-insertion");
    }

    @Bean
    @ConfigurationProperties("spring.datasource.pws-loading")
    public DataSource pwsLoadingDataSource() {
        return createDataSource("spring.datasource.pws-loading");
    }

    private DataSource createDataSource(String prefix) {
        HikariDataSource dataSource = DataSourceBuilder.create().type(HikariDataSource.class).build();
        
        // Set common properties
        dataSource.setDriverClassName(env.getProperty("spring.datasource.common.driver-class-name"));
        dataSource.setJdbcUrl(env.getProperty(prefix + ".jdbc-url"));
        dataSource.setUsername(env.getProperty(prefix + ".username"));
        dataSource.setPassword(env.getProperty(prefix + ".password"));
        
        // Set Hikari-specific properties
        dataSource.setMaximumPoolSize(env.getProperty("spring.datasource.common.hikari.maximum-pool-size", Integer.class));
        dataSource.setMinimumIdle(env.getProperty("spring.datasource.common.hikari.minimum-idle", Integer.class));
        dataSource.setIdleTimeout(env.getProperty("spring.datasource.common.hikari.idle-timeout", Long.class));
        dataSource.setConnectionTimeout(env.getProperty("spring.datasource.common.hikari.connection-timeout", Long.class));
        dataSource.setMaxLifetime(env.getProperty("spring.datasource.common.hikari.max-lifetime", Long.class));
        
        // Configure Oracle Vault if enabled
        if (Boolean.parseBoolean(env.getProperty("spring.datasource.common.vault.enabled"))) {
            Properties props = new Properties();
            props.setProperty("oracle.net.wallet_location", env.getProperty("spring.datasource.common.vault.wallet-location"));
            props.setProperty("javax.net.ssl.keyStore", env.getProperty("spring.datasource.common.vault.key-store"));
            props.setProperty("javax.net.ssl.keyStorePassword", env.getProperty("spring.datasource.common.vault.key-store-password"));
            props.setProperty("javax.net.ssl.keyStoreType", env.getProperty("spring.datasource.common.vault.key-store-type"));
            props.setProperty("javax.net.ssl.trustStore", env.getProperty("spring.datasource.common.vault.trust-store"));
            props.setProperty("javax.net.ssl.trustStorePassword", env.getProperty("spring.datasource.common.vault.trust-store-password"));
            
            dataSource.setDataSourceProperties(props);
        }
        
        return dataSource;
    }
}
```

```java
package com.example.paymentprocessing.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@MapperScan(basePackages = "com.example.paymentprocessing.mapper")
public class MyBatisConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource-pws-insertion")
    public DataSource pwsInsertionDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource-pws-loading")
    public DataSource pwsLoadingDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @Primary
    public SqlSessionFactory primarySqlSessionFactory(@Qualifier("primaryDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mappers/**/*.xml"));
        return sessionFactory.getObject();
    }

    @Bean
    public SqlSessionFactory pwsInsertionSqlSessionFactory(@Qualifier("pwsInsertionDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mappers/pwsinsertion/**/*.xml"));
        return sessionFactory.getObject();
    }

    @Bean
    public SqlSessionFactory pwsLoadingSqlSessionFactory(@Qualifier("pwsLoadingDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mappers/pwsloading/**/*.xml"));
        return sessionFactory.getObject();
    }
}
```

```java
package com.example.paymentprocessing.config;

import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.MapJobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
public class BatchConfig extends DefaultBatchConfigurer {

    @Value("${spring.batch.job-repository.initialize:false}")
    private boolean shouldInitializeJobRepository;

    private final DataSource dataSource;

    public BatchConfig(DataSource dataSource) {
        // This will be the default datasource if it exists
        this.dataSource = dataSource;
    }

    @Override
    public void setDataSource(DataSource dataSource) {
        // Override to use no datasource if initialization is disabled
        if (shouldInitializeJobRepository) {
            super.setDataSource(dataSource);
        }
    }

    @Override
    protected JobRepository createJobRepository() throws Exception {
        if (shouldInitializeJobRepository) {
            return super.createJobRepository();
        } else {
            MapJobRepositoryFactoryBean factoryBean = new MapJobRepositoryFactoryBean();
            factoryBean.afterPropertiesSet();
            return factoryBean.getObject();
        }
    }

    @Override
    protected JobExplorer createJobExplorer() throws Exception {
        if (shouldInitializeJobRepository) {
            return super.createJobExplorer();
        } else {
            MapJobExplorerFactoryBean factoryBean = new MapJobExplorerFactoryBean(createJobRepository());
            factoryBean.afterPropertiesSet();
            return factoryBean.getObject();
        }
    }

    @Bean
    public JobLauncher jobLauncher() throws Exception {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(createJobRepository());
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}
```

```java
package com.example.paymentprocessing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "bulk-routes")
@Data
public class BulkRoutesConfig {
    private List<RouteConfig> routes;

    @Data
    public static class RouteConfig {
        private String routeName;
        private ProcessingType processingType;
        private SourceDestinationType sourceType;
        private SourceDestinationType destinationType;
        private boolean enabled;
        private List<String> steps;
        private FileSource fileSource;
        private PwsInsertion pwsInsertion;
        private Notification notification;
        private FileArchive fileArchive;
        private PwsLoading pwsLoading;
        private Pain001Transformation pain001Transformation;
        private FileDestination fileDestination;
    }

    @Data
    public static class FileSource {
        private String path;
        private String filePatterns;
        private int interval;
    }

    @Data
    public static class PwsInsertion {
        private String datasource;
    }

    @Data
    public static class Notification {
        private Email email;

        @Data
        public static class Email {
            private String host;
            private int port;
            private String username;
            private String password;
        }
    }

    @Data
    public static class FileArchive {
        private String path;
    }

    @Data
    public static class PwsLoading {
        private String datasource;
        private String cron;
    }

    @Data
    public static class Pain001Transformation {
        private String templatePath;
    }

    @Data
    public static class FileDestination {
        private String path;
    }

    @Getter
    public enum ProcessingType {
        INBOUND, OUTBOUND
    }

    @Getter
    public enum SourceDestinationType {
        FILE, JDBC, MESSAGE, API
    }
}
```

```java
package com.uob.pwb.pbp.config;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.uob.pwb.pbp.config.BulkRoutesConfig.RouteConfig;
import com.uob.pwb.pbp.config.BulkRoutesConfig.ProcessingType;

import org.apache.camel.Exchange;

@Configuration
public class BulkProcessingFlowBuilder extends RouteBuilder {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BulkRoutesConfig bulkRoutesConfig;

    @Autowired
    private JobLauncher jobLauncher;

    @Override
    public void configure() throws Exception {
        for (RouteConfig routeConfig : bulkRoutesConfig.getRoutes()) {
            if (routeConfig.isEnabled()) {
                configureRoute(routeConfig);
            }
        }
    }

    private void configureRoute(RouteConfig routeConfig) {
        if (routeConfig.getProcessingType() == ProcessingType.INBOUND) {
            from(buildInboundFromUri(routeConfig))
                .routeId(routeConfig.getRouteName())
                .process(exchange -> {
                    Job job = createJob(routeConfig);
                    jobLauncher.run(job, createJobParameters(exchange, routeConfig));
                })
                .to(buildInboundToUri(routeConfig));
        } else { // OUTBOUND
            from(buildOutboundFromUri(routeConfig))
                .routeId(routeConfig.getRouteName())
                .process(exchange -> {
                    Job job = createJob(routeConfig);
                    jobLauncher.run(job, createJobParameters(exchange, routeConfig));
                })
                .to(buildOutboundToUri(routeConfig));
        }
    }

    private String buildInboundFromUri(RouteConfig routeConfig) {
        return String.format("file:%s?include=%s&delay=%d",
            routeConfig.getFileSource().getPath(),
            routeConfig.getFileSource().getFilePatterns(),
            routeConfig.getFileSource().getInterval());
    }

    private String buildInboundToUri(RouteConfig routeConfig) {
        return "direct:fileArchive";
    }

    private String buildOutboundFromUri(RouteConfig routeConfig) {
        return String.format("quartz://pwsLoadTimer?cron=%s", routeConfig.getPwsLoading().getCron());
    }

    private String buildOutboundToUri(RouteConfig routeConfig) {
        return "file:" + routeConfig.getFileDestination().getPath();
    }

    private JobParameters createJobParameters(Exchange exchange, RouteConfig routeConfig) {
        return new JobParametersBuilder()
            .addString("routeName", routeConfig.getRouteName())
            .addString("fileName", exchange.getIn().getHeader("CamelFileName", String.class))
            .addLong("time", System.currentTimeMillis())
            .toJobParameters();
    }

    private Job createJob(RouteConfig routeConfig) {
        JobBuilder jobBuilder = jobBuilderFactory.get(routeConfig.getRouteName() + "Job");

        for (String stepName : routeConfig.getSteps()) {
            Step step = createStepForName(stepName, routeConfig);
            jobBuilder = jobBuilder.next(step);
        }

        return jobBuilder.build();
    }

    private Step createStepForName(String stepName, RouteConfig routeConfig) {
        switch (stepName) {
            case "file-validation":
                return createFileValidationStep(routeConfig);
            case "group-validation":
                return createGroupValidationStep(routeConfig);
            case "paymentInfo-splitting":
                return createPaymentInfoSplittingStep(routeConfig);
            case "paymentInfo-validation":
                return createPaymentInfoValidationStep(routeConfig);
            case "paymentInfo-enrichment":
                return createPaymentInfoEnrichmentStep(routeConfig);
            case "transaction-validation":
                return createTransactionValidationStep(routeConfig);
            case "transaction-enrichment":
                return createTransactionEnrichmentStep(routeConfig);
            case "pws-computation":
                return createPwsComputationStep(routeConfig);
            case "pws-insertion":
                return createPwsInsertionStep(routeConfig);
            case "notification":
                return createNotificationStep(routeConfig);
            case "file-archive":
                return createFileArchiveStep(routeConfig);
            case "pws-loading":
                return createPwsLoadingStep(routeConfig);
            case "pain001-transformation":
                return createPain001TransformationStep(routeConfig);
            default:
                throw new IllegalArgumentException("Unknown step: " + stepName);
        }
    }

    // Individual step creation methods (implement these based on your requirements)
    private Step createFileValidationStep(RouteConfig routeConfig) {
        return stepBuilderFactory.get("fileValidationStep")
            .tasklet((contribution, chunkContext) -> {
                // Implement file validation logic
                return null;
            })
            .build();
    }

    // Implement other step creation methods similarly
    // ...

    // Define readers, processors, writers here if needed
    // ...
}
```

```java
package com.example.paymentprocessing.route;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.paymentprocessing.config.BulkRoutesConfig;
import com.example.paymentprocessing.config.BulkRoutesConfig.RouteConfig;
import com.example.paymentprocessing.config.BulkRoutesConfig.ProcessingType;
import com.example.paymentprocessing.config.BulkRoutesConfig.SourceDestinationType;

@Component
public class DynamicRouteBuilder extends RouteBuilder {

    @Autowired
    private BulkRoutesConfig bulkRoutesConfig;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Map<String, Job> jobs;

    @Override
    public void configure() throws Exception {
        for (RouteConfig routeConfig : bulkRoutesConfig.getRoutes()) {
            if (routeConfig.isEnabled()) {
                configureRoute(routeConfig);
            }
        }
    }

    private void configureRoute(RouteConfig routeConfig) {
        String jobName = routeConfig.getRouteName() + "Job";

        if (routeConfig.getProcessingType() == ProcessingType.INBOUND) {
            from(buildInboundFromUri(routeConfig))
                .routeId(routeConfig.getRouteName())
                .process(exchange -> {
                    Job job = jobs.get(jobName);
                    JobParameters jobParameters = new JobParametersBuilder()
                        .addString("fileName", exchange.getIn().getHeader("CamelFileName", String.class))
                        .addString("routeName", routeConfig.getRouteName())
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters();
                    jobLauncher.run(job, jobParameters);
                });
        } else { // OUTBOUND
            from("quartz://pwsLoadTimer?cron=" + routeConfig.getPwsLoading().get("cron"))
                .routeId(routeConfig.getRouteName())
                .process(exchange -> {
                    Job job = jobs.get(jobName);
                    JobParameters jobParameters = new JobParametersBuilder()
                        .addString("routeName", routeConfig.getRouteName())
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters();
                    jobLauncher.run(job, jobParameters);
                })
                .to(buildOutboundToUri(routeConfig));
        }
    }

    private String buildInboundFromUri(RouteConfig routeConfig) {
        switch (routeConfig.getSourceType()) {
            case FILE:
                return String.format("file:%s?include=%s&delay=%d",
                    routeConfig.getFileSource().get("path"),
                    routeConfig.getFileSource().get("file-patterns"),
                    routeConfig.getFileSource().get("interval"));
            case JDBC:
                return "jdbc://select * from source_table"; // Placeholder, adjust as needed
            case MESSAGE:
                return "jms:queue:sourceQueue"; // Placeholder, adjust as needed
            case API:
                return "rest:get:api/source"; // Placeholder, adjust as needed
            default:
                throw new IllegalArgumentException("Unsupported source type: " + routeConfig.getSourceType());
        }
    }

    private String buildOutboundToUri(RouteConfig routeConfig) {
        switch (routeConfig.getDestinationType()) {
            case FILE:
                return String.format("file:%s", routeConfig.getFileDestination().get("path"));
            case JDBC:
                return "jdbc:destinationDataSource"; // Placeholder, adjust as needed
            case MESSAGE:
                return "jms:queue:destinationQueue"; // Placeholder, adjust as needed
            case API:
                return "rest:post:api/destination"; // Placeholder, adjust as needed
            default:
                throw new IllegalArgumentException("Unsupported destination type: " + routeConfig.getDestinationType());
        }
    }
}

package com.example.paymentprocessing.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class Pain001BatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private ErrorHandlingListener errorHandlingListener;

    @Bean
    public Job pain001ProcessingJob() {
        return jobBuilderFactory.get("pain001ProcessingJob")
                .start(pain001ProcessingFlow())
                .listener(errorHandlingListener)
                .build();
    }

    @Bean
    public Flow pain001ProcessingFlow() {
        return new FlowBuilder<SimpleFlow>("pain001ProcessingFlow")
                .start(groupValidationStep())
                .next(paymentInfoSplittingStep())
                .next(transactionProcessingStep())
                .next(pwsComputationStep())
                .next(pwsInsertionStep())
                .next(notificationStep())
                .build();
    }

    @Bean
    public Step groupValidationStep() {
        return stepBuilderFactory.get("groupValidationStep")
                .tasklet((contribution, chunkContext) -> {
                    // Implement group validation logic
                    boolean validationPassed = true;
                    if (!validationPassed && isErrorOnFileEnabled()) {
                        throw new ValidationException("Group validation failed");
                    }
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step paymentInfoSplittingStep() {
        return stepBuilderFactory.get("paymentInfoSplittingStep")
                .tasklet((contribution, chunkContext) -> {
                    // Implement payment info splitting logic
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step transactionProcessingStep() {
        FaultTolerantStepBuilder<Transaction, Transaction> builder = stepBuilderFactory.get("transactionProcessingStep")
                .<Transaction, Transaction>chunk(1000)
                .reader(transactionReader())
                .processor(compositeTransactionProcessor())
                .writer(transactionWriter())
                .faultTolerant()
                .retry(RetryableException.class)
                .retryLimit(3)
                .skip(SkippableException.class)
                .skipLimit(10)
                .listener(new TransactionProcessingListener())
                .taskExecutor(taskExecutor())
                .throttleLimit(20);

        if (isErrorOnFileEnabled()) {
            builder.completionPolicy(new ErrorAwareCompletionPolicy());
        }

        return builder.build();
    }

    @Bean
    public Step pwsComputationStep() {
        return stepBuilderFactory.get("pwsComputationStep")
                .tasklet((contribution, chunkContext) -> {
                    // Implement PWS computation logic
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step pwsInsertionStep() {
        return stepBuilderFactory.get("pwsInsertionStep")
                .<PwsRecord, PwsRecord>chunk(1000)
                .reader(pwsRecordReader())
                .writer(pwsRecordWriter())
                .faultTolerant()
                .retry(RetryableException.class)
                .retryLimit(3)
                .skip(SkippableException.class)
                .skipLimit(10)
                .taskExecutor(taskExecutor())
                .throttleLimit(10)
                .build();
    }

    @Bean
    public Step notificationStep() {
        return stepBuilderFactory.get("notificationStep")
                .tasklet((contribution, chunkContext) -> {
                    // Implement notification logic
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("pain001-");
        executor.initialize();
        return executor;
    }

    @Bean
    public CompositeItemProcessor<Transaction, Transaction> compositeTransactionProcessor() {
        CompositeItemProcessor<Transaction, Transaction> processor = new CompositeItemProcessor<>();
        processor.setDelegates(Arrays.asList(
            transactionValidator(),
            transactionEnricher()
        ));
        return processor;
    }

    private boolean isErrorOnFileEnabled() {
        // Implement logic to check if errorOnFile setting is enabled
        return true; // Placeholder
    }

    // Other beans (readers, processors, writers) ...
}

class ErrorAwareCompletionPolicy implements CompletionPolicy {
    private boolean errorOccurred = false;

    @Override
    public boolean isComplete(RepeatContext context, RepeatStatus result) {
        return errorOccurred || RepeatStatus.FINISHED.equals(result);
    }

    @Override
    public boolean isComplete(RepeatContext context) {
        return errorOccurred;
    }

    @Override
    public RepeatContext start(RepeatContext parent) {
        return parent;
    }

    @Override
    public void update(RepeatContext context) {
        // This method can be called to set errorOccurred to true when an error is detected
    }

    public void setErrorOccurred(boolean errorOccurred) {
        this.errorOccurred = errorOccurred;
    }
}

class TransactionProcessingListener implements ItemProcessListener<Transaction, Transaction>,
                                              ItemWriteListener<Transaction> {
    @Autowired
    private ErrorAwareCompletionPolicy completionPolicy;

    @Override
    public void onProcessError(Transaction item, Exception e) {
        if (isErrorOnFileEnabled()) {
            completionPolicy.setErrorOccurred(true);
        }
    }

    @Override
    public void onWriteError(Exception exception, List<? extends Transaction> items) {
        if (isErrorOnFileEnabled()) {
            completionPolicy.setErrorOccurred(true);
        }
    }

    // Other interface methods...
}
    @Bean
    public Map<String, Job> jobs() {
        Map<String, Job> jobs = new HashMap<>();

        for (RouteConfig routeConfig : bulkRoutesConfig.getRoutes()) {
            if (routeConfig.isEnabled()) {
                String jobName = routeConfig.getRouteName() + "Job";
                jobs.put(jobName, createJob(routeConfig));
            }
        }

        return jobs;
    }

    private Job createJob(RouteConfig routeConfig) {
        JobBuilder jobBuilder = jobBuilderFactory.get(routeConfig.getRouteName() + "Job");

        for (String stepName : routeConfig.getSteps()) {
            Step step = steps.get(stepName + "Step");
            if (step != null) {
                jobBuilder = jobBuilder.next(step);
            }
        }

        return jobBuilder.build();
    }

    // Define step beans for each processing step
    @Bean
    public Step fileValidationStep() {
        return stepBuilderFactory.get("fileValidationStep")
            .tasklet((contribution, chunkContext) -> {
                // Implement file validation logic
                return RepeatStatus.FINISHED;
            })
            .build();
    }

    @Bean
    public Step groupValidationStep() {
        // Implement group validation step
    }

    @Bean
    public Step paymentInfoValidationStep() {
        // Implement paymentInfo validation step
    }

    @Bean
    public Step paymentInfoEnrichmentStep() {
        // Implement paymentInfo enrichment step
    }

    @Bean
    public Step transactionValidationStep() {
        // Implement transaction validation step
    }

    @Bean
    public Step transactionEnrichmentStep() {
        // Implement transaction enrichment step
    }

    @Bean
    public Step pwsComputationStep() {
        // Implement PWS computation step
    }

    @Bean
    public Step pwsInsertionStep() {
        // Implement PWS insertion step
    }

    @Bean
    public Step notificationStep() {
        // Implement notification step
    }

    @Bean
    public Step fileArchiveStep() {
        // Implement file archive step
    }

    @Bean
    public Step pwsLoadingStep() {
        return stepBuilderFactory.get("pwsLoadingStep")
            .<PwsPayment, PwsPayment>chunk(10)
            .reader(pwsPaymentReader())
            .processor(pwsPaymentProcessor())
            .writer(pwsPaymentWriter())
            .build();
    }

    @Bean
    public Step pain001TransformationStep() {
        return stepBuilderFactory.get("pain001TransformationStep")
            .<PwsPayment, Pain001Payment>chunk(10)
            .reader(pwsPaymentReader())
            .processor(pain001TransformProcessor())
            .writer(pain001Writer())
            .build();
    }

    // Define readers, processors, and writers
    @Bean
    public JdbcCursorItemReader<PwsPayment> pwsPaymentReader() {
        // Configure to read from PWS tables
    }

    @Bean
    public ItemProcessor<PwsPayment, PwsPayment> pwsPaymentProcessor() {
        // Process PWS payments if needed
    }

    @Bean
    public ItemWriter<PwsPayment> pwsPaymentWriter() {
        // Write processed PWS payments if needed
    }

    @Bean
    public ItemProcessor<PwsPayment, Pain001Payment> pain001TransformProcessor() {
        // Transform PWS payment to Pain001 format
    }

    @Bean
    public ItemWriter<Pain001Payment> pain001Writer() {
        // Write Pain001 payments to the configured destination
    }

    @Value("${spring.batch.job-repository.initialize:false}")
    private boolean shouldInitializeJobRepository;

    private final DataSource dataSource;

    public BatchConfig(DataSource dataSource) {
        // This will be the default datasource if it exists
        this.dataSource = dataSource;
    }

    @Override
    public void setDataSource(DataSource dataSource) {
        // Override to use no datasource if initialization is disabled
        if (shouldInitializeJobRepository) {
            super.setDataSource(dataSource);
        }
    }

    @Override
    protected JobRepository createJobRepository() throws Exception {
        if (shouldInitializeJobRepository) {
            return super.createJobRepository();
        } else {
            MapJobRepositoryFactoryBean factoryBean = new MapJobRepositoryFactoryBean();
            factoryBean.afterPropertiesSet();
            return factoryBean.getObject();
        }
    }

    @Override
    protected JobExplorer createJobExplorer() throws Exception {
        if (shouldInitializeJobRepository) {
            return super.createJobExplorer();
        } else {
            MapJobExplorerFactoryBean factoryBean = new MapJobExplorerFactoryBean(createJobRepository());
            factoryBean.afterPropertiesSet();
            return factoryBean.getObject();
        }
    }

    @Bean
    public JobLauncher jobLauncher() throws Exception {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(createJobRepository());
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

}
```
