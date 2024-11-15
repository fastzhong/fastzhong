# maven

```shell
$ mvn install:install-file -Dfile=C:/zhonglun/isprint-xmlrpc-3.1.3.20190815.jar -DgroupId=org.apache.xmlrpc -DartifactId=isprint-xmlrpc -Dversion=3.1.3.20190815 -Dpackaging=jar
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.uob.gwb.pws.pbp</groupId>
    <artifactId>payment-bulk-processing</artifactId>
    <version>1.0-SNAPSHOT</version>
    <name>payment-bulk-processing</name>
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
        <ojdbc.version>19.3.0.0</ojdbc.version>
        <sonar.coverage.exclusions>
            **/*Test.java,**/aggregation/**,**/api/**,**/bo/**,**/po/**,**/status/**,**/validation/**,**/config/**,**/constant/**,**/dao/** */,**/domain/**,**/domestic/**,**/dto/**,**/mapper/**,**/model/**
        </sonar.coverage.exclusions>
        <sonar.cpd.exclusions>
            **/*Test.java,**/aggregation/**,**/api/**,**/bo/**,**/po/**,**/status/**,**/validation/**,**/config/**,**/constant/**,**/dao/** */,**/domain/**,**/domestic/**,**/dto/**,**/mapper/**,**/model/**
        </sonar.cpd.exclusions>
        <sonar.exclusions>**/api/*.java</sonar.exclusions>
        <spotless.plugin.version>2.26.0</spotless.plugin.version>
        <common-utils.version>2.0.2</common-utils.version>
        <!-- cew -->
        <cew-feature-toggle-starter.version>1.0.10</cew-feature-toggle-starter.version>
        <transaction-common.version>6.0.106</transaction-common.version>
        <payment-pre-processing-lib.version>1.0.36</payment-pre-processing-lib.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>com.apache.xmlrpc</groupId>
            <artifactId>isprint-xmlrpc</artifactId>
            <version>3.1.3.20190815</version>
        </dependency>
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
            <version>${ojdbc.version}</version>
        </dependency>
        <dependency>
            <groupId>com.oracle.ojdbc</groupId>
            <artifactId>osdt_cert</artifactId>
            <version>${ojdbc.version}</version>
        </dependency>
        <dependency>
            <groupId>com.oracle.ojdbc</groupId>
            <artifactId>osdt_core</artifactId>
            <version>${ojdbc.version}</version>
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
        <dependency>
            <groupId>org.drools</groupId>
            <artifactId>drools-compiler</artifactId>
            <version>${drools.version}</version>
        </dependency>
        <dependency>
            <groupId>org.kie</groupId>
            <artifactId>kie-api</artifactId>
            <version>${drools.version}</version>
        </dependency>
        <!-- misc -->
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
        <!-- CEW -->
        <dependency>
            <groupId>com.uob.gwb</groupId>
            <artifactId>common-utils</artifactId>
            <version>${common-utils.version}</version>
        </dependency>
        <dependency>
            <groupId>com.uob.gwb</groupId>
            <artifactId>cew-feature-toggle-starter</artifactId>
            <version>${cew-feature-toggle-starter.version}</version>
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
            <groupId>com.uob.gwb.pre.processing</groupId>
            <artifactId>payment-pre-processing-lib</artifactId>
            <version>${payment-pre-processing-lib.version}</version>
        </dependency>
        <!-- compile -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>compile</scope>
        </dependency>
        <!-- test -->
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-inline</artifactId>
            <version>4.11.0</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.google.code.bean-matchers</groupId>
            <artifactId>bean-matchers</artifactId>
            <version>${bean-matchers.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
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

```shell
[INFO] --------------------------------[ jar ]---------------------------------
Downloading from virtual-maven: https://artifactoryp.sg.uobnet.com/artifactory/virtual-maven/com/apache/xmlrpc/isprint-xmlrpc/3.1.3.20190815/isprint-xmlrpc-3.1.3.2
0190815.pom
Downloading from central: https://repo.maven.apache.org/maven2/com/apache/xmlrpc/isprint-xmlrpc/3.1.3.20190815/isprint-xmlrpc-3.1.3.20190815.pom
Downloading from virtual-maven: https://artifactoryp.sg.uobnet.com/artifactory/virtual-maven/org/apache/xmlrpc/xmlrpc/3.1.3-isprint-20190815/xmlrpc-3.1.3-isprint-2
0190815.pom
Downloading from central: https://repo.maven.apache.org/maven2/org/apache/xmlrpc/xmlrpc/3.1.3-isprint-20190815/xmlrpc-3.1.3-isprint-20190815.pom
Downloading from virtual-maven: https://artifactoryp.sg.uobnet.com/artifactory/virtual-maven/commons-httpclient/commons-httpclient/3.1.0.1265/commons-httpclient-3.
1.0.1265.pom
Downloading from central: https://repo.maven.apache.org/maven2/commons-httpclient/commons-httpclient/3.1.0.1265/commons-httpclient-3.1.0.1265.pom
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  12.791 s
[INFO] Finished at: 2024-11-16T02:35:50+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal on project payment-bulk-processing: Could not resolve dependencies for project com.uob.gwb.pws.pbp:payment-bulk-processing:jar:1.0-S
NAPSHOT: Failed to collect dependencies at com.apache.xmlrpc:isprint-xmlrpc:jar:3.1.3.20190815: Failed to read artifact descriptor for com.apache.xmlrpc:isprint-xm
lrpc:jar:3.1.3.20190815: The following artifacts could not be resolved: com.apache.xmlrpc:isprint-xmlrpc:pom:3.1.3.20190815 (absent): Could not transfer artifact c
om.apache.xmlrpc:isprint-xmlrpc:pom:3.1.3.20190815 from/to central (https://repo.maven.apache.org/maven2): No such host is known (repo.maven.apache.org) -> [Help 1
]
```

# rbk

```
read.account.attributes=select ACCOUNT_NUMBER, ACCOUNT_TYPE, CUR_CODE,OWNER_TYPE from aes_Account_attributes where account_type in ('01','03')
batch.read.count=select count(*) from aes_Account_attributes
sync.account.attributes.update.query=MERGE into aes_account_attributes A Using (select * from ( SELECT abc.*, DECODE(abc.cew_acc_id, NULL, abc.account_id, abc.cew_acc_id) cew_account_id FROM ( SELECT mv.*, aai.account_id cew_acc_id FROM geb_account_data mv LEFT JOIN aes_account_alternate_identifiers aai ON aai.id_value = mv.account_id ) abc ) xyz where xyz.cew_account_id  not in (select ACCOUNT_ID from (select ACCOUNT_ID, count(ID_VALUE) from aes_account_alternate_identifiers  group by ACCOUNT_ID having count(ID_VALUE)>1))) b on (a.ACCOUNT_ID = b.cew_account_id) WHEN MATCHED THEN UPDATE SET A.BRCH_CODE = B.BRCH_CODE,A.ACCOUNT_TYPE = B.ACCOUNT_TYPE, A.ADDRESS_LINE_1 = B.ADDRESS_LINE_1, A.ADDRESS_LINE_2 = B.ADDRESS_LINE_2, A.COUNTRY = B.COUNTRY, A.BANK_NAME = B.BANK_NAME, A.BANK_ADDRESS_LINE_1 = B.BANK_ADDRESS_LINE_1, A.BANK_ADDRESS_LINE_2 = B.BANK_ADDRESS_LINE_2, A.CUR_CODE = B.CUR_CODE, A.ROUTING_BIC = B.ROUTING_BIC, A.ACTV_FLAG = B.ACTV_FLAG, A.BANK_ID = B.BANK_ID,A.ACCOUNT_NAME = B.ACCT_NAME, A.description = B.description, A.account_number = B.account_no, A.UPDATED_BY  = 'SYSTEM', A.UPDATED_DATE = SYSDATE where A.BRCH_CODE <> B.BRCH_CODE or  A.ACCOUNT_TYPE <> B.ACCOUNT_TYPE or  A.ADDRESS_LINE_1 <> B.ADDRESS_LINE_1 or  A.ADDRESS_LINE_2 <> B.ADDRESS_LINE_2 or  A.COUNTRY <> B.COUNTRY or  A.BANK_NAME <> B.BANK_NAME or  A.BANK_ADDRESS_LINE_1 <> B.BANK_ADDRESS_LINE_1 or  A.BANK_ADDRESS_LINE_2 <> B.BANK_ADDRESS_LINE_2 or  A.CUR_CODE <> B.CUR_CODE or  A.ROUTING_BIC <> B.ROUTING_BIC or  A.ACTV_FLAG <> B.ACTV_FLAG or  A.BANK_ID <> B.BANK_ID or  A.ACCOUNT_NAME <> B.ACCT_NAME or  A.description <> B.description or  A.account_number <> B.account_no or  A.name <> B.name or  A.owner_type <> B.owner_type WHEN NOT MATCHED THEN Insert  (account_id, account_type, account_name, description, name, bank_id, cur_code, account_nick_name, owner_type, non_residential_status, account_status, brch_code, branch_no, account_type_name, islamic, bank_name, routing_bic, address_line_1, address_line_2, bank_address_line_1, bank_address_line_2, legal_id, actv_flag, bank_account_product_type, country, bank_entity_id, created_by, created_date, account_number) values (b.cew_account_id,b.account_type,b.ACCT_NAME,b.description,b.name, b.bank_id,b.cur_code,NULL,b.owner_type,NULL, NULL,b.brch_code,b.branch_no,NULL,NULL, b.bank_name,b.routing_bic,b.address_line_1,b.address_line_2,b.bank_address_line_1, b.bank_address_line_2,NULL,b.actv_flag,NULL,b.country, 'UOBT','SYSTEM',sysdate,b.account_no)
sync.account.delete.query=delete from AES_ACCOUNT_ATTRIBUTES acc where acc.account_id not in (select decode(abc.cew_acc_id,NULL,abc.account_id,abc.cew_acc_id) cew_account_id  from (select mv.account_id,aai.account_id cew_acc_id from geb_account_data mv left join aes_account_alternate_identifiers aai on aai.id_value=mv.account_id and aai.id_type='GEB' and aai.bank_entity_id='UOBT') abc) 
sync.account.bankacct.update.query=merge INTO aes_account_attributes a using ( select  Account_id, case when account_type='01' and owner_type='01' then 'CUR' when account_type='03' and owner_type='01' then 'SAV'  when account_type='01' and owner_type='05' then 'EXT' when account_type='05' and owner_type='01' then 'GTD' when account_type='04' and owner_type='01' then 'LNS'  else c.bank_account_type end as bank_account_type from aes_account_attributes c  )b on (a.ACCOUNT_ID=b.ACCOUNT_ID and  a.BANK_ACCOUNT_TYPE is null) WHEN MATCHED THEN UPDATE set a.BANK_ACCOUNT_TYPE=b.bank_account_type
sync.account.nra.islamic.query=merge  INTO aes_account_attributes a using (select AAAI.Account_id, GEB_NRA.String_Value as NRA_Ind, GEB_Islamic.String_Value as IslamicInd from  aes_account_alternate_identifiers AAAI left join geb_account_objectdata GEB_NRA ON aaai.id_value = GEB_NRA.account_id and aaai.id_type = 'GEB' and GEB_NRA.string_name = 'nra' left join geb_account_objectdata GEB_Islamic ON aaai.id_value = GEB_Islamic.account_id and aaai.id_type = 'GEB' and GEB_Islamic.string_name = 'IslamicInd') b on (a.ACCOUNT_ID=b.ACCOUNT_ID  and  UPDATED_DATE is null) WHEN MATCHED THEN UPDATE set a.NON_RESIDENTIAL_STATUS = b.NRA_Ind, a.ISLAMIC = b.IslamicInd

