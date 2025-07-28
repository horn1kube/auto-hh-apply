plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    java
}

group = "app"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // HTML parsing
    implementation("org.jsoup:jsoup:1.17.2")
    
    // Environment configuration
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
    
    // Database
    implementation("org.xerial:sqlite-jdbc:3.45.0.0")
    
    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind")
    
    // Logging
    implementation("org.slf4j:slf4j-api")
    implementation("ch.qos.logback:logback-classic")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
} 