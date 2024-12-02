<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.1</version>
        <relativePath/>
    </parent>
    
    <groupId>com.example</groupId>
    <artifactId>report-engine</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>report-engine</name>
    <description>Report Engine with JasperReports and Spring Boot</description>
    
    <properties>
        <java.version>17</java.version>
        <jasperreports.version>6.20.6</jasperreports.version>
        <jasperreports-fonts.version>6.20.6</jasperreports-fonts.version>
        <commons-lang3.version>3.14.0</commons-lang3.version>
        <lombok.version>1.18.30</lombok.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- JasperReports Dependencies -->
        <dependency>
            <groupId>net.sf.jasperreports</groupId>
            <artifactId>jasperreports</artifactId>
            <version>${jasperreports.version}</version>
        </dependency>
        <dependency>
            <groupId>net.sf.jasperreports</groupId>
            <artifactId>jasperreports-fonts</artifactId>
            <version>${jasperreports-fonts.version}</version>
        </dependency>
        
        <!-- For handling PDF generation -->
        <dependency>
            <groupId>com.lowagie</groupId>
            <artifactId>itext</artifactId>
            <version>2.1.7</version>
        </dependency>
        
        <!-- For Excel export support if needed -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi</artifactId>
            <version>5.2.5</version>
        </dependency>
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.2.5</version>
        </dependency>
        
        <!-- Utilities -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>${commons-lang3.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <optional>true</optional>
        </dependency>
        
        <!-- For handling different charsets in reports -->
        <dependency>
            <groupId>com.ibm.icu</groupId>
            <artifactId>icu4j</artifactId>
            <version>74.1</version>
        </dependency>
        
        <!-- Testing Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
        </plugins>
        
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
                <includes>
                    <include>**/*.properties</include>
                    <include>**/*.xml</include>
                    <include>**/*.jrxml</include>
                    <include>**/*.jasper</include>
                    <include>**/*.jpg</include>
                    <include>**/*.png</include>
                    <include>**/*.ttf</include>
                </includes>
            </resource>
        </resources>
    </build>
    
    <repositories>
        <repository>
            <id>jaspersoft-third-party</id>
            <url>https://jaspersoft.jfrog.io/jaspersoft/third-party-ce-artifacts/</url>
        </repository>
        <repository>
            <id>jasperreports</id>
            <url>https://jaspersoft.jfrog.io/jaspersoft/jr-ce-releases</url>
        </repository>
    </repositories>
    
</project>