```

## Detailed Data Flow Analysis

### System Overview
The SQL statements show an integration between two systems:
1. GEB (Source System) - Contains tables:
   - geb_account_data: Primary account information
   - geb_account_objectdata: Additional account attributes

2. AES (Target System) - Contains tables:
   - aes_account_attributes: Main account table
   - aes_account_alternate_identifiers: Links accounts between systems

### Data Flow Process

#### Step 1: Initial Data Read
```sql
select ACCOUNT_NUMBER, ACCOUNT_TYPE, CUR_CODE, OWNER_TYPE 
from aes_Account_attributes 
where account_type in ('01','03')
```
- Purpose: Initial read of existing accounts
- Only reads accounts of type '01' (likely current accounts) and '03' (likely savings accounts)
- Used to establish baseline of existing data

Purpose:
The first query retrieves details (ACCOUNT_NUMBER, ACCOUNT_TYPE, CUR_CODE, OWNER_TYPE) from the aes_Account_attributes table for specific account types ('01' and '03'). This could be used to gather all account information that fits these types before running updates.
The second query counts the total number of records in aes_Account_attributes, which might be used to verify the number of accounts to ensure synchronization completeness.

Data Flow: The information fetched here could be used for comparison or logging to ensure updates are accurate and complete.

#### Step 2: Major Data Synchronization
```sql
MERGE into aes_account_attributes A 
Using (select * from (
    SELECT abc.*, 
    DECODE(abc.cew_acc_id, NULL, abc.account_id, abc.cew_acc_id) cew_account_id 
    FROM (
        SELECT mv.*, aai.account_id cew_acc_id 
        FROM geb_account_data mv 
        LEFT JOIN aes_account_alternate_identifiers aai 
        ON aai.id_value = mv.account_id
    ) abc
) xyz where xyz.cew_account_id not in (
    select ACCOUNT_ID from (
        select ACCOUNT_ID, count(ID_VALUE) 
        from aes_account_alternate_identifiers 
        group by ACCOUNT_ID 
        having count(ID_VALUE)>1
    )
)) b
```

Key Operations:
1. Data Preparation:
   - Joins GEB account data with alternate identifiers
   - Creates unified account ID (cew_account_id)
   - Excludes accounts with multiple mappings

2. Data Updates:
   ```sql
   WHEN MATCHED THEN UPDATE SET 
   A.BRCH_CODE = B.BRCH_CODE,
   A.ACCOUNT_TYPE = B.ACCOUNT_TYPE,
   /* ... more fields ... */
   where A.BRCH_CODE <> B.BRCH_CODE or 
   A.ACCOUNT_TYPE <> B.ACCOUNT_TYPE
   /* ... more conditions ... */
   ```
   - Updates only changed fields
   - Tracks system updates with timestamp

Purpose:
This MERGE statement (a combined UPDATE/INSERT query) is used to synchronize data from geb_account_data into aes_account_attributes.
The subqueries here:
mv.*: Grabs account details from geb_account_data.
aai.account_id: Brings in alternative identifiers from aes_account_alternate_identifiers.
DECODE function: Ensures that if cew_acc_id (alternate identifier) is NULL, the main account ID (account_id) is used instead.
Exclusion of accounts with multiple alternate identifiers prevents updating or inserting duplicate accounts.

Data Flow: If cew_account_id (main or alternate) exists in aes_account_attributes:
UPDATE fields like BRCH_CODE, ACCOUNT_TYPE, and ADDRESS_LINE_1 where they differ, and set the updated date.
If the account does not exist, INSERT a new row with values from

3. New Account Creation:
   ```sql
   WHEN NOT MATCHED THEN Insert
   (account_id, account_type, /* ... */)
   values (b.cew_account_id, b.account_type, /* ... */)
   ```
   - Creates new accounts in AES
   - Sets default values for optional fields

Purpose:
This MERGE statement (a combined UPDATE/INSERT query) is used to synchronize data from geb_account_data into aes_account_attributes.
The subqueries here:
mv.*: Grabs account details from geb_account_data.
aai.account_id: Brings in alternative identifiers from aes_account_alternate_identifiers.
DECODE function: Ensures that if cew_acc_id (alternate identifier) is NULL, the main account ID (account_id) is used instead.
Exclusion of accounts with multiple alternate identifiers prevents updating or inserting duplicate accounts.

Data Flow: If cew_account_id (main or alternate) exists in aes_account_attributes:
UPDATE fields like BRCH_CODE, ACCOUNT_TYPE, and ADDRESS_LINE_1 where they differ, and set the updated date.
If the account does not exist, INSERT a new row with values from geb_account_data.

#### Step 3: Cleanup of Obsolete Records
```sql
delete from AES_ACCOUNT_ATTRIBUTES acc 
where acc.account_id not in (
    select decode(abc.cew_acc_id,NULL,abc.account_id,abc.cew_acc_id) 
    from (
        select mv.account_id, aai.account_id cew_acc_id 
        from geb_account_data mv 
        left join aes_account_alternate_identifiers aai 
        on aai.id_value=mv.account_id 
        and aai.id_type='GEB' 
        and aai.bank_entity_id='UOBT'
    ) abc
)
```
- Removes accounts that no longer exist in GEB
- Maintains referential integrity
- Only affects accounts for bank entity 'UOBT'

Purpose:
This deletes accounts from aes_account_attributes that do not exist in geb_account_data.
The subquery gathers account IDs from geb_account_data, selecting cew_acc_id when available; otherwise, it uses account_id.

Data Flow: Only those records present in geb_account_data or linked with a GEB identifier (UOBT entity) are retained, keeping aes_account_attributes synchronized with GEB data.


#### Step 4: Account Type Standardization
```sql
merge INTO aes_account_attributes a 
using (
    select Account_id,
    case 
        when account_type='01' and owner_type='01' then 'CUR'
        when account_type='03' and owner_type='01' then 'SAV'
        when account_type='01' and owner_type='05' then 'EXT'
        when account_type='05' and owner_type='01' then 'GTD'
        when account_type='04' and owner_type='01' then 'LNS'
        else c.bank_account_type 
    end as bank_account_type 
    from aes_account_attributes c
) b
```
Standardizes account types based on combinations:
- Maps numerical codes to readable formats
- Only updates null bank_account_type fields
- Preserves existing valid values

Purpose:
This deletes accounts from aes_account_attributes that do not exist in geb_account_data.
The subquery gathers account IDs from geb_account_data, selecting cew_acc_id when available; otherwise, it uses account_id.

Data Flow: Only those records present in geb_account_data or linked with a GEB identifier (UOBT entity) are retained, keeping aes_account_attributes synchronized with GEB data.

#### Step 5: Additional Attributes Update
```sql
merge INTO aes_account_attributes a 
using (
    select AAAI.Account_id,
           GEB_NRA.String_Value as NRA_Ind,
           GEB_Islamic.String_Value as IslamicInd 
    from aes_account_alternate_identifiers AAAI 
    left join geb_account_objectdata GEB_NRA 
    ON aaai.id_value = GEB_NRA.account_id 
    and GEB_NRA.string_name = 'nra'
    left join geb_account_objectdata GEB_Islamic 
    ON aaai.id_value = GEB_Islamic.account_id 
    and GEB_Islamic.string_name = 'IslamicInd'
)
```
- Updates special account indicators
- NRA (Non-Residential Account) status
- Islamic banking flags
- Only for accounts not previously updated

Purpose:
This updates the BANK_ACCOUNT_TYPE field in aes_account_attributes based on specific conditions.
If account_type and owner_type match specific values, corresponding bank_account_type codes like 'CUR', 'SAV', 'EXT', etc., are assigned. If no match is found, it defaults to the current bank_account_type of the row.

Data Flow: This standardizes and completes account type information in aes_account_attributes.

### Final Data State

After this process completes:

1. Account Master Data:
   - Synchronized account details
   - Standardized account types
   - Updated addresses and bank information

2. Account Classifications:
   - Mapped account types (CUR, SAV, EXT, GTD, LNS)
   - NRA status
   - Islamic banking indicators

3. Data Quality:
   - No duplicate mappings
   - No orphaned records
   - Consistent account types
   - Complete audit trail (system updates tracked)

## Detailed SQL Business Rules Analysis

### 1. Initial Account Reading
```sql
select ACCOUNT_NUMBER, ACCOUNT_TYPE, CUR_CODE, OWNER_TYPE 
from aes_Account_attributes 
where account_type in ('01','03')
```

Business Rules:
- Only processes specific account types:
  - '01': Likely Current/Checking accounts
  - '03': Likely Savings accounts
- Required fields for each account:
  - ACCOUNT_NUMBER: Unique identifier
  - ACCOUNT_TYPE: Account classification
  - CUR_CODE: Currency of the account
  - OWNER_TYPE: Type of account holder

### 2. Account Synchronization (Major MERGE Operation)

#### Data Preparation Rules
```sql
SELECT abc.*, 
DECODE(abc.cew_acc_id, NULL, abc.account_id, abc.cew_acc_id) cew_account_id 
FROM (
    SELECT mv.*, aai.account_id cew_acc_id 
    FROM geb_account_data mv 
    LEFT JOIN aes_account_alternate_identifiers aai 
    ON aai.id_value = mv.account_id
) abc
```

Business Rules:
1. Account ID Resolution:
   - Primary: Use CEW (Common Enterprise-Wide) account ID if available
   - Fallback: Use original account ID if no CEW ID exists
   - Purpose: Maintain single source of truth for account identification

2. Data Quality Rules:
   ```sql
   where xyz.cew_account_id not in (
       select ACCOUNT_ID 
       from (
           select ACCOUNT_ID, count(ID_VALUE) 
           from aes_account_alternate_identifiers 
           group by ACCOUNT_ID 
           having count(ID_VALUE)>1
       )
   )
   ```
   - Excludes accounts with multiple ID mappings
   - Prevents data inconsistency
   - Ensures one-to-one mapping between systems

#### Update Rules
```sql
WHEN MATCHED THEN UPDATE SET 
    A.BRCH_CODE = B.BRCH_CODE,
    A.ACCOUNT_TYPE = B.ACCOUNT_TYPE,
    A.ADDRESS_LINE_1 = B.ADDRESS_LINE_1,
    /* ... other fields ... */
WHERE 
    A.BRCH_CODE <> B.BRCH_CODE or
    A.ACCOUNT_TYPE <> B.ACCOUNT_TYPE or
    /* ... other conditions ... */
```

Business Rules:
1. Update Conditions:
   - Only update when values actually change
   - All fields must be explicitly compared
   - Null handling is implicit in comparison

2. Audit Trail:
   - UPDATED_BY set to 'SYSTEM'
   - UPDATED_DATE set to SYSDATE
   - Tracks all system-driven changes

#### Insert Rules
```sql
WHEN NOT MATCHED THEN Insert
(account_id, account_type, account_name, /* ... */)
values 
(b.cew_account_id, b.account_type, b.ACCT_NAME, /* ... */)
```

Business Rules:
1. New Account Creation:
   - Uses CEW account ID as primary key
   - Sets default values for optional fields
   - Maintains standardized naming conventions

2. Required Fields:
   - account_id: Mandatory unique identifier
   - account_type: Must be valid type code
   - cur_code: Valid currency code
   - bank_entity_id: Set to 'UOBT'

### 3. Account Cleanup Rules
```sql
delete from AES_ACCOUNT_ATTRIBUTES acc 
where acc.account_id not in (
    select decode(abc.cew_acc_id,NULL,abc.account_id,abc.cew_acc_id) 
    from (
        select mv.account_id, aai.account_id cew_acc_id 
        from geb_account_data mv 
        left join aes_account_alternate_identifiers aai 
        on aai.id_value=mv.account_id 
        and aai.id_type='GEB' 
        and aai.bank_entity_id='UOBT'
    ) abc
)
```

Business Rules:
1. Deletion Criteria:
   - Remove accounts not in GEB system
   - Only affect UOBT bank entity
   - Consider both direct and indirect matches

2. Data Integrity:
   - Checks alternate identifiers
   - Preserves linked accounts
   - Maintains referential integrity

### 4. Account Type Standardization Rules
```sql
case 
    when account_type='01' and owner_type='01' then 'CUR'
    when account_type='03' and owner_type='01' then 'SAV'
    when account_type='01' and owner_type='05' then 'EXT'
    when account_type='05' and owner_type='01' then 'GTD'
    when account_type='04' and owner_type='01' then 'LNS'
    else c.bank_account_type 
end as bank_account_type
```

Business Rules:
1. Account Type Mapping:
   - '01'+'01' → 'CUR': Individual Current Account
   - '03'+'01' → 'SAV': Individual Savings Account
   - '01'+'05' → 'EXT': External Current Account
   - '05'+'01' → 'GTD': Individual Guaranteed Account
   - '04'+'01' → 'LNS': Individual Loan Account

2. Update Conditions:
   - Only update NULL bank_account_type
   - Preserve existing valid values
   - Maintain type consistency

### 5. Special Attributes Update Rules
```sql
merge INTO aes_account_attributes a 
using (
    select AAAI.Account_id,
           GEB_NRA.String_Value as NRA_Ind,
           GEB_Islamic.String_Value as IslamicInd 
    /* ... joins ... */
)
```

Business Rules:
1. NRA (Non-Residential Account) Rules:
   - Derived from GEB system
   - Updates only if UPDATED_DATE is null
   - Maintains regulatory compliance

2. Islamic Banking Rules:
   - Flags Islamic banking products
   - Updates in sync with NRA status
   - Ensures proper product classification

### Overall Process Rules

1. Data Synchronization:
   - Source of Truth: GEB system
   - Target: AES system
   - Frequency: Implied batch process

2. Data Quality:
   - No duplicate mappings allowed
   - Complete audit trail maintained
   - Standardized account classifications

3. Business Entity:
   - Specific to 'UOBT' bank entity
   - Maintains entity-specific rules
   - Preserves entity boundaries

4. Error Handling:
   - Implicit handling of nulls
   - Validation through constraints
   - Atomic operations for consistency

# utils

## LinuxFileNameSanitizer
```java
String sourcePath = Optional.of(jobContext.getString(ContextKey.sourcePath))
                    .filter(String::isEmpty)
                    .orElseThrow(() -> new BulkProcessingException("sourcePath is missing from job context",
                            new Throwable("sourcePath is missing from job context: "
                                    + chunkContext.getStepContext().getJobName())));
```


# WebClient & Resilient4J

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot2</artifactId>
    <version>1.7.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

```properties
resilience4j.circuitbreaker.instances.externalService.failureRateThreshold=50
resilience4j.circuitbreaker.instances.externalService.waitDurationInOpenState=5000
resilience4j.circuitbreaker.instances.externalService.permittedNumberOfCallsInHalfOpenState=3
resilience4j.circuitbreaker.instances.externalService.slidingWindowSize=10
resilience4j.circuitbreaker.instances.externalService.minimumNumberOfCalls=5
```

export SERVICE_A_URL=http://prod-service-a1.example.com,http://prod-service-a2.example.com
export SERVICE_B_URL=http://prod-service-b1.example.com,http://prod-service-b2.example.com
export SERVICE_C_URL=http://prod-service-c1.example.com,http://prod-service-c2.example.com

```java
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.ConnectionObserver;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfiguration {

    @Value("${SERVICE_A_URL:http://localhost:8080}")
    private String serviceAUrl;

    @Value("${SERVICE_B_URL:http://localhost:8081}")
    private String serviceBUrl;

    @Value("${SERVICE_C_URL:http://localhost:8082}")
    private String serviceCUrl;

    @Bean
    public WebClient webClient(ReactorLoadBalancerExchangeFilterFunction loadBalancerExchangeFilterFunction) {
        // Create a ConnectionProvider to manage connection pooling
        ConnectionProvider connectionProvider = ConnectionProvider.builder("customConnectionProvider")
                .maxConnections(100) // Max number of connections
                .pendingAcquireTimeout(Duration.ofMillis(5000)) // Timeout for acquiring connection
                .maxIdleTime(Duration.ofSeconds(20)) // Max idle time before a connection is closed
                .build();

        // Create a Reactor Netty HttpClient with timeouts and connection pooling
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // Connection timeout
                .doOnConnected(connection ->
                    connection.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                             .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)))
                .observe((connection, newState) -> {
                    if (newState == ConnectionObserver.State.DISCONNECTED) {
                        // Handle disconnects if needed
                    }
                })
                .responseTimeout(Duration.ofSeconds(10)) // Response timeout
                .wiretap("reactor.netty.http.client.HttpClient",
                          io.netty.handler.logging.LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL) // Enable logging
                .metrics(true) // Enable metrics for performance monitoring
                .compress(true) // Enable response compression
                .keepAlive(true); // Enable HTTP Keep-Alive

        // Build the WebClient
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(retryFilter()) // Add retry filter
                .filter(loggingFilter()) // Add logging filter
                .filter(loadBalancerExchangeFilterFunction) // Add load balancer filter
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // Set max memory size for responses (16MB)
                        .build())
                .defaultHeader("User-Agent", "Spring WebClient") // Set a default User-Agent header
                .build();
    }

    private ExchangeFilterFunction retryFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            // Add your retry logic here
            // For example, use RetryBackoffSpec for sophisticated retry policies
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction loggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            // if debugging
            if (clientRequest.headers().containsKey("Authorization")) {
                String jwtToken = clientRequest.headers().getFirst("Authorization").replace("Bearer ", "");
                System.out.println("JWT Token: " + jwtToken);
            }
            System.out.println("Request: " + clientRequest.method() + " " + clientRequest.url());
            return Mono.just(clientRequest);
        }).andThen(ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            // if debugging
            System.out.println("Response Status: " + clientResponse.statusCode());
            return Mono.just(clientResponse);
        }));
    }

    private boolean isDebugging() {
        return true; // Implement your logic for enabling/disabling logging
    }

    // Example method to construct WebClient with service-specific URL
    private WebClient serviceClient(String serviceUrl) {
        return webClient(null).mutate()
                .baseUrl(serviceUrl)
                .defaultHeader("Authorization", "Bearer " + generateJwtToken()) // Add JWT token to header
                .build();
    }

    private String generateJwtToken() {
        // Logic to generate JWT token
        return "your_jwt_token";
    }


}

```

```java
import reactor.core.publisher.Mono;

public class MyService {

    private final WebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    // Inject the configured WebClient
    public MyService(WebClient webClient CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = webClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    // Example method to perform an HTTP POST request
    @CircuitBreaker(name = "externalService", fallbackMethod = "fallback")
    @Retry(name = "externalService")
    @TimeLimiter(name = "externalService")
    public Mono<ServiceResponse> callExternalService(CompanyInfoRequest companyInfoRequest) {
        return webClient.post()
                .uri("/company")
                .body(Mono.just(companyInfoRequest), CompanyInfoRequest.class)
                .retrieve()
                .bodyToMono(ServiceResponse.class)
                .doOnError(e -> System.out.println("Error: " + e.getMessage()));
    }

    public Mono<String> fallback(Exception e) {
        // Log or handle the exception
        System.out.println("Fallback executed due to: " + throwable.getMessage());
        // Return a fallback response or alternative value
        return Mono.just(new ServiceResponse("Fallback response"));
    }

    // Example usage in a service class
    public Mono<CompanyInfoResponse> getCompanyInfo(String companyId) {
        return serviceClient(serviceAUrl)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/company/{id}")
                        .build(companyId))
                .retrieve()
                .bodyToMono(CompanyInfoResponse.class)
                .doOnError(e -> System.out.println("Error: " + e.getMessage()));
    }
}

```

```yaml
resilience4j:
    retry:
        instances:
            externalService:
                max-attempts: 3
                wait-duration: 500ms
                enable-exponential-backoff: true
                exponential-backoff-multiplier: 1.5
                retry-exceptions:
                    - org.springframework.web.reactive.function.client.WebClientResponseException
                ignore-exceptions:
                    - java.lang.IllegalArgumentException

    circuitbreaker:
        instances:
            externalService:
                register-health-indicator: true
                sliding-window-type: COUNT_BASED
                sliding-window-size: 50
                failure-rate-threshold: 50
                wait-duration-in-open-state: 30s
                permitted-number-of-calls-in-half-open-state: 10
                minimum-number-of-calls: 10

    timelimiter:
        instances:
            externalService:
                timeout-duration: 2s
                cancel-running-future: true
```

```java
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4jConfig {

    @Bean
    public RetryConfig retryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(WebClientResponseException.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();
    }

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(50)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(10)
                .minimumNumberOfCalls(10)
                .build();
    }

    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(2))
                .cancelRunningFuture(true)
                .build();
    }
}
```

```java
@Configuration
@EnableRetry
public class RetryConfig {
    @Bean
    public RetryTemplate retryTemplate() {
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(5000); // 5 seconds

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOffPolicy);

        return template;
    }
}
```

# Command Line Application & Spring Active Profile

This approach will load properties in the following order:

application.properties (default shared properties)
application-{env}.properties (environment-specific properties)
application-{flow}.properties (flow-specific properties)

```bash
> java -Denv=dev -Dflow=flow1 -Dconfig.path=/path/to/your/config -jar your-application.jar
```

```java
@SpringBootApplication
 @Configuration
 @Slf4j
 public class UobCmdBootApplication implements CommandLineRunner {

     @Autowired
     ConfigurableApplicationContext context;

     @Autowired
     PfpProperties pfpProperties;

     @Autowired
     AtomicBoolean runOnce;

     @Autowired
     CountDownLatch latch;

     @Autowired
     FilePollingAdapter filePollingAdapter;

     public static void main(String[] args) {
        String env = System.getProperty("env", "dev");
        String flow = System.getProperty("flow", "flow1");
        String configPath = System.getProperty("config.path", "/etc/myapp/config")
        SpringApplication app = new SpringApplication(UobCmdBootApplication.class)
            .properties("spring.config.additional-location=" + configPath + "/")
            .profiles(env, flow)
            .build()
            .run(args);
     }

     @Override
     public void run(String... args) throws Exception {
         log.info("DMP Auth file processing path: {}", pfpProperties.getDmpAuthProcessingFilePath());
         log.info("polling {} mins", pfpProperties.getDmpAuthFilePolling());
         boolean runOnceFlag = Arrays.asList(args).contains("--once");
         runOnce.getAndSet(runOnceFlag);
         filePollingAdapter.startFilePolling();

         if (runOnceFlag) {
             // run once and exit
             log.info("run once only ...");
             latch.await();
             filePollingAdapter.stopFilePolling();
             SpringApplication.exit(context, () -> 0);
         } else {
             log.info("continuously running ...");
             Thread.currentThread().join();
         }
     }

    @Autowired
    private FileLockService fileLockService;

    @PreDestroy
    public void onShutdown() {
        fileLockService.releaseAllLocks();
    }
 }

```

For automatic restarts, you can use a process manager like systemd on Linux or a tool like Supervisor. Here's a sample systemd service file:

```txt
[Unit]
Description=UOB CMD Boot Application
After=network.target

[Service]
ExecStart=/usr/bin/java -jar /path/to/your/application.jar
Restart=always
User=youruser

[Install]
WantedBy=multi-user.target
```

# NAS & Non-blocking IO

## File Polling

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class FilePollingAdapter {

    private final ProcessContext processContext;
    private final PollerMetadata poller;
    private final ObjectMapper objectMapper;
    private final FileReadingMessageSource fileReadingMessageSource;
    private final MessageChannel fileProcessingInputChannel;
    private final FileLockService fileLockService;
    private final IntegrationFlowContext flowContext;

    private final AtomicBoolean isPolling = new AtomicBoolean(false);
    private IntegrationFlowContext.IntegrationFlowRegistration filePollingFlowRegistration;

    @ServiceActivator(inputChannel = "startFilePollingChannel")
    public void startFilePolling() {
        if (isPolling.compareAndSet(false, true)) {
            IntegrationFlow filePollingFlow = IntegrationFlow
                    .from(fileReadingMessageSource, config -> config.poller(poller))
                    .filter(File.class, file -> {
                        if (fileLockService.tryLock(file.getAbsolutePath())) {
                            return true;
                        } else {
                            log.info("failed to create lock for file: {}", file.getAbsolutePath());
                            return false;
                        }
                    })
                    .handle(this, HandlerName.readFile.name())
                    .channel(fileProcessingInputChannel)
                    .get();
            filePollingFlowRegistration = flowContext.registration(filePollingFlow).register();
            log.info("File Polling started");
        } else {
            log.info("File Polling already running");
        }
    }

    @ServiceActivator(inputChannel = "stopFilePollingChannel")
    public void stopFilePolling() {
        if (isPolling.compareAndSet(true, false)) {
            if (filePollingFlowRegistration != null) {
                flowContext.remove(filePollingFlowRegistration.getId());
                filePollingFlowRegistration = null;
            }
            log.info("File Polling stopped");
        } else {
            log.info("File Polling already stopped");
        }
    }

    public Message<?> readFile(Message<?> message) {
        log.info("readFile started");
        MessageHeaders headers = message.getHeaders();
        String fileName = (String)headers.get("file_name");
        List<HandlerName> processors = new ArrayList<>();
        processors.add(HandlerName.readFile);
        processContext.setContext(fileName, "processors", processors);
        File file = (File) message.getPayload();
        String jsonString = "";
        try {
            jsonString = new String(Files.readAllBytes(file.toPath()));
            log.info("successful to read file: {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("failed to read file: {}", file.getAbsolutePath(), e);
            return null;
        }
        AuthPain001 authPain001 = null;
        try {
            authPain001 = objectMapper.readValue(jsonString, AuthPain001.class);
            log.info("successful to convert json from: {}", file.getAbsolutePath());
        } catch (JsonProcessingException e) {
            log.error("failed to read file: {}", file.getAbsolutePath(), e);
            return null;
        }

        return MessageBuilder.withPayload(authPain001).copyHeaders(headers).build();
    }

}
```

## File Lock

```java
@Service
public class FileLockService {
    private final ConcurrentMap<String, FileLock> locks = new ConcurrentHashMap<>();

    public boolean tryLock(String filePath) {
        try {
            FileChannel channel = FileChannel.open(Paths.get(filePath), StandardOpenOption.WRITE);
            FileLock lock = channel.tryLock();
            if (lock != null) {
                locks.put(filePath, lock);
                return true;
            }
        } catch (IOException e) {
            // Log error
        }
        return false;
    }

    public void releaseLock(String filePath) {
        FileLock lock = locks.remove(filePath);
        if (lock != null) {
            try {
                lock.release();
                lock.channel().close();
            } catch (IOException e) {
                // Log error
            }
        }
    }

    public void releaseAllLocks() {
        locks.forEach((path, lock) -> {
            try {
                lock.release();
                lock.channel().close();
            } catch (IOException e) {
                // Log error
            }
        });
        locks.clear();
    }
}
```

## File Status Tracking

```java
public class FileProcessingStatus {

    private String fileName;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    private LocalDateTime lastUpdated;

    // Constructors, getters, and setters
}

public enum ProcessingStatus {
    PENDING, PROCESSING, COMPLETED, ERROR
}

@Mapper
public interface FileProcessingStatusMapper {
    @Select("SELECT * FROM file_processing_status WHERE file_name = #{fileName}")
    FileProcessingStatus findByFileName(String fileName);

    @Insert("INSERT INTO file_processing_status (file_name, status, last_updated) " +
            "VALUES (#{fileName}, #{status}, #{lastUpdated})")
    void insert(FileProcessingStatus status);

    @Update("UPDATE file_processing_status SET status = #{status}, last_updated = #{lastUpdated} " +
            "WHERE file_name = #{fileName}")
    void update(FileProcessingStatus status);
}
```

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.yourpackage.FileProcessingStatusMapper">
    <select id="findByFileName" resultType="com.yourpackage.FileProcessingStatus">
        SELECT * FROM file_processing_status WHERE file_name = #{fileName}
    </select>

    <insert id="insert" parameterType="com.yourpackage.FileProcessingStatus">
        INSERT INTO file_processing_status (file_name, status, last_updated)
        VALUES (#{fileName}, #{status}, #{lastUpdated})
    </insert>

    <update id="update" parameterType="com.yourpackage.FileProcessingStatus">
        UPDATE file_processing_status
        SET status = #{status}, last_updated = #{lastUpdated}
        WHERE file_name = #{fileName}
    </update>
</mapper>
```

```java
@Service
@Transactional
public class FileProcessingService {
    private final FileProcessingStatusRepository statusRepository;
    private final FileLockService fileLockService;

    public FileProcessingService(FileProcessingStatusRepository statusRepository, FileLockService fileLockService) {
        this.statusRepository = statusRepository;
        this.fileLockService = fileLockService;
    }

    public void processFile(File file) {
        String fileName = file.getName();
        if (!fileLockService.tryLock(file.getPath())) {
            throw new RuntimeException("Unable to acquire lock for file: " + fileName);
        }

        try {
            FileProcessingStatus status = statusRepository.findById(fileName)
                .orElse(new FileProcessingStatus(fileName, ProcessingStatus.PENDING, LocalDateTime.now()));

            if (status.getStatus() != ProcessingStatus.PENDING) {
                return; // File already processed or being processed
            }

            status.setStatus(ProcessingStatus.PROCESSING);
            status.setLastUpdated(LocalDateTime.now());
            statusRepository.save(status);

            // Process the file
            // ...

            status.setStatus(ProcessingStatus.COMPLETED);
            status.setLastUpdated(LocalDateTime.now());
            statusRepository.save(status);
        } catch (Exception e) {
            FileProcessingStatus status = statusRepository.findById(fileName)
                .orElseThrow(() -> new RuntimeException("Status not found for file: " + fileName));
            status.setStatus(ProcessingStatus.ERROR);
            status.setLastUpdated(LocalDateTime.now());
            statusRepository.save(status);
            throw e;
        } finally {
            fileLockService.releaseLock(file.getPath());
        }
    }
}
```

# Process flow & DLQ flow

## Process Flow

```java

@Configuration
 @EnableIntegration
 @Slf4j
 @RequiredArgsConstructor
@Profile("DmpFlow")
 public class DmpIntegrationConfig {

     private final PfpProperties pfpProperties;

     @Bean
     public ObjectMapper objectMapper() {
         ObjectMapper mapper = new ObjectMapper();
         mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
         return mapper;
     }

     @Bean
     public AtomicBoolean runOnce() {
         return new AtomicBoolean(false);
     }

     @Bean
     public CountDownLatch runOnceLatch() {
         return new CountDownLatch(1);
     }

     @Bean
     public MessageChannel startFilePollingChannel() {
         return new DirectChannel();
     }

     @Bean
     public MessageChannel stopFilePollingChannel() {
         return new DirectChannel();
     }

     @Bean
     public MessageChannel fileProcessingInputChannel() {
         return new DirectChannel();
     }

     @Bean
     public MessageChannel pwsProcessingInputChannel() {
         return new DirectChannel();
     } // ToDo: change to multithreading

     @Bean
     public MessageChannel notificationInputChannel() {
         return new DirectChannel();
     }

     @Bean
     public MessageChannel fileBackupInputChannel() {
         return new DirectChannel();
     }

     @Bean(name = PollerMetadata.DEFAULT_POLLER)
     public PollerMetadata poller() {
         return Pollers.fixedDelay(Duration.ofMinutes(pfpProperties.getDmpAuthFilePolling())).getObject();
     }

     @Bean
     public FileReadingMessageSource fileReadingMessageSource() {
         FileReadingMessageSource source = new FileReadingMessageSource();
         source.setDirectory(new File(pfpProperties.getDmpAuthProcessingFilePath()));
         source.setFilter(new SimplePatternFileListFilter(pfpProperties.getDmpAuthProcessingFilePattern()));
         source.setUseWatchService(true);
         source.setWatchEvents(FileReadingMessageSource.WatchEventType.CREATE,
                 FileReadingMessageSource.WatchEventType.MODIFY);
         return source;
     }

     @Bean
     public IntegrationFlow fileProcessingFlow(ProcessingHandler processingHandler, ProcessingHandlerErrorAdvice advice) {
         return IntegrationFlow.from("fileProcessingInputChannel")
                 .handle(processingHandler, HandlerName.processAuthFile.name(), e -> e.advice(advice))
                 .channel("pwsProcessingInputChannel")
                 .handle(processingHandler, HandlerName.processPwsMessage.name(), e -> e.advice(advice))
                 .channel("notificationInputChannel")
                 .handle(processingHandler, HandlerName.sendNotification.name())
                 .channel("fileBackupInputChannel")
                 .handle(processingHandler, HandlerName.backupFile.name())
                 .get();
     }

 }
```

## DLQ flow

```xml
<dependency>
    <groupId>com.ibm.mq</groupId>
    <artifactId>mq-jms-spring-boot-starter</artifactId>
    <version>2.6.4</version>
</dependency>
```

```propertis
ibm.mq.queueManager=YOUR_QUEUE_MANAGER
ibm.mq.channel=YOUR_CHANNEL
ibm.mq.connName=YOUR_CONNECTION_NAME(1414)
ibm.mq.user=YOUR_USERNAME
ibm.mq.password=YOUR_PASSWORD
ibm.mq.queue=YOUR_DLQ_QUEUE_NAME
```

```java
@Configuration
public class MQConfig {
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        return new JmsTemplate(connectionFactory);
    }

    @Bean
    public MessageChannel deadLetterChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow deadLetterFlow() {
        return IntegrationFlow.from("deadLetterChannel")
            .<Message<?>>handle((payload, headers) -> {
                // Log the error, potentially move the file to an error directory
                return null;
            })
            .get();
    }
}
```

```java
@Service
public class ErrorHandlingService {
    private final JmsTemplate jmsTemplate;

    public ErrorHandlingService(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void sendToDeadLetterQueue(ErrorMessage errorMessage) {
        jmsTemplate.convertAndSend("YOUR_DLQ_QUEUE_NAME", errorMessage);
    }
}

@Bean
public IntegrationFlow fileProcessingFlow() {
    return IntegrationFlow.from("fileProcessingInputChannel")
        .handle(processingHandler, "processFile", e -> e
            .advice(retryAdvice())
            .handleMessageProcessingError(error -> error.sendTo("deadLetterChannel")))
        // Rest of your flow
        .get();
}
```

## Spring Batch Process

Key Considerations:

-   File Synchronization: To prevent multiple instances from processing the same CSV files, you can use a file locking mechanism or database coordination.
-   Transaction Consistency: Using Spring Batch’s transaction management, we can ensure that all steps of the batch are either fully processed or rolled back in case of failure.
-   Handling Multiple CSVs per Batch: You need to group the CSVs of a single batch (with the same prefix) and process them together.
-   Notification on Completion or Failure: You can use a listener to send a success or failure message to IBM MQ.
-   Concurrency: Running multiple batch processes on different nodes for different customers and batches should be controlled to prevent race conditions.

```text
src/main/java/com/yourcompany/batchprocessing/
├── BatchApplication.java
├── config/
│   ├── BatchConfig.java
│   ├── DatabaseConfig.java
│   └── MQConfig.java
├── model/
│   ├── PaymentTransaction.java
│   ├── PaymentInstruction.java
│   ├── PaymentPayee.java
│   ├── PaymentAdvice.java
│   └── PaymentCharge.java
├── mapper/
│   ├── PaymentTransactionMapper.java
│   ├── PaymentInstructionMapper.java
│   ├── PaymentPayeeMapper.java
│   ├── PaymentAdviceMapper.java
│   └── PaymentChargeMapper.java
├── processor/
│   ├── PaymentTransactionProcessor.java
│   ├── PaymentInstructionProcessor.java
│   ├── PaymentPayeeProcessor.java
│   ├── PaymentAdviceProcessor.java
│   └── PaymentChargeProcessor.java
├── reader/
│   └── MultiResourceItemReader.java
├── writer/
│   └── MyBatisBatchItemWriter.java
├── listener/
│   ├── JobCompletionNotificationListener.java
│   └── StepExecutionListener.java
└── service/
    ├── BatchStatusService.java
    └── NotificationService.java
```

```java
@SpringBootApplication
@EnableBatchProcessing
public class BatchApplication implements CommandLineRunner {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importPaymentDataJob;

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String customerFolder = System.getProperty("customer.folder", "customer1");
        String inputPath = "/path/to/nas/" + customerFolder + "/";

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("inputPath", inputPath)
                .addDate("date", new Date())
                .toJobParameters();

        jobLauncher.run(importPaymentDataJob, jobParameters);
    }
}
```

```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Value("${chunk.size:1000}")
    private int chunkSize;

    @Bean
    public Job importPaymentDataJob(Step importPaymentTransactionsStep,
                                    Step importPaymentInstructionsStep,
                                    Step importPaymentPayeesStep,
                                    Step importPaymentAdvicesStep,
                                    Step importPaymentChargesStep,
                                    JobCompletionNotificationListener listener) {
        return jobBuilderFactory.get("importPaymentDataJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(importPaymentTransactionsStep)
                .next(importPaymentInstructionsStep)
                .next(importPaymentPayeesStep)
                .next(importPaymentAdvicesStep)
                .next(importPaymentChargesStep)
                .end()
                .build();
    }

    @Bean
    public Step importPaymentTransactionsStep(MyBatisBatchItemWriter<PaymentTransaction> writer) {
        return stepBuilderFactory.get("importPaymentTransactionsStep")
                .<PaymentTransaction, PaymentTransaction>chunk(chunkSize)
                .reader(multiResourceItemReader("paymentTransactions"))
                .processor(new PaymentTransactionProcessor())
                .writer(writer)
                .listener(new StepExecutionListener())
                .build();
    }

    // Similar step beans for other payment types...

    @Bean
    @StepScope
    public MultiResourceItemReader<PaymentTransaction> multiResourceItemReader(@Value("#{jobParameters['inputPath']}") String inputPath) {
        return new MultiResourceItemReader<>(inputPath, "batch*_paymentTransactions.csv", new PaymentTransactionMapper());
    }

    // Similar MultiResourceItemReader beans for other payment types...
}
```

```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Value("${chunk.size:1000}")
    private int chunkSize;

    // Define the Job that coordinates the different steps
    @Bean
    public Job importPaymentDataJob(JobCompletionNotificationListener listener,
                                    Step importPaymentTransactionsStep,
                                    Step importPaymentInstructionsStep,
                                    Step importPaymentPayeesStep,
                                    Step importPaymentAdvicesStep,
                                    Step importPaymentChargesStep) {
        return jobBuilderFactory.get("importPaymentDataJob")
                .listener(listener)
                .start(importPaymentTransactionsStep(paymentTransactionWriter()))  // First Step
                .next(importPaymentInstructionsStep(paymentInstructionWriter()))    // Second Step
                .next(importPaymentPayeesStep(paymentPayeeWriter()))
                .end()
                .build();
    }

    // Define the steps for each CSV file processing
    @Bean
    public Step importPaymentTransactionsStep(MyBatisBatchItemWriter<PaymentTransaction> writer) {
        return stepBuilderFactory.get("importPaymentTransactionsStep")
                .<PaymentTransaction, PaymentTransaction>chunk(chunkSize)
                .reader(multiResourceItemReader("paymentTransactions"))
                .processor(new PaymentTransactionProcessor())
                .writer(writer)
                .listener(new StepExecutionListener())
                .build();
    }

    // Similarly define step beans for other payment files (instructions, payees, advices, charges)
    @Bean
    public Step importPaymentInstructionsStep(MyBatisBatchItemWriter<PaymentInstruction> writer) {
        return stepBuilderFactory.get("importPaymentInstructionsStep")
                .<PaymentInstruction, PaymentInstruction>chunk(chunkSize)
                .reader(multiResourceItemReader("paymentInstructions"))
                .processor(new PaymentInstructionProcessor())
                .writer(writer)
                .listener(new StepExecutionListener())
                .build();
    }

    // Add steps for payees, advices, charges...
}
```

```sql
CREATE TABLE csv_file_process_status (
    id NUMBER PRIMARY KEY,
    file_name VARCHAR2(255) UNIQUE,
    process_status VARCHAR2(50),
    lock_flag NUMBER(1),  -- 0: unlocked, 1: locked
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

```java
@Service
public class FileProcessingLockService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Acquire the lock for a file
    public boolean acquireLock(String fileName) {
        String sql = "UPDATE csv_file_process_status SET lock_flag = 1 WHERE file_name = ? AND lock_flag = 0";
        int rowsUpdated = jdbcTemplate.update(sql, fileName);
        return rowsUpdated > 0;  // If rowsUpdated > 0, the lock was successfully acquired
    }

    // Release the lock for a file
    public void releaseLock(String fileName) {
        String sql = "UPDATE csv_file_process_status SET lock_flag = 0 WHERE file_name = ?";
        jdbcTemplate.update(sql, fileName);
    }

    // Update the process status
    public void updateProcessStatus(String fileName, String status) {
        String sql = "UPDATE csv_file_process_status SET process_status = ?, last_updated = CURRENT_TIMESTAMP WHERE file_name = ?";
        jdbcTemplate.update(sql, status, fileName);
    }

    // Check if file is being processed
    public boolean isFileLocked(String fileName) {
        String sql = "SELECT lock_flag FROM csv_file_process_status WHERE file_name = ?";
        Integer lockFlag = jdbcTemplate.queryForObject(sql, new Object[]{fileName}, Integer.class);
        return lockFlag != null && lockFlag == 1;
    }
}

```

```java
@Bean
public Step importPaymentTransactionsStep(JdbcBatchItemWriter<PaymentTransaction> writer, FileProcessingLockService lockService) {
    return stepBuilderFactory.get("importPaymentTransactionsStep")
            .<PaymentTransaction, PaymentTransaction>chunk(chunkSize)
            .reader(paymentTransactionReader())
            .processor(new PaymentTransactionProcessor())
            .writer(new PaymentTransactionWriter())
            .listener(new StepExecutionListener() {
                @Override
                public void beforeStep(StepExecution stepExecution) {
                    String fileName = stepExecution.getJobParameters().getString("fileName");
                    if (!lockService.acquireLock(fileName)) {
                        throw new IllegalStateException("Unable to acquire lock for file: " + fileName);
                    }
                    lockService.updateProcessStatus(fileName, "IN_PROGRESS");
                }

                @Override
                public ExitStatus afterStep(StepExecution stepExecution) {
                    String fileName = stepExecution.getJobParameters().getString("fileName");
                    if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
                        lockService.updateProcessStatus(fileName, "COMPLETED");
                    } else {
                        lockService.updateProcessStatus(fileName, "FAILED");
                    }
                    lockService.releaseLock(fileName);
                    return stepExecution.getExitStatus();
                }
            })
            .build();
}

```

```java
@Bean
@StepScope
public MultiResourceItemReader<PaymentTransaction> multiResourceItemReader(@Value("#{jobParameters['inputPath']}") String inputPath) {
    MultiResourceItemReader<PaymentTransaction> reader = new MultiResourceItemReader<>();
    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    try {
        Resource[] resources = resolver.getResources(inputPath + "/batch*_paymentTransactions.csv");
        reader.setResources(resources);
    } catch (IOException e) {
        throw new RuntimeException("Error reading CSV files", e);
    }

    reader.setDelegate(paymentTransactionReader()); // Define the CSV reader for PaymentTransaction
    return reader;
}

@Bean
@StepScope
public FlatFileItemReader<PaymentTransaction> paymentTransactionReader() {
    return new FlatFileItemReaderBuilder<PaymentTransaction>()
            .name("paymentTransactionReader")
            .delimited()
            .names(new String[] {"transactionId", "amount", "currency", "date"})
            .fieldSetMapper(new BeanWrapperFieldSetMapper<PaymentTransaction>() {{
                setTargetType(PaymentTransaction.class);
            }})
            .build();
}

// Similar reader configuration for instructions, payees, advices, charges
```

1. importPaymentTransactionsStep: Insert the payment transactions, store paymentTransactionId in the StepExecutionContext.
2. importPaymentInstructionsStep: Insert payment instructions, retrieve paymentTransactionId from the StepExecutionContext, and store paymentInstructionId in the StepExecutionContext.
3. importPaymentPayeesStep: Retrieve both paymentTransactionId and paymentInstructionId from the StepExecutionContext and use them for inserting payee information.

```java
public class PaymentTransactionProcessor implements ItemProcessor<PaymentTransaction, PaymentTransaction> {

    @Override
    public PaymentTransaction process(PaymentTransaction item) {
        // Any transformation logic for PaymentTransaction
        return item;
    }
}

public class PaymentInstructionWriter extends JdbcBatchItemWriter<PaymentInstruction> {

    @Override
    public void write(List<? extends PaymentInstruction> items) throws Exception {
        super.write(items);

        // Retrieve the StepExecution and StepExecutionContext
        StepExecution stepExecution = StepSynchronizationManager.getContext().getStepExecution();
        ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();

        // Assuming transactionId is already present in the context
        List<Long> transactionIds = (List<Long>) stepExecutionContext.get("transactionIds");

        // Store the generated instructionIds in the StepExecutionContext
        List<Long> instructionIds = items.stream()
                .map(PaymentInstruction::getInstructionId)  // Assuming getInstructionId is the method to get the generated ID
                .collect(Collectors.toList());

        // Save the instructionIds in the context
        stepExecutionContext.put("instructionIds", instructionIds);
    }
}

public class PaymentPayeeProcessor implements ItemProcessor<PaymentPayee, PaymentPayee> {

    @Override
    public PaymentPayee process(PaymentPayee item) throws Exception {
        // Retrieve StepExecutionContext
        StepExecution stepExecution = StepSynchronizationManager.getContext().getStepExecution();
        ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();

        // Get transactionIds and instructionIds from the context
        List<Long> transactionIds = (List<Long>) stepExecutionContext.get("transactionIds");
        List<Long> instructionIds = (List<Long>) stepExecutionContext.get("instructionIds");

        // Assign transactionId and instructionId to the Payee
        item.setTransactionId(transactionIds.get(0));  // Assuming single transaction scenario
        item.setInstructionId(instructionIds.get(0)); // Assign corresponding instructionId

        return item;
    }
}

public class PaymentPayeeWriter extends JdbcBatchItemWriter<PaymentPayee> {
    // No special changes required here, as the payees with their transactionId and instructionId
    // will be written directly to the database.
}

@Bean
public Step importPaymentPayeesStep(JdbcBatchItemWriter<PaymentPayee> writer) {
    return stepBuilderFactory.get("importPaymentPayeesStep")
            .<PaymentPayee, PaymentPayee>chunk(chunkSize)
            .reader(paymentPayeeReader())  // Custom reader to read payee data
            .processor(new PaymentPayeeProcessor())  // Processor retrieves IDs from context
            .writer(new PaymentPayeeWriter())  // Standard writer for payees
            .listener(new StepExecutionListener() {
                @Override
                public void beforeStep(StepExecution stepExecution) {
                    // Optionally add some logging or initialization logic
                }

                @Override
                public ExitStatus afterStep(StepExecution stepExecution) {
                    // Optionally handle post-step logic
                    return stepExecution.getExitStatus();
                }
            })
            .build();
}

```

```java
@Bean
public MyBatisBatchItemWriter<PaymentTransaction> paymentTransactionWriter(SqlSessionFactory sqlSessionFactory) {
    return new MyBatisBatchItemWriterBuilder<PaymentTransaction>()
            .sqlSessionFactory(sqlSessionFactory)
            .statementId("insertPaymentTransaction") // Define your MyBatis insert statement
            .build();
}

// Similarly, define writers for instructions, payees, advices, charges
```

```java
@Configuration
@MapperScan("com.yourcompany.batchprocessing.mapper")
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        // Configure your Oracle DataSource
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        return factoryBean.getObject();
    }

    @Bean
    public MyBatisBatchItemWriter<PaymentTransaction> paymentTransactionWriter(SqlSessionFactory sqlSessionFactory) {
        return new MyBatisBatchItemWriter<>(sqlSessionFactory, PaymentTransactionMapper.class, "insertPaymentTransaction");
    }

    // Similar writer beans for other payment types...
}
```

```java
@Configuration
@MapperScan("com.yourcompany.batchprocessing.mapper")
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        // Configure your Oracle DataSource
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        return factoryBean.getObject();
    }

    @Bean
    public MyBatisBatchItemWriter<PaymentTransaction> paymentTransactionWriter(SqlSessionFactory sqlSessionFactory) {
        return new MyBatisBatchItemWriter<>(sqlSessionFactory, PaymentTransactionMapper.class, "insertPaymentTransaction");
    }

    // Similar writer beans for other payment types...
}
```

```java
@Service
public class BatchStatusService {

    @Autowired
    private BatchStatusMapper batchStatusMapper;

    public void updateBatchStatus(String batchId, String status) {
        batchStatusMapper.updateBatchStatus(batchId, status);
    }
}
```

```java
@Service
public class NotificationService {

    @Autowired
    private JmsTemplate jmsTemplate;

    public void sendNotification(String message) {
        jmsTemplate.convertAndSend("NOTIFICATION_QUEUE", message);
    }
}
```

```java
@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

    @Autowired
    private BatchStatusService batchStatusService;

    @Autowired
    private NotificationService notificationService;

    @Override
    public void afterJob(JobExecution jobExecution) {
        if(jobExecution.getStatus() == BatchStatus.COMPLETED) {
            batchStatusService.updateBatchStatus(jobExecution.getJobParameters().getString("inputPath"), "COMPLETED");
            notificationService.sendNotification("Batch processing completed successfully");
        } else {
            batchStatusService.updateBatchStatus(jobExecution.getJobParameters().getString("inputPath"), "FAILED");
            notificationService.sendNotification("Batch processing failed");
        }
    }
}
```

```java
public class StepExecutionListener implements org.springframework.batch.core.StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        // Log step start
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        // Log step completion
        return null;
    }
}
```

Camel:

```xml
<!-- Apache Camel dependencies -->
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-file-starter</artifactId>
</dependency>

<!-- Spring Batch dependencies -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-batch</artifactId>
</dependency>
```

```java
@Configuration
public class CamelFilePollingRoute extends RouteBuilder {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importPaymentDataJob;

    @Autowired
    private DatabaseFileLockService fileLockService;

    @Value("${input.directory.path}")
    private String inputDirectory;

    @Override
    public void configure() throws Exception {
        from("file://" + inputDirectory + "?noop=true&include=batch.*_paymentTransactions.csv")
            .routeId("filePollingRoute")
            .process(this::lockAndLaunchJob)
            .log("Processing file ${header.CamelFileName}")
            .end();
    }

    private void lockAndLaunchJob(Exchange exchange) {
        File file = exchange.getIn().getBody(File.class);
        String fileName = file.getName();

        // Use DatabaseFileLockService to lock the file
        if (fileLockService.lockFile(fileName)) {
            try {
                JobParameters jobParameters = new JobParametersBuilder()
                    .addString("inputPath", file.getAbsolutePath())
                    .addLong("time", System.currentTimeMillis())  // Ensure unique job parameters
                    .toJobParameters();

                jobLauncher.run(importPaymentDataJob, jobParameters);
                fileLockService.unlockFile(fileName, "COMPLETED");
            } catch (JobExecutionException e) {
                fileLockService.unlockFile(fileName, "FAILED");
                exchange.setException(e);  // Handle exception in the route
            }
        } else {
            // Skip the file if it is already being processed
            log.info("File {} is already being processed or completed, skipping.", fileName);
        }
    }
}
```

```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Value("${chunk.size:1000}")
    private int chunkSize;

    @Bean
    public Job importPaymentDataJob(Step importPaymentTransactionsStep,
                                    Step importPaymentInstructionsStep,
                                    Step importPaymentPayeesStep,
                                    Step importPaymentAdvicesStep,
                                    Step importPaymentChargesStep) {
        return jobBuilderFactory.get("importPaymentDataJob")
                .incrementer(new RunIdIncrementer())
                .flow(importPaymentTransactionsStep)
                .next(importPaymentInstructionsStep)
                .next(importPaymentPayeesStep)
                .next(importPaymentAdvicesStep)
                .next(importPaymentChargesStep)
                .end()
                .build();
    }

    @Bean
    public Step importPaymentTransactionsStep(JdbcBatchItemWriter<PaymentTransaction> writer) {
        return stepBuilderFactory.get("importPaymentTransactionsStep")
                .<PaymentTransaction, PaymentTransaction>chunk(chunkSize)
                .reader(paymentTransactionReader(null))  // Use the CSV reader
                .processor(new PaymentTransactionProcessor()) // Custom processor
                .writer(writer)  // Custom writer
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<PaymentTransaction> paymentTransactionReader(
            @Value("#{jobParameters['inputPath']}") String inputPath) {
        return new FlatFileItemReaderBuilder<PaymentTransaction>()
                .name("paymentTransactionReader")
                .resource(new FileSystemResource(inputPath))
                .delimited()
                .names(new String[]{"column1", "column2", "column3"})
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(PaymentTransaction.class);
                }})
                .build();
    }
}
```

Large-Volume Data Insertion Optimization: For large-volume data insertion (e.g., in importPaymentInstructionsStep, importPaymentPayeesStep), Spring Batch provides several mechanisms to optimize data processing and reduce resource contention:

Chunk-based processing: Spring Batch processes data in chunks to optimize memory usage.
Batch Inserts: Spring Batch can batch the database inserts to reduce round trips between the application and the database.
Paging: Use paging techniques when reading large datasets.
Parallel Processing: You can split data across multiple threads or nodes for faster processing.
Partitioning: This allows splitting the data into partitions, where each partition can be processed independently on different threads or nodes.

1. Database Lock (Avoiding File Lock)
   Spring Batch uses the JobRepository to coordinate job executions across multiple instances and nodes. Ensure that the JobRepository is properly configured to use your Oracle database. Spring Batch handles locking at the job level using this repository, so you don’t need to manually implement file locking or database locks.

```java
@Configuration
public class BatchConfig {

    @Bean
    public JobRepository jobRepository(DataSource dataSource, PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean factoryBean = new JobRepositoryFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTransactionManager(transactionManager);
        factoryBean.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED"); // Adjust as necessary
        factoryBean.setDatabaseType("ORACLE"); // Specify Oracle as the database type
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

2. Large-Volume Data Insertion (Optimizing importPaymentInstructionsStep and importPaymentPayeesStep)
   Here’s how you can optimize your large-volume data insertion steps:

A. Chunk-based Processing and Batch Inserts
You can use chunk-based processing with a large chunk size to process multiple records in a single transaction and perform batch inserts. Below is an example of how you can configure chunk-based processing for importPaymentInstructionsStep.

```java
@Bean
public Step importPaymentInstructionsStep(JdbcBatchItemWriter<PaymentInstruction> writer) {
    return stepBuilderFactory.get("importPaymentInstructionsStep")
            .<PaymentInstruction, PaymentInstruction>chunk(1000)  // Define a suitable chunk size
            .reader(paymentInstructionReader(null))
            .processor(new PaymentInstructionProcessor())
            .writer(writer)
            .build();
}

@Bean
@StepScope
public FlatFileItemReader<PaymentInstruction> paymentInstructionReader(
        @Value("#{jobParameters['inputPath']}") String inputPath) {
    return new FlatFileItemReaderBuilder<PaymentInstruction>()
            .name("paymentInstructionReader")
            .resource(new FileSystemResource(inputPath))
            .delimited()
            .names(new String[]{"transactionId", "instructionData", "otherColumns"})
            .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                setTargetType(PaymentInstruction.class);
            }})
            .build();
}

@Bean
public JdbcBatchItemWriter<PaymentInstruction> paymentInstructionWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<PaymentInstruction>()
            .dataSource(dataSource)
            .sql("INSERT INTO payment_instruction (transaction_id, instruction_data, other_columns) "
                    + "VALUES (:transactionId, :instructionData, :otherColumns)")
            .beanMapped()
            .build();
}
```

Chunk Size: The chunk(1000) defines that 1000 rows will be processed per transaction. You can adjust this value based on your memory and database performance.
Batch Inserts: JdbcBatchItemWriter automatically batches SQL inserts for efficiency.
B. Parallel Processing with Task Executors
You can speed up processing by running multiple threads for each step. Spring Batch provides a TaskExecutor to execute steps in parallel.

```java
@Bean
public Step importPaymentInstructionsStep(JdbcBatchItemWriter<PaymentInstruction> writer, TaskExecutor taskExecutor) {
    return stepBuilderFactory.get("importPaymentInstructionsStep")
            .<PaymentInstruction, PaymentInstruction>chunk(1000)
            .reader(paymentInstructionReader(null))
            .processor(new PaymentInstructionProcessor())
            .writer(writer)
            .taskExecutor(taskExecutor)  // Enable parallel execution
            .throttleLimit(10)  // Limit the number of concurrent threads
            .build();
}

@Bean
public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("batch-thread-");
    executor.initialize();
    return executor;
}
```

TaskExecutor: This allows the step to run with multiple threads, speeding up data processing.
Throttle Limit: Defines the maximum number of concurrent threads. Adjust this according to your system's capacity.
C. Partitioning for Large Data Volumes
Spring Batch allows partitioning, where you can split the data into multiple partitions, each processed by a separate thread or even a separate JVM.

```java
@Bean
public Step masterStep(Step slaveStep) {
    return stepBuilderFactory.get("masterStep")
            .partitioner(slaveStep.getName(), new MultiResourcePartitioner())
            .step(slaveStep)
            .taskExecutor(taskExecutor())  // Parallel processing
            .build();
}

@Bean
public Step slaveStep(JdbcBatchItemWriter<PaymentInstruction> writer) {
    return stepBuilderFactory.get("slaveStep")
            .<PaymentInstruction, PaymentInstruction>chunk(1000)
            .reader(paymentInstructionReader(null))
            .processor(new PaymentInstructionProcessor())
            .writer(writer)
            .build();
}

@Bean
@StepScope
public MultiResourcePartitioner partitioner(@Value("#{jobParameters['inputPath']}") String inputPath) {
    MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
    partitioner.setResources(new FileSystemResource(inputPath).getFile().listFiles());  // Partition by file
    return partitioner;
}
```

Master-Slave Step: The master step divides the data into partitions and delegates the work to slave steps.
Partitioner: Here, files are partitioned, and each partition is processed in parallel. You can also partition data by ranges (e.g., ranges of IDs).

Summary:
Database Locking: Spring Batch's JobRepository ensures that jobs are locked across distributed nodes. You don't need file locks, and jobs are coordinated using Oracle database tables (BATCH_JOB_INSTANCE, BATCH_JOB_EXECUTION).

Optimizing Large-Volume Data Insertions:

Chunk-based processing ensures memory efficiency and batches data inserts.
Task Executors allow parallel processing of steps for faster data insertion.
Partitioning divides the data into smaller chunks, each processed independently, and can be run on different threads or even nodes.
By combining these techniques, you can scale up your batch processing and handle large CSV files more efficiently.

# Spring Batch

```bash
// run.sh
#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <flow> <stage>"
    echo "Stages: pre-db, db-insert, post-db"
    exit 1
fi

stage=$1

case $stage in
    pre-db|db-batch|post-db)
        java -jar -Dspring.profiles.active=$stage payment-processor.jar $stage
        ;;
    *)
        echo "Invalid stage. Use pre-db, db-insert, or post-db."
        exit 1
        ;;
esac
```

```sql
-- SQL schema for payment_file_status table
CREATE TABLE payment_file_status (
    id NUMBER GENERATED ALWAYS AS IDENTITY,
    file_name VARCHAR2(255) NOT NULL UNIQUE,
    flow VARCHAR2(100) NOT NULL,
    stage VARCHAR2(50) NOT NULL
    status VARCHAR2(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    error_message VARCHAR2(4000),
    csv_file_name VARCHAR2(255),
    CONSTRAINT pk_payment_file_status PRIMARY KEY (id)
);
```

```java
public enum Stage {
    PRE_DB,
    DB_INSERT,
    POST_DB
}
```

```java
package com.example.payment;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;

@SpringBootApplication
public class PaymentProcessingApplication {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private ConfigurableApplicationContext context;

    public static void main(String[] args) {
        String env = System.getProperty("env", "dev");
        String flow = System.getProperty("flow", "flow1");
        String configPath = System.getProperty("config.path", "/etc/myapp/config");

        SpringApplication app = new SpringApplication(PaymentProcessingApplication.class);
        app.setAdditionalProfiles(env);
        app.setDefaultProperties(Map.of(
            "spring.config.additional-location", configPath + "/",
            "flow", flow
        ));
        app.run(args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            if (args.length < 1) {
                System.out.println("Usage: java -jar payment-processor.jar <stage>");
                return;
            }

            String flow = context.getEnvironment().getProperty("flow");
            Stage stage = Stage.valueOf(args[0].toUpperCase());

            JobParameters params = new JobParametersBuilder()
                    .addString("flow", flow)
                    .addString("stage", stage.name())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            String jobName = stage.name().toLowerCase() + "Job" + flow;
            Job job = (Job) context.getBean(jobName);
            jobLauncher.run(job, params);
        };
    }

    @Bean
    public CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                // Configure Camel context before start if needed
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // Add shutdown hook to remove Camel file locks
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        System.out.println("Removing Camel file locks...");
                        try {
                            camelContext.getRoutes().forEach(route -> {
                                if (route.getEndpoint() instanceof org.apache.camel.component.file.GenericFileEndpoint) {
                                    org.apache.camel.component.file.GenericFileEndpoint endpoint =
                                        (org.apache.camel.component.file.GenericFileEndpoint) route.getEndpoint();
                                    endpoint.getGenericFileOperations().releaseAllLocksForPath(endpoint.getConfiguration().getDirectory());
                                }
                            });
                        } catch (Exception e) {
                            System.err.println("Error removing Camel file locks: " + e.getMessage());
                        }
                    }
                });
            }
        };
    }
}
```

```java
package com.example.payment.config;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.payment.service.PaymentFileStatusService;
import com.example.payment.Stage;

import javax.sql.DataSource;

@Configuration
public class CamelConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PaymentFileStatusService statusService;

    @Value("${stage}")
    private Stage stage;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Bean
    public JdbcMessageIdRepository jdbcMessageIdRepo() {
        return new JdbcMessageIdRepository(dataSource, "PROCESSED_FILES");
    }

    @Bean
    public RouteBuilder routeBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                errorHandler(deadLetterChannel("direct:errorHandler")
                    .useOriginalMessage()
                    .maximumRedeliveries(3)
                    .redeliveryDelay(1000)
                    .backOffMultiplier(2)
                    .retryAttemptedLogLevel(LoggingLevel.WARN));

                switch (stage) {
                    case PRE_DB:
                        configurePreDbStage();
                        break;
                    case DB_INSERT:
                        configureDbInsertStage();
                        break;
                    case POST_DB:
                        configurePostDbStage();
                        break;
                }

                from("direct:errorHandler")
                    .routeId("errorHandlerRoute")
                    .log(LoggingLevel.ERROR, "Error processing file: ${header.CamelFileName}")
                    .process(this::handleProcessingError);
            }

            private void configurePreDbStage() {
                from("file:{{input.directory}}?include={{file.pattern}}&readLock=idempotent&idempotent=true&idempotentRepository=#jdbcMessageIdRepo&readLockRemoveOnCommit=true&readLockRemoveOnRollback=true")
                    .routeId("preDbInsertionRoute")
                    .log("Pre-DB Insertion: ${header.CamelFileName}")
                    .process(this::initializeFileStatus)
                    .to("spring-batch:preDbJob" + activeProfile)
                    .process(this::completePreDbProcessing)
                    .to("file:{{output.directory}}");
            }

            private void configureDbInsertStage() {
                from("file:{{db.input.directory}}?include=*.csv&readLock=idempotent&idempotent=true&idempotentRepository=#jdbcMessageIdRepo&readLockRemoveOnCommit=true&readLockRemoveOnRollback=true")
                    .routeId("dbInsertionRoute")
                    .log("DB Insertion: ${header.CamelFileName}")
                    .process(this::updateStatusToDbInserting)
                    .to("spring-batch:dbInsertJob" + activeProfile)
                    .process(this::completeDbInsertion)
                    .to("file:{{db.output.directory}}");
            }

            private void configurePostDbStage() {
                from("file:{{post.input.directory}}?include=*.txt&readLock=idempotent&idempotent=true&idempotentRepository=#jdbcMessageIdRepo&readLockRemoveOnCommit=true&readLockRemoveOnRollback=true")
                    .routeId("postDbProcessingRoute")
                    .log("Post-DB Processing: ${header.CamelFileName}")
                    .process(this::updateStatusToPostDbProcessing)
                    .to("spring-batch:postDbJob" + activeProfile)
                    .process(this::completePostDbProcessing);
            }

            // ... (status update methods remain the same)
        };
    }
}
```

```java
@Configuration
public class CamelConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PaymentFileStatusService statusService;

    @Value("${stage}")
    private Stage stage;

    @Value("${flow}")
    private String flow;

    @Bean
    public JdbcMessageIdRepository jdbcMessageIdRepo() {
        return new JdbcMessageIdRepository(dataSource, "PROCESSED_FILES");
    }

    @Bean
    public RouteBuilder routeBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                errorHandler(deadLetterChannel("direct:errorHandler")
                    .useOriginalMessage()
                    .maximumRedeliveries(3)
                    .redeliveryDelay(1000)
                    .backOffMultiplier(2)
                    .retryAttemptedLogLevel(LoggingLevel.WARN));

                switch (stage) {
                    case PRE_DB:
                        configurePreDbStage();
                        break;
                    case DB_INSERT:
                        configureDbInsertStage();
                        break;
                    case POST_DB:
                        configurePostDbStage();
                        break;
                }

                from("direct:errorHandler")
                    .routeId("errorHandlerRoute")
                    .log(LoggingLevel.ERROR, "Error processing file: ${header.CamelFileName}")
                    .process(this::handleProcessingError);
            }

            private void configurePreDbStage() {
                from("file:{{" + flow + ".input.directory}}?include={{" + flow + ".file.pattern}}&readLock=idempotent&idempotent=true&idempotentRepository=#jdbcMessageIdRepo&readLockRemoveOnCommit=true&readLockRemoveOnRollback=true")
                    .routeId("preDbInsertionRoute")
                    .log("Pre-DB Insertion: ${header.CamelFileName}")
                    .process(this::initializeFileStatus)
                    .to("spring-batch:preDbJob" + flow)
                    .process(this::completePreDbProcessing)
                    .to("file:{{" + flow + ".output.directory}}");
            }

            private void configureDbInsertStage() {
                from("file:{{db.input.directory}}?include=*.csv&readLock=idempotent&idempotent=true&idempotentRepository=#jdbcMessageIdRepo&readLockRemoveOnCommit=true&readLockRemoveOnRollback=true")
                    .routeId("dbInsertionRoute")
                    .log("DB Insertion: ${header.CamelFileName}")
                    .process(this::updateStatusToDbInserting)
                    .to("spring-batch:dbInsertionJob")
                    .process(this::completeDbInsertion)
                    .to("file:{{db.output.directory}}");
            }

            private void configurePostDbStage() {
                from("file:{{post.input.directory}}?include=*.txt&readLock=idempotent&idempotent=true&idempotentRepository=#jdbcMessageIdRepo&readLockRemoveOnCommit=true&readLockRemoveOnRollback=true")
                    .routeId("postDbProcessingRoute")
                    .log("Post-DB Processing: ${header.CamelFileName}")
                    .process(this::updateStatusToPostDbProcessing)
                    .to("spring-batch:postDbJob")
                    .process(this::completePostDbProcessing);
            }

            private void completePreDbProcessing(Exchange exchange) {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                statusService.updateStatus(fileName, Stage.PRE_DB, "COMPLETED");
            }

            private void updateStatusToDbInserting(Exchange exchange) {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                statusService.updateStatus(fileName, Stage.DB_INSERT, "PROCESSING");
            }

            private void completeDbInsertion(Exchange exchange) {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                statusService.updateStatus(fileName, Stage.DB_INSERT, "COMPLETED");
            }

            private void updateStatusToPostDbProcessing(Exchange exchange) {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                statusService.updateStatus(fileName, Stage.POST_DB, "PROCESSING");
            }

            private void completePostDbProcessing(Exchange exchange) {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                statusService.updateStatus(fileName, Stage.POST_DB, "COMPLETED");
            }

            private void handleProcessingError(Exchange exchange) {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                statusService.updateStatusToError(fileName, stage, cause.getMessage());
            }
        };
    }
}
```

```java
package com.example.payment.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import com.example.payment.tasklet.*;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("spring_batch");
    }

    // Common DB Insertion Job
    @Bean
    public Job dbInsertionJob(
            Step insertPaymentTransactionStep,
            Step insertPaymentBulkInformationStep,
            Step insertPaymentInstructionStep) {
        return jobBuilderFactory.get("dbInsertionJob")
                .start(insertPaymentTransactionStep)
                .next(insertPaymentBulkInformationStep)
                .next(insertPaymentInstructionStep)
                .build();
    }

    @Bean
    public Step insertPaymentTransactionStep(InsertPaymentTransactionTasklet tasklet) {
        return stepBuilderFactory.get("insertPaymentTransactionStep")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step insertPaymentBulkInformationStep(InsertPaymentBulkInformationTasklet tasklet) {
        return stepBuilderFactory.get("insertPaymentBulkInformationStep")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step insertPaymentInstructionStep(InsertPaymentInstructionTasklet tasklet) {
        return stepBuilderFactory.get("insertPaymentInstructionStep")
                .tasklet(tasklet)
                .build();
    }

    // Common Post-DB Job
    @Bean
    public Job postDbJob(Step sendNotificationStep, Step archiveFileStep) {
        return jobBuilderFactory.get("postDbJob")
                .start(sendNotificationStep)
                .next(archiveFileStep)
                .build();
    }

    @Bean
    public Step sendNotificationStep(SendNotificationTasklet tasklet) {
        return stepBuilderFactory.get("sendNotificationStep")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step archiveFileStep(ArchiveFileTasklet tasklet) {
        return stepBuilderFactory.get("archiveFileStep")
                .tasklet(tasklet)
                .build();
    }
}
```

```java
package com.example.payment.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import com.example.payment.tasklet.*;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("spring_batch");
    }

    // Common DB Insertion Job
    @Bean
    public Job dbInsertionJob(
            Step insertPaymentTransactionStep,
            Step insertPaymentBulkInformationStep,
            Step insertPaymentInstructionStep) {
        return jobBuilderFactory.get("dbInsertionJob")
                .start(insertPaymentTransactionStep)
                .next(insertPaymentBulkInformationStep)
                .next(insertPaymentInstructionStep)
                .build();
    }

    @Bean
    public Step insertPaymentTransactionStep(InsertPaymentTransactionTasklet tasklet) {
        return stepBuilderFactory.get("insertPaymentTransactionStep")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step insertPaymentBulkInformationStep(InsertPaymentBulkInformationTasklet tasklet) {
        return stepBuilderFactory.get("insertPaymentBulkInformationStep")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step insertPaymentInstructionStep(InsertPaymentInstructionTasklet tasklet) {
        return stepBuilderFactory.get("insertPaymentInstructionStep")
                .tasklet(tasklet)
                .build();
    }

    // Common Post-DB Job
    @Bean
    public Job postDbJob(Step sendNotificationStep, Step archiveFileStep) {
        return jobBuilderFactory.get("postDbJob")
                .start(sendNotificationStep)
                .next(archiveFileStep)
                .build();
    }

    @Bean
    public Step sendNotificationStep(SendNotificationTasklet tasklet) {
        return stepBuilderFactory.get("sendNotificationStep")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step archiveFileStep(ArchiveFileTasklet tasklet) {
        return stepBuilderFactory.get("archiveFileStep")
                .tasklet(tasklet)
                .build();
    }
}
```

```java
@Configuration
@ConditionalOnProperty(name = "flow", havingValue = "flow1")
public class BatchConfigFlow1 {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job preDbJobFlow1(Step validateFlow1Step, Step enrichFlow1Step) {
        return jobBuilderFactory.get("preDbJobFlow1")
                .start(validateFlow1Step)
                .next(enrichFlow1Step)
                .build();
    }

    @Bean
    public Step validateFlow1Step(ValidateFlow1Tasklet tasklet) {
        return stepBuilderFactory.get("validateFlow1Step")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step enrichFlow1Step(EnrichFlow1Tasklet tasklet) {
        return stepBuilderFactory.get("enrichFlow1Step")
                .tasklet(tasklet)
                .build();
    }
}

@Configuration
@ConditionalOnProperty(name = "flow", havingValue = "flow2")
public class BatchConfigFlow2 {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job preDbJobFlow2(Step validateFlow2Step, Step enrichFlow2Step) {
        return jobBuilderFactory.get("preDbJobFlow2")
                .start(validateFlow2Step)
                .next(enrichFlow2Step)
                .build();
    }

    @Bean
    public Step validateFlow2Step(ValidateFlow2Tasklet tasklet) {
        return stepBuilderFactory.get("validateFlow2Step")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step enrichFlow2Step(EnrichFlow2Tasklet tasklet) {
        return stepBuilderFactory.get("enrichFlow2Step")
                .tasklet(tasklet)
                .build();
    }
}
```

```java
@Component
public class InsertPaymentTransactionTasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement logic to insert payment transaction
        return RepeatStatus.FINISHED;
    }
}

@Component
public class InsertPaymentBulkInformationTasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement logic to insert payment bulk information
        return RepeatStatus.FINISHED;
    }
}

@Component
public class InsertPaymentInstructionTasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement logic to insert payment instruction
        return RepeatStatus.FINISHED;
    }
}

@Component
public class SendNotificationTasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement logic to send notification
        return RepeatStatus.FINISHED;
    }
}

@Component
public class ArchiveFileTasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement logic to archive file
        return RepeatStatus.FINISHED;
    }
}

@Component
@ConditionalOnProperty(name = "flow", havingValue = "flow1")
public class ValidateFlow1Tasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement Flow1-specific validation logic
        return RepeatStatus.FINISHED;
    }
}

@Component
@ConditionalOnProperty(name = "flow", havingValue = "flow1")
public class EnrichFlow1Tasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement Flow1-specific enrichment logic
        return RepeatStatus.FINISHED;
    }
}

@Component
@ConditionalOnProperty(name = "flow", havingValue = "flow2")
public class ValidateFlow2Tasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement Flow2-specific validation logic
        return RepeatStatus.FINISHED;
    }
}

@Component
@ConditionalOnProperty(name = "flow", havingValue = "flow2")
public class EnrichFlow2Tasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement Flow2-specific enrichment logic
        return RepeatStatus.FINISHED;
    }
}
```

Key points in this implementation:

File Locking and Idempotent Processing:

We use readLock=idempotent along with idempotent=true and idempotentRepository=#jdbcMessageIdRepo.
This ensures that each file is processed only once and remains locked during processing.

Lock Management:

readLockRemoveOnCommit=true: Releases the lock when processing completes successfully.
readLockRemoveOnRollback=true: Releases the lock if an error occurs during processing.

Status Tracking:

We use processor methods to update the file status in the database at different stages of processing.

Error Handling:

The onException clause handles any errors during processing, updating the file status accordingly.

Spring Batch Integration:

The to("spring-batch:preDbInsertionJob") step triggers the Spring Batch job.
The Camel route handles the overall flow, including pre and post-processing steps.

Benefits of this approach:

Simplified Locking: We rely on Camel's built-in locking mechanism, which covers both file access and processing uniqueness.
Reduced Complexity: By eliminating the separate database lock for batch jobs, we simplify our code and reduce potential points of failure.
Consistent Status Tracking: File status is updated within the Camel route, providing a clear picture of the processing lifecycle.
Automatic Lock Release: Locks are automatically released on both successful completion and errors, reducing the risk of stuck locks.

This integration provides several benefits:

Separation of Concerns: Camel handles file monitoring and routing, while Spring Batch manages the complex data processing tasks.

-   Scalability: Multiple instances of the application can run on different nodes, each polling for files and triggering jobs as needed.
-   Flexibility: It's easy to add new routes for different file types or customers, and to trigger different jobs based on file characteristics.
-   Reliability: The use of file locks and idempotent repositories (configured elsewhere) ensures that files are processed exactly once, even in distributed environments.

To summarize, Camel acts as the orchestrator, detecting new files and triggering the appropriate Spring Batch jobs. This separation allows for efficient file handling (Camel's strength) combined with robust, scalable data processing (Spring Batch's strength).

# DLQ

This implementation provides the following benefits:

Files that fail processing multiple times are moved to a dead letter queue.
The system keeps track of retry attempts for each file.
Successfully processed files have their retry count reset.
A monitoring service regularly checks the DLQ and notifies operations if there are files needing attention.
A REST endpoint allows for manual reprocessing of files in the DLQ.

To further improve this system:

Implement more detailed logging for files moved to the DLQ.
Create a user interface for managing the DLQ, showing file details and processing history.
Implement analytics to identify patterns in files that end up in the DLQ.
Add the ability to automatically reprocess DLQ files during off-peak hours.

This DLQ implementation enhances your system's reliability by preventing problematic files from blocking the entire process while still allowing for their eventual processing through manual intervention.

1.First, let's add a new configuration for the dead letter queue:

```java
@Configuration
public class DlqConfig {

    @Value("${dlq.retry.count}")
    private int retryCount;

    @Value("${dlq.directory}")
    private String dlqDirectory;

    @Bean
    public DeadLetterQueueHandler dlqHandler() {
        return new DeadLetterQueueHandler(retryCount, dlqDirectory);
    }
}
```

2. Next, create a DeadLetterQueueHandler class:

```java
@Component
public class DeadLetterQueueHandler {

    private final int retryCount;
    private final String dlqDirectory;
    private final Map<String, Integer> fileRetryCount = new ConcurrentHashMap<>();

    public DeadLetterQueueHandler(int retryCount, String dlqDirectory) {
        this.retryCount = retryCount;
        this.dlqDirectory = dlqDirectory;
    }

    public boolean shouldMoveToDeadLetterQueue(String fileName) {
        int count = fileRetryCount.compute(fileName, (k, v) -> (v == null) ? 1 : v + 1);
        return count > retryCount;
    }

    public void moveToDeadLetterQueue(File file) throws IOException {
        File dlqFile = new File(dlqDirectory, file.getName());
        Files.move(file.toPath(), dlqFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        fileRetryCount.remove(file.getName());
    }

    public void resetRetryCount(String fileName) {
        fileRetryCount.remove(fileName);
    }
}
```

3. Update the CamelConfig to use the DeadLetterQueueHandler:

```java
@Configuration
public class CamelConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PaymentFileStatusService statusService;

    @Autowired
    private DeadLetterQueueHandler dlqHandler;

    // ... other fields and methods

    @Bean
    public RouteBuilder routeBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                onException(Exception.class)
                    .handled(true)
                    .process(this::handleProcessingError)
                    .choice()
                        .when(method(dlqHandler, "shouldMoveToDeadLetterQueue(${header.CamelFileName})"))
                            .process(exchange -> {
                                File file = exchange.getIn().getBody(File.class);
                                dlqHandler.moveToDeadLetterQueue(file);
                                statusService.updateStatus(file.getName(), Stage.ERROR, "Moved to DLQ after multiple failures");
                            })
                        .otherwise()
                            .to("direct:${header.CamelFileNameOnly}")
                    .end();

                // ... rest of the route configurations

                from("direct:errorHandler")
                    .routeId("errorHandlerRoute")
                    .log(LoggingLevel.ERROR, "Error processing file: ${header.CamelFileName}")
                    .process(this::handleProcessingError);
            }

            private void handleProcessingError(Exchange exchange) {
                Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                statusService.updateStatusToError(fileName, exchange.getFromRouteId(), cause.getMessage());
            }

            // ... other methods
        };
    }
}
```

4. Modify each stage's route to handle retries and DLQ:

```java
private void configurePreDbStage() {
    from("file:{{" + flow + ".input.directory}}?include={{" + flow + ".file.pattern}}&readLock=idempotent&idempotent=true&idempotentRepository=#jdbcMessageIdRepo&readLockRemoveOnCommit=true&readLockRemoveOnRollback=true")
        .routeId("preDbInsertionRoute")
        .log("Pre-DB Insertion: ${header.CamelFileName}")
        .process(this::initializeFileStatus)
        .doTry()
            .to("spring-batch:preDbJob" + flow)
            .process(this::completePreDbProcessing)
            .to("file:{{" + flow + ".output.directory}}")
            .process(exchange -> dlqHandler.resetRetryCount(exchange.getIn().getHeader("CamelFileName", String.class)))
        .doCatch(Exception.class)
            .to("direct:${header.CamelFileNameOnly}")
        .end();
}
```

5.

```properties
dlq.retry.count=3
dlq.directory=/path/to/dlq/directory
```

6. Create a DLQ monitoring service:

```java
@Service
public class DlqMonitoringService {

    private final String dlqDirectory;

    public DlqMonitoringService(@Value("${dlq.directory}") String dlqDirectory) {
        this.dlqDirectory = dlqDirectory;
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void monitorDlq() {
        File dlqDir = new File(dlqDirectory);
        File[] dlqFiles = dlqDir.listFiles();
        if (dlqFiles != null && dlqFiles.length > 0) {
            // Send notification to operations team
            sendNotification("Files in DLQ: " + dlqFiles.length);
        }
    }

    private void sendNotification(String message) {
        // Implement notification logic (e.g., email, Slack message, etc.)
    }
}
```

7. Add a controller for manual intervention:

```java
@RestController
@RequestMapping("/dlq")
public class DlqController {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private PaymentFileStatusService statusService;

    @PostMapping("/retry/{fileName}")
    public ResponseEntity<String> retryFile(@PathVariable String fileName) throws Exception {
        File dlqFile = new File(dlqDirectory, fileName);
        if (!dlqFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        // Move file back to the input directory
        File inputFile = new File(inputDirectory, fileName);
        Files.move(dlqFile.toPath(), inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Reset file status
        statusService.resetStatus(fileName);

        // Trigger processing
        camelContext.getRouteController().startRoute("preDbInsertionRoute");

        return ResponseEntity.ok("File queued for reprocessing");
    }
}
```

```java
private File fileTransformer(String fileName, String localDirectory, Document document)
            throws TransformerException {
        DOMSource dom = new DOMSource(document);
        Transformer transformer = newDefaultInstance().newTransformer();
        File signatureFile = archiveFileUtils.fileWithDirectory(localDirectory, fileName);
        StreamResult result = new StreamResult(signatureFile);
        transformer.transform(dom, result);
        return signatureFile;
    }
```

# Multilingual File Name for File Upload/Download

Ensure your Spring Boot application is configured to use UTF-8 encoding globally:
```yml
server:
  servlet:
    encoding:
      charset: UTF-8
      force: true
```

Invalid Character Handling: Instead of trying to transliterate or remove non-Latin characters, we now only replace characters that are typically invalid in most file systems (like /, , :, *, ?, ", <, >, |).

Length Limitation: We still trim the filename to a maximum length, but we do so carefully to avoid cutting in the middle of a multi-byte character (which is common in Asian scripts).


File System Considerations: Ensure your Linux file system is configured to use UTF-8. Most modern Linux distributions do this by default. For the ext4 file system (commonly used in Linux), file names are stored as byte sequences, which works well with UTF-8 encoded Asian characters.


Database Storage: Make sure your Oracle database columns for storing file names use a character set that supports Asian languages, such as AL32UTF8 (which is Oracle's implementation of UTF-8).

```java
import org.springframework.stereotype.Component;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.regex.Pattern;

@Component
public class FileUtil {

    private static final int MAX_FILENAME_LENGTH = 255;
    private static final Pattern INVALID_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

    public String sanitizeFilename(String filename) {
        // Replace invalid characters with underscore
        String sanitized = INVALID_CHARS.matcher(filename).replaceAll("_");
        
        // Trim to max length, being careful not to cut in the middle of a multi-byte character
        if (sanitized.length() > MAX_FILENAME_LENGTH) {
            byte[] bytes = sanitized.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (bytes.length > MAX_FILENAME_LENGTH) {
                sanitized = new String(bytes, 0, MAX_FILENAME_LENGTH, java.nio.charset.StandardCharsets.UTF_8);
                // Ensure we don't end with an incomplete multi-byte character
                while (!sanitized.isEmpty() && !Character.isValidCodePoint(sanitized.charAt(sanitized.length() - 1))) {
                    sanitized = sanitized.substring(0, sanitized.length() - 1);
                }
            }
        }
        
        return sanitized.isEmpty() ? "file" : sanitized;
    }

    public String getUniqueFilename(Path directory, String filename) throws IOException {
        String baseName = filename;
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = filename.substring(0, dotIndex);
            extension = filename.substring(dotIndex);
        }

        Path filePath = directory.resolve(filename);
        int counter = 1;

        while (Files.exists(filePath)) {
            String newName = String.format("%s(%d)%s", baseName, counter++, extension);
            filePath = directory.resolve(newName);
        }

        return filePath.getFileName().toString();
    }

    public String storeFile(Path directory, String originalFilename, byte[] content) throws IOException {
        String sanitizedFilename = sanitizeFilename(originalFilename);
        String uniqueFilename = getUniqueFilename(directory, sanitizedFilename);
        
        Path filePath = directory.resolve(uniqueFilename);
        Files.write(filePath, content);
        
        return uniqueFilename;
    }
}
```

When serving files for download, use UTF-8 encoding for the file name in the Content-Disposition header:
```java
@GetMapping("/download/{storedFilename}")
public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable String storedFilename) throws IOException {
    byte[] data = fileService.getFile(storedFilename);
    String originalFilename = fileService.getOriginalFilename(storedFilename);
    ByteArrayResource resource = new ByteArrayResource(data);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment;filename*=UTF-8''" + URLEncoder.encode(originalFilename, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20"))
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(data.length)
        .body(resource);
}
```